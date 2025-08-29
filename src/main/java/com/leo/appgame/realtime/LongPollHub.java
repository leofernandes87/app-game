package com.leo.appgame.realtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@ApplicationScoped
public class LongPollHub {
    private static final Logger LOG = Logger.getLogger(LongPollHub.class.getName());
    private final List<AsyncResponse> waiters = new CopyOnWriteArrayList<>();

    public void subscribe(AsyncResponse ar) {
        ar.register((CompletionCallback) t -> {
            waiters.remove(ar);
            LOG.info("[LP] complete -> removed (error? " + (t != null) + ")");
        });
        waiters.add(ar);
        LOG.info("[LP] subscribers: " + waiters.size());
    }

    public void heartbeat(AsyncResponse ar) {
        waiters.remove(ar);
        ar.resume(Response.status(204)
                .header("Cache-Control", "no-store")
                .build());
    }

    public void publish(String json) {
        LOG.info("[LP] publish to " + waiters.size() + " subscribers");
        for (AsyncResponse ar : waiters) {
            ar.resume(Response.ok(json)
                    .header("Cache-Control", "no-store")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
        waiters.clear();
    }
}
