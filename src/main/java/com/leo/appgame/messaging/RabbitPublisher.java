package com.leo.appgame.messaging;

import com.leo.appgame.models.Jogo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;

import java.nio.charset.StandardCharsets;

/**
 * Publicador de eventos no RabbitMQ para o domínio de "jogos".
 *
 * <p><b>O que faz:</b> após a criação/persistência de um {@link Jogo}, este bean publica
 * um pequeno JSON na fila {@link #QUEUE}. Quem consome essa fila é o {@code RabbitConsumer},
 * que, por sua vez, reencaminha o evento para todas as conexões long-poll ativas através do
 * {@code LongPollHub.publish(...)} (ou seja, o publisher → fila → consumer → hub → navegadores).</p>
 *
 * <p><b>Ciclo de vida:</b> é um EJB {@link Singleton} iniciado na subida da aplicação
 * ({@link Startup}). O {@link #init()} abre a conexão/canal e garante a fila.
 * O {@link #close()} fecha os recursos no undeploy/shutdown.</p>
 *
 * <p><b>Observação:</b> implementação simples para exercício, com fila não-durável
 * e uso de um único canal/connection mantidos como estado do bean.</p>
 */
@Startup
@Singleton
public class RabbitPublisher {
    /** Nome da fila onde os eventos serão publicados. */
    private static final String QUEUE = "jogos.events";

    /** Conexão AMQP com o broker. É aberta no {@link #init()} e fechada no {@link #close()}. */
    private Connection connection;

    /** Canal AMQP usado para publicar mensagens. Criado no {@link #init()} e fechado no {@link #close()}. */
    private Channel channel;

    /**
     * Inicializa a infraestrutura de publicação ao subir a aplicação.
     *
     * <p>Passos:
     * <ol>
     *   <li>Lê host/porta/credenciais de variáveis de ambiente;</li>
     *   <li>Abre {@link #connection} e {@link #channel};</li>
     *   <li>Garante a existência da fila {@link #QUEUE} (não-durável, exclusiva=false, autoDelete=false).</li>
     * </ol>
     * Em caso de falha, a exceção sobe (o container loga na inicialização).</p>
     */
    @PostConstruct
    public void init() throws Exception {
        //todo preciso extrair para um auxiliar pra evitar duplicação de código
        /*
        MELHORIA:
        Canal do Rabbit não é thread-safe
        */
        ConnectionFactory f = new ConnectionFactory();

        f.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
        f.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        f.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "guest"));
        f.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "guest"));

        connection = f.newConnection("appgame-publisher");
        channel = connection.createChannel();

        // fila simples
        channel.queueDeclare(QUEUE, false, false, false, null);
    }

    /**
     * Publica um evento JSON "JOGO_CRIADO" com os dados do jogo recém-persistido.
     *
     * <p>Formato do JSON (exemplo):
     * <pre>
     * {
     *   "type": "JOGO_CRIADO",
     *   "id": 1,
     *   "timeA": "...",
     *   "timeB": "...",
     *   "placarA": 0,
     *   "placarB": 0,
     *   "statusJogo": "NAO_INICIADO",
     *   "dataHoraPartida": "2025-08-28T20:00:00"
     * }
     * </pre>
     * O {@code RabbitConsumer} receberá esse JSON e chamará o {@code LongPollHub.publish(json)}
     * para acordar os clientes em long-poll.</p>
     *
     * @param j jogo que acabou de ser criado/persistido (espera-se {@code id} não nulo)
     */
    public void publishJogoCriado(Jogo j) {
        try {
            var json = Json.createObjectBuilder()
                    .add("type", "JOGO_CRIADO")
                    .add("id", j.getId())
                    .add("timeA", j.getTimeA())
                    .add("timeB", j.getTimeB())
                    .add("placarA", j.getPlacarA())
                    .add("placarB", j.getPlacarB())
                    .add("statusJogo", j.getStatusJogo().name())
                    .add("dataHoraPartida", j.getDataHoraPartida() == null ? "" : j.getDataHoraPartida().toString())
                    .build()
                    .toString();
            channel.basicPublish("", QUEUE, null, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // todo melhorar esse log
            e.printStackTrace();
        }
    }

    /**
     * Encerra o canal e a conexão com o broker durante o shutdown/undeploy da aplicação.
     * <p>Chamado automaticamente pelo container devido ao {@link PreDestroy}.</p>
     */
    @PreDestroy
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) channel.close();
        if (connection != null && connection.isOpen()) connection.close();
    }
}
