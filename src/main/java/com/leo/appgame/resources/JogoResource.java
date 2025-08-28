package com.leo.appgame.resources;

import com.leo.appgame.dtos.CriarJogoDto;
import com.leo.appgame.models.Jogo;
import com.leo.appgame.services.JogoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("/jogos")
@Produces("application/json")
@Consumes("application/json")
public class JogoResource {

    private static final Logger LOG = Logger.getLogger(JogoResource.class.getName());

    @Inject
    private JogoService jogoService;

    @POST
    public Response criarNovoJogo(CriarJogoDto criarJogoDto) {
        LOG.info("Criando novo jogo...");
        Jogo jogo = jogoService.criarNovoJogo(criarJogoDto);
        return Response.ok(jogo).status(201).build();
    }

    @GET
    @Path("/{id}")
    public Response buscarJogoPorId(@PathParam("id") Long id) {
        LOG.info("Buscando jogo por id...");
        Jogo jogo = jogoService.buscarJogoPorId(id);
        return Response.ok(jogo).status(200).build();
    }
}
