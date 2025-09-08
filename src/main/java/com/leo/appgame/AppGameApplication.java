package com.leo.appgame;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

/**
 * Configuração raiz da aplicação JAX-RS e documentação de arquitetura/fluxo.
 *
 * <h2>Papel</h2>
 * Define o path base dos endpoints REST como <b>/api</b>. Ex.: <code>/api/jogos</code>,
 * <code>/api/events/long-poll</code>.
 *
 * <h2>Fluxo ponta-a-ponta (“sino de mudança”)</h2>
 * <ol>
 *   <li><b>Criação de jogo (producer)</b><br/>
 *       <code>POST /api/jogos</code> → <code>JogoResource</code> chama <code>JogoService</code> para persistir
 *       → ao salvar, o service chama <code>RabbitPublisher.publishJogoCriado(jogo)</code>.<br/>
 *       O publisher envia um JSON curto para a fila <code>jogos.events</code> (ex.: <code>{"type":"JOGO_CRIADO","id":123,...}</code>).</li>
 *
 *   <li><b>Fila RabbitMQ</b><br/>
 *       O broker recebe e enfileira a mensagem. Em DEV usamos <code>queueDeclare(..., durable=false,...)</code>
 *       (sem persistência entre reinícios).</li>
 *
 *   <li><b>Consumer → Hub (ponte AMQP → HTTP)</b><br/>
 *       <code>RabbitConsumer</code> (EJB <code>@Singleton @Startup</code>) fica ouvindo com <code>basicConsume</code>.
 *       A cada mensagem recebida, ele chama <code>LongPollHub.publish(json)</code>, acordando quem estiver aguardando.</li>
 *
 *   <li><b>Long-poll responde aos browsers</b><br/>
 *       O front mantém uma requisição pendurada em <code>GET /api/events/long-poll</code> (até ~25s).
 *       Se um evento chega nesse intervalo, o <code>EventsResource</code>/<code>LongPollHub</code> respondem <b>200 OK</b> com JSON;
 *       se nada chega, respondem <b>204 No Content</b> (heartbeat) e o cliente reconecta.</li>
 *
 *   <li><b>Página atualiza a lista</b><br/>
 *       Ao receber 200 do long-poll, o script de <i>TodosJogosPage</i> dá <code>form.submit()</code>.
 *       Isso chama sua própria API <code>GET /api/jogos</code> (com filtros) e renderiza a tabela atualizada.
 *       <b>Importante:</b> o RabbitMQ não envia a lista; ele apenas <i>notifica</i> que “tem coisa nova”.
 *       O dado sempre vem da API REST.</li>
 * </ol>
 *
 * <h3>Modelo “edge-triggered”</h3>
 * Mensagem curta no Rabbit → navegador refaz a consulta. Simples, desacoplado e fácil de escalar.
 *
 * <h2>Endpoints principais</h2>
 * <ul>
 *   <li><code>POST /api/jogos</code> — cria jogo (201) e publica evento no RabbitMQ.</li>
 *   <li><code>GET  /api/jogos/{id}</code> — busca por id (200).</li>
 *   <li><code>GET  /api/jogos?status&de&ate</code> — lista com filtros opcionais (200).</li>
 *   <li><code>GET  /api/events/long-poll</code> — long-poll (~25s): 200 com evento ou 204 se vazio.</li>
 *   <li><code>GET  /api/events/ping</code> — health check simples (200).</li>
 * </ul>
 *
 * <h2>Comportamento do long-poll no cliente</h2>
 * O script evita requisições concorrentes, pausa quando a aba está em background e usa
 * <b>backoff exponencial</b> em erros (p.ex. 0.25s → 0.5s → 1s → 2s → 4s, com limite).
 * Para <b>204</b> (timeout sem evento), reconecta imediatamente (sem backoff).
 *
 * <h2>Ambiente & variáveis</h2>
 * <ul>
 *   <li>RabbitMQ (Docker): host padrão <code>rabbitmq</code> (ou <code>RABBITMQ_HOST</code>), porta <code>RABBITMQ_PORT</code> (5672),
 *       credenciais <code>RABBITMQ_USER/RABBITMQ_PASS</code>.</li>
 *   <li>WildFly + Postgres sob Docker Compose; Wicket mapeado em <code>/wicket/*</code> e a API em <code>/api/*</code>.</li>
 * </ul>
 *
 * <h2>Notas para produção (quando/ se evoluir)</h2>
 * <ul>
 *   <li>Tornar fila <code>durable=true</code> e mensagens persistentes (<code>deliveryMode=2</code>).</li>
 *   <li>Usar exchange (ex.: <i>fanout</i>) + bindings para broadcast.</li>
 *   <li>Acks manuais, DLQ e Publisher Confirms.</li>
 *   <li>Considerar SSE/WebSocket se precisar de push contínuo bidirecional.</li>
 * </ul>
 */
public class AppGameApplication extends WebApplication {
    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        super.init();

        // Desativa CSP completamente (só para DEV)
        getCspSettings().blocking().disabled();
    }
}
