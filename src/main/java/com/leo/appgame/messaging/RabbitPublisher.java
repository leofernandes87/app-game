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

@Startup
@Singleton
public class RabbitPublisher {
    private static final String QUEUE = "jogos.events";

    private Connection connection;
    private Channel channel;

    @PostConstruct
    public void init() throws Exception {
        //todo preciso extrair para um auxiliar pra evitar duplicação de código
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

    @PreDestroy
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) channel.close();
        if (connection != null && connection.isOpen()) connection.close();
    }
}
