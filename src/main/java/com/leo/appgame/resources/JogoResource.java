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

/**
 * Recurso REST (JAX-RS) para operações de "jogos".
 *
 * <p>Base path: {@code /api/jogos}</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li><b>POST /jogos</b> — cria um novo jogo a partir de um {@link CriarJogoDto} e retorna o {@link Jogo} criado (201).</li>
 *   <li><b>GET /jogos/{id}</b> — busca um jogo pelo identificador (200).</li>
 *   <li><b>GET /jogos</b> — lista todos os jogos, com filtros opcionais por status e faixa de data/hora (200).</li>
 * </ul>
 *
 * <p>Formato de data/hora aceito nos filtros: ISO-8601, ex.: {@code 2025-08-30T20:00} ou {@code 2025-08-30T20:00:00}.</p>
 */
@Path("/jogos")
@Produces("application/json")
@Consumes("application/json")
public class JogoResource {

    private static final Logger LOG = Logger.getLogger(JogoResource.class.getName());

    @Inject
    private JogoService jogoService;

    /**
     * Cria um novo jogo.
     *
     * @param criarJogoDto payload com os campos necessários para criar o jogo
     * @return 201 Created + JSON do jogo persistido
     */
    @POST
    public Response criarNovoJogo(CriarJogoDto criarJogoDto) {
        LOG.info("Criando novo jogo...");
        Jogo jogo = jogoService.criarNovoJogo(criarJogoDto);
        return Response.ok(jogo).status(201).build();
    }

    /**
     * Busca um jogo pelo seu identificador.
     *
     * @param id identificador do jogo
     * @return 200 OK + JSON do jogo (ou 404 se o service lançar exceção de não encontrado)
     */
    @GET
    @Path("/{id}")
    public Response buscarJogoPorId(@PathParam("id") Long id) {
        LOG.info("Buscando jogo por id...");
        Jogo jogo = jogoService.buscarJogoPorId(id);
        return Response.ok(jogo).status(200).build();
    }

    /**
     * Lista jogos com filtros opcionais.
     *
     * <p>Todos os filtros são opcionais; se nenhum for informado, retorna a lista completa.</p>
     *
     * @param status status do jogo para filtrar
     *               (valores válidos do enum: {@code NAO_INICIADO}, {@code EM_ANDAMENTO}, {@code FINALIZADO})
     * @param de     data/hora inicial (inclusive) no formato ISO-8601, ex.: {@code 2025-08-30T00:00} ou {@code 2025-08-30T00:00:00}
     * @param ate    data/hora final (inclusive) no formato ISO-8601, ex.: {@code 2025-08-31T23:59} ou {@code 2025-08-31T23:59:59}
     * @return 200 OK + lista JSON (pode ser vazia)
     */
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

    // ======================
    // Helpers de parsing
    // ======================

    /**
     * Converte a string do status para o enum {@link StatusJogo}.
     *
     * @param s valor textual do status (case-insensitive)
     * @return Optional vazio se nulo/vazio; senão o enum correspondente
     * @throws BadRequestException se o valor não for reconhecido
     */
    private Optional<StatusJogo> parseStatus(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        try {
            return Optional.of(StatusJogo.valueOf(s.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status inválido: " + s + " (use EM_ANDAMENTO ou ENCERRADO)");
        }
    }

    /**
     * Converte uma string ISO-8601 para {@link LocalDateTime}.
     *
     * @param s data/hora em ISO-8601 (ex.: 2025-08-30T20:00 ou 2025-08-30T20:00:00)
     * @return Optional vazio se nulo/vazio; senão o {@link LocalDateTime} correspondente
     * @throws BadRequestException se o valor não estiver em formato ISO-8601 válido
     */
    private Optional<LocalDateTime> parseDateTime(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(s.trim()));
        } catch (Exception e) {
            throw new BadRequestException("datetime inválido (use ISO-8601, ex.: 2025-08-30T20:00:00)");
        }
    }
}
