package com.leo.appgame.messaging;

import com.leo.appgame.realtime.LongPollHub;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;

/**
 * Consumer de eventos do RabbitMQ para o domínio de "jogos".
 *
 * <p><b>O que faz:</b>
 * <ul>
 *   <li>Conecta no RabbitMQ e começa a consumir mensagens da fila {@link #QUEUE}.</li>
 *   <li>Cada mensagem recebida (JSON) é <b>replicada</b> para todos os navegadores
 *       conectados via long-poll, chamando {@link LongPollHub#publish(String)}.</li>
 *   <li>Com isso, o RabbitMQ atua como um "sino" de que algo mudou; o front recebe
 *       o aviso e recarrega a listagem pela sua API REST.</li>
 * </ul>
 *
 * <p><b>Relação com o LongPollHub:</b> este consumer é a "ponte AMQP → HTTP".
 * Quando chega uma mensagem na fila, ele chama {@code hub.publish(json)} que
 * dá <i>resume(200 OK)</i> em todas as requisições long-poll pendentes.</p>
 *
 * <p><b>Ciclo de vida:</b> é um EJB {@link Singleton} iniciado junto com a aplicação
 * ({@link Startup}). O método {@link #start()} roda ao subir; {@link #stop()} roda
 * no shutdown/undeploy para encerrar o consumo.</p>
 *
 * <p><b>Simplificações propositalmente adotadas para o exercício:</b>
 * auto-ack ativado, fila declarada como não-durável e sem reentregas.</p>
 */

//Usado Singleton propositalmente para simplificar o ciclo de vida e iniciar ao subir
@Startup
@Singleton
public class RabbitConsumer {
    private static final String QUEUE = "jogos.events";

    @Inject
    LongPollHub hub;

    private Connection conn;
    private Channel ch;
    private String consumerTag;

    /**
     * Inicializa o consumo da fila ao subir a aplicação.
     *
     * <p>Passos:
     * <ol>
     *   <li>Cria {@link ConnectionFactory} lendo variáveis de ambiente (host/porta/credenciais).</li>
     *   <li>Abre {@link #conn} e {@link #ch} e garante a existência da fila {@link #QUEUE}.</li>
     *   <li>Inicia o {@code basicConsume} em auto-ack: ao receber uma entrega,
     *       transforma em String (UTF-8) e chama {@link LongPollHub#publish(String)}.</li>
     * </ol>
     *
     * <p><b>Notas:</b> Auto-ack (true) simplifica o fluxo e é suficiente para o exercício.
     * Em produção, você provavelmente usaria ack manual, DLQ e mensagens persistentes.</p>
     */
    @PostConstruct
    public void start() throws Exception {
        //todo preciso extrair para um auxiliar pra evitar duplicação de código
        ConnectionFactory f = new ConnectionFactory();

        f.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
        f.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        f.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "guest"));
        f.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "guest"));

        conn = f.newConnection("appgame-consumer");
        ch = conn.createChannel();

        // Fila simples (não durável) para desenvolvimento.
        // Se quiser manter mensagens após restart do broker, torne 'durable=true'.
        ch.queueDeclare(QUEUE, false, false, false, null);

        // Inicia consumo com autoAck = true (sem reentrega em falha de processamento).
        consumerTag = ch.basicConsume(QUEUE, true, (ctag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // reencaminha para todos os long-pollers conectados
            hub.publish(body);
        }, ctag -> {});
    }

    /**
     * Encerra com segurança o consumidor, canal e conexão ao desligar/undeploy.
     *
     * <p>Passos:
     * <ol>
     *   <li>Cancela o consumidor pelo {@link #consumerTag}.</li>
     *   <li>Fecha o {@link #ch} se ainda estiver aberto.</li>
     *   <li>Fecha a {@link #conn} se ainda estiver aberta.</li>
     * </ol>
     *
     * <p>Chamado automaticamente pelo container antes de destruir o bean.</p>
     */
    @PreDestroy
    public void stop() throws Exception {
        if (ch != null && ch.isOpen() && consumerTag != null) ch.basicCancel(consumerTag);
        if (ch != null && ch.isOpen()) ch.close();
        if (conn != null && conn.isOpen()) conn.close();
    }
}
