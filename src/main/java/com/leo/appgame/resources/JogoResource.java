package com.leo.appgame.resources;

import com.leo.appgame.dtos.CriarJogoDto;
import com.leo.appgame.enums.StatusJogo;
import com.leo.appgame.models.Jogo;
import com.leo.appgame.services.JogoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    // NOVO: listar todos (com filtros opcionais)
    @GET
    public Response listarJogos(
            @QueryParam("status") String status,                // ex.: EM_ANDAMENTO | ENCERRADO (opcional)
            @QueryParam("de") String de,                        // ex.: 2025-08-30T00:00:00 (opcional)
            @QueryParam("ate") String ate                       // ex.: 2025-08-31T23:59:59 (opcional)
    ) {
        LOG.info("Listando jogos...");
        Optional<StatusJogo> statusOpt = parseStatus(status);
        Optional<LocalDateTime> deOpt = parseDateTime(de);
        Optional<LocalDateTime> ateOpt = parseDateTime(ate);

        List<Jogo> jogos = jogoService.listarJogos(statusOpt, deOpt, ateOpt);
        return Response.ok(jogos).build(); // 200 + [] se vazio
    }

    // Helpers
    private Optional<StatusJogo> parseStatus(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        try {
            return Optional.of(StatusJogo.valueOf(s.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status inválido: " + s + " (use EM_ANDAMENTO ou ENCERRADO)");
        }
    }

    private Optional<LocalDateTime> parseDateTime(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(s.trim()));
        } catch (Exception e) {
            throw new BadRequestException("datetime inválido (use ISO-8601, ex.: 2025-08-30T20:00:00)");
        }
    }
}
