package com.leo.appgame.realtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
/**
 * Hub de Long-Poll em memória.
 *
 * <p>Mantém uma lista de requisições HTTP penduradas (AsyncResponse) e:
 * <ul>
 *   <li>{@link #subscribe(AsyncResponse)} adiciona uma nova conexão para aguardar eventos;</li>
 *   <li>{@link #publish(String)} envia o evento para todos os conectados (200 + JSON) e limpa a fila;</li>
 *   <li>{@link #heartbeat(AsyncResponse)} responde 204 quando o timeout expira (sem evento).</li>
 * </ul>
 * É um mecanismo simples de “notificação”: o cliente refaz o long-poll após cada resposta.</p>
 */
@ApplicationScoped
public class LongPollHub {
    private static final Logger LOG = Logger.getLogger(LongPollHub.class.getName());

    /** Conexões pendentes aguardando um evento. CopyOnWrite para evitar problemas de concorrência. */
    private final List<AsyncResponse> waiters = new CopyOnWriteArrayList<>();

    /**
     * Registra uma nova conexão long-poll para receber o próximo evento.
     * <p>Também registra um callback de conclusão para remover automaticamente
     * a conexão da lista, seja por sucesso, erro ou cancelamento do cliente.</p>
     *
     * @param ar a resposta assíncrona (suspensa) desta requisição long-poll
     */
    public void subscribe(AsyncResponse ar) {
        ar.register((CompletionCallback) t -> {
            waiters.remove(ar);
            LOG.info("[LP] complete -> removed (error? " + (t != null) + ")");
        });
        waiters.add(ar);
        LOG.info("[LP] subscribers: " + waiters.size());
    }

    /**
     * Responde com 204 (No Content) quando o timeout do long-poll expira.
     * <p>Serve como “batimento” para o cliente saber que deve reconectar,
     * sem trafegar corpo de resposta.</p>
     *
     * @param ar a resposta assíncrona que estava aguardando
     */
    public void heartbeat(AsyncResponse ar) {
        waiters.remove(ar);
        ar.resume(Response.status(204)
                .header("Cache-Control", "no-store")
                .build());
    }

    /**
     * Publica um evento JSON para todas as conexões pendentes.
     * <p>Envia 200 OK com corpo JSON e cabeçalho no-store, depois limpa a lista
     * (cada long-poll entrega no máximo um evento).</p>
     *
     * @param json payload do evento no formato JSON (string já serializada)
     */
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
