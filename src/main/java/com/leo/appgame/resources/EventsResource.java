package com.leo.appgame.resources;

import com.leo.appgame.realtime.LongPollHub;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON) // <- negocia JSON por padrão (200)
public class EventsResource {
    private static final Logger LOG = Logger.getLogger(EventsResource.class.getName());

    @Inject
    LongPollHub hub;

    @GET
    @Path("/long-poll")
    public void waitEvent(@Suspended AsyncResponse ar) {
        LOG.info("[LP] nova conexão long-poll");
        ar.setTimeout(25, TimeUnit.SECONDS);
        ar.setTimeoutHandler(resp -> {
            LOG.info("[LP] timeout -> 204");
            hub.heartbeat(resp);
        });
        hub.subscribe(ar);
    }

    @GET @Path("/ping")
    public Response ping() {
        return Response.ok("{\"ok\":true}")
                .header("Cache-Control","no-store")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
