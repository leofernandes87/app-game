package com.leo.appgame;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;

import java.io.Serial;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Se você tem uma BasePage Java, troque para "extends BasePage". */
public class TodosJogosPage extends BasePage {

    @Serial
    private static final long serialVersionUID = 1L;

    // Filtros (modelos)
    private final Model<String> status = Model.of("");     // "", EM_ANDAMENTO, FINALIZADO
    private final Model<String> de     = Model.of("");     // yyyy-MM-dd'T'HH:mm
    private final Model<String> ate    = Model.of("");     // yyyy-MM-dd'T'HH:mm
    private static final DateTimeFormatter HTML5_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // Dados renderizados
    private final List<JogoVM> jogos = new ArrayList<>();

    @Override
    protected void onInitialize() {
        super.onInitialize();

        // Painel de feedback local (independente do da BasePage)
        // Componentes para atualizar mensagens/tabela
        FeedbackPanel feedback = new FeedbackPanel("localFeedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        // Form de filtros (submit normal, sem Ajax)
        Form<Void> form = new Form<>("filtrosForm") {
            @Override
            protected void onSubmit() {
                carregarJogos();
                info("Lista atualizada.");
            }
        };
        form.setOutputMarkupId(true);
        add(form);

        // Status
        List<String> opcoes = List.of("", "NAO_INICIADO", "EM_ANDAMENTO", "FINALIZADO");
        form.add(new DropDownChoice<>("status", status, opcoes));

        // Campos data/hora (como datetime-local no HTML)
        TextField<String> campoDe  = new TextField<>("de", de);
        campoDe.add(AttributeModifier.replace("type", "datetime-local"));
        TextField<String> campoAte = new TextField<>("ate", ate);
        campoAte.add(AttributeModifier.replace("type", "datetime-local"));
        form.add(campoDe, campoAte);

        // Botão limpar (sem validação)
        Button limpar = new Button("limpar") {
            @Override
            public void onSubmit() {
                status.setObject("");
                de.setObject("");
                ate.setObject("");
                carregarJogos();
                info("Filtros limpos.");
            }
        };
        limpar.setDefaultFormProcessing(false);
        form.add(limpar);

        // Tabela (ListView puro, sem JS)
        ListView<JogoVM> tabela = getComponents();
        add(tabela);

        // Carrega inicialmente (sem filtros)
        carregarJogos();
    }

    private ListView<JogoVM> getComponents() {
        ListView<JogoVM> tabela = new ListView<>("rows", jogos) {
            @Override
            protected void populateItem(ListItem<JogoVM> item) {
                JogoVM j = item.getModelObject();
                item.add(new Label("id", j.id == null ? "" : j.id.toString()));
                item.add(new Label("timeA", j.timeA == null ? "" : j.timeA));
                item.add(new Label("timeB", j.timeB == null ? "" : j.timeB));
                item.add(new Label("dataHora", j.dataHoraPartidaFmt()));
                item.add(new Label("placar", j.placarFmt()));
                item.add(new Label("status", j.statusFmt()));
            }
        };
        tabela.setOutputMarkupId(true);
        return tabela;
    }

    /** Busca /api/jogos no servidor, aplica filtros, parseia JSON e popula a lista. */
    private void carregarJogos() {
        try {
            String url = montarApiUrlComFiltros();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (!respIsOk(resp.statusCode())) {
                error("Falha ao carregar jogos: HTTP " + resp.statusCode() + " - " + resp.body());
                jogos.clear();
                return;
            }

            String body = resp.body();
            jogos.clear();
            if (body != null && !body.isBlank()) {
                try (JsonReader reader = Json.createReader(new StringReader(body))) {
                    JsonArray arr = reader.readArray();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject o = arr.getJsonObject(i);
                        JogoVM j = new JogoVM();
                        j.id = o.isNull("id") ? null : o.getJsonNumber("id").longValue();
                        j.timeA = o.isNull("timeA") ? null : o.getString("timeA", null);
                        j.timeB = o.isNull("timeB") ? null : o.getString("timeB", null);
                        j.placarA = o.isNull("placarA") ? null : toIntSafe(o, "placarA");
                        j.placarB = o.isNull("placarB") ? null : toIntSafe(o, "placarB");

                        // status pode vir como "statusJogo" ou "status"
                        j.status = o.containsKey("statusJogo") && !o.isNull("statusJogo")
                                ? o.getString("statusJogo", null)
                                : (o.isNull("status") ? null : o.getString("status", null));

                        // data/hora em ISO-8601
                        String dt = o.isNull("dataHoraPartida") ? null : o.getString("dataHoraPartida", null);
                        j.dataHoraPartida = parseIsoLocal(dt);

                        jogos.add(j);
                    }
                }
            }
        } catch (Exception e) {
            error("Erro ao carregar jogos: " + e.getMessage());
            jogos.clear();
            e.printStackTrace();
        }
    }

    private String montarApiUrlComFiltros() {
        // base: http(s)://host:porta/context
        ServletWebRequest swr = (ServletWebRequest) getRequest();
        var req = swr.getContainerRequest();

        String scheme = req.getScheme();
        String host   = req.getServerName();
        int port      = req.getServerPort();
        String ctx    = req.getContextPath();

        String base = scheme + "://" + host + ((port == 80 || port == 443) ? "" : ":" + port) + ctx;
        String api  = base + "/api/jogos";

        StringBuilder qs = new StringBuilder();
        if (status.getObject() != null && !status.getObject().isBlank()) {
            addParam(qs, "status", status.getObject());
        }
        if (de.getObject() != null && !de.getObject().isBlank()) {
            // já vem como yyyy-MM-dd'T'HH:mm → a API aceita ISO truncado
            addParam(qs, "de", de.getObject());
        }
        if (ate.getObject() != null && !ate.getObject().isBlank()) {
            addParam(qs, "ate", ate.getObject());
        }
        return !qs.isEmpty() ? api + "?" + qs : api;
    }

    private static void addParam(StringBuilder qs, String key, String value) {
        if (!qs.isEmpty()) qs.append('&');
        qs.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        qs.append('=');
        qs.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static boolean respIsOk(int sc) {
        return sc >= 200 && sc < 300;
    }

    private static Integer toIntSafe(JsonObject o, String key) {
        try {
            return o.getJsonNumber(key).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseIsoLocal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // aceita "2025-08-28T20:00" ou "2025-08-28T20:00:00"
            return s.length() == 16 ? LocalDateTime.parse(s, HTML5_DT) : LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** View model simples para a tabela. */
    public static class JogoVM {
        Long id;
        String timeA;
        String timeB;
        Integer placarA;
        Integer placarB;
        String status;
        LocalDateTime dataHoraPartida;

        String placarFmt() {
            int a = placarA == null ? 0 : placarA;
            int b = placarB == null ? 0 : placarB;
            return a + " x " + b;
        }

        String statusFmt() {
            if (status == null) return "-";
            return switch (status.toUpperCase()) {
                case "EM_ANDAMENTO" -> "Em andamento";
                case "FINALIZADO"   -> "Finalizado";
                default -> status;
            };
        }

        String dataHoraPartidaFmt() {
            if (dataHoraPartida == null) return "";
            // Mostra no formato local do servidor; ajuste se quiser outro padrão
            return dataHoraPartida.toString().replace('T', ' ');
        }
    }
}
