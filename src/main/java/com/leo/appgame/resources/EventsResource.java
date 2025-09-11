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

/**
 * Recurso REST para “tempo real” via <b>Long-Polling</b>.
 *
 * <p>Este endpoint mantém a requisição HTTP aberta por alguns segundos, aguardando
 * um evento do servidor. Se um evento chegar nesse intervalo, responde <b>200 OK</b>
 * com JSON; se não chegar, responde <b>204 No Content</b> e o cliente reconecta.</p>
 *
 * <h3>Rotas</h3>
 * <ul>
 *   <li><b>GET /events/long-poll</b> — abre a conexão e aguarda um evento (até ~25s).</li>
 *   <li><b>GET /events/ping</b> — verificação rápida de saúde (sempre 200 + {"ok":true}).</li>
 * </ul>
 *
 * <p>Observação: O envio do evento para os clientes é disparado pelo {@code LongPollHub}
 * (chamado, por exemplo, pelo RabbitMQ consumer). Este recurso apenas <i>suspende</i> a
 * resposta e delega ao hub quando houver algo para entregar.</p>
 */
@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
public class EventsResource {
    private static final Logger LOG = Logger.getLogger(EventsResource.class.getName());

    /**
     * Hub em memória que mantém as conexões pendentes e realiza o "resume"
     * (200 ou 204) quando há evento ou timeout.
     */
    @Inject
    LongPollHub hub;

    /**
     * Abre uma conexão long-poll com o cliente.
     *
     * <p>Comportamento:</p>
     * <ul>
     *   <li>Suspende a resposta por até <b>25 segundos</b>.</li>
     *   <li>Se o tempo expirar sem eventos, responde <b>204 No Content</b> (via {@link LongPollHub#heartbeat}).</li>
     *   <li>Se algum evento for publicado no intervalo, o hub responde <b>200 OK</b> com JSON.</li>
     * </ul>
     *
     * <p>O cliente deve, ao receber 200 ou 204, <b>reconectar</b> imediatamente
     * (loop simples de long-poll).</p>
     *
     * @param ar resposta assíncrona que será retomada pelo hub (evento) ou por timeout
     */
    @GET
    @Path("/long-poll")
    public void waitEvent(@Suspended AsyncResponse ar) {
        LOG.info("[LP] nova conexão long-poll");
        ar.setTimeout(25, TimeUnit.SECONDS);
        ar.setTimeoutHandler(resp -> {
            LOG.info("[LP] timeout -> 204");
            hub.heartbeat(resp); // devolve 204 e remove da lista
        });
        hub.subscribe(ar);// registra para ser acordado quando houver publish(...)
    }

    /**
     * Verificação rápida de saúde do endpoint.
     *
     * <p>Útil para testar rede/proxy sem segurar conexão: sempre retorna
     * <b>200 OK</b> com <code>{"ok":true}</code> e cabeçalho <code>Cache-Control: no-store</code>.</p>
     */
    @GET @Path("/ping")
    public Response ping() {
        return Response.ok("{\"ok\":true}")
                .header("Cache-Control","no-store")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
