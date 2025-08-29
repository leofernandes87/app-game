package com.leo.appgame.messaging;

import com.leo.appgame.realtime.LongPollHub;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;

@Startup
@Singleton
public class RabbitConsumer {
    private static final String QUEUE = "jogos.events";

    @Inject
    LongPollHub hub;

    private Connection conn;
    private Channel ch;
    private String consumerTag;

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
        ch.queueDeclare(QUEUE, false, false, false, null);

        consumerTag = ch.basicConsume(QUEUE, true, (ctag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // reencaminha para todos os long-pollers conectados
            hub.publish(body);
        }, ctag -> {});
    }

    @PreDestroy
    public void stop() throws Exception {
        if (ch != null && ch.isOpen() && consumerTag != null) ch.basicCancel(consumerTag);
        if (ch != null && ch.isOpen()) ch.close();
        if (conn != null && conn.isOpen()) conn.close();
    }
}
