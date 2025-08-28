package com.leo.appgame;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/hello-world")
public class HelloResource {
    @PersistenceContext(unitName = "appPU")
    EntityManager em;

    @GET
    @Transactional
    public Response hello() {
        Game game = new Game();
        game.setTimeA("Flamengo");
        game.setTimeB("Palmeiras");
        em.persist(game);
        return Response.ok("ok " + game.getId()).build();
    }
}