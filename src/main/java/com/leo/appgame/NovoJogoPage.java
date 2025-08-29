package com.leo.appgame;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NovoJogoPage extends WebPage {

    private final Model<String> timeA = Model.of("");
    private final Model<String> timeB = Model.of("");
    private final Model<Integer> placarA = Model.of(0);
    private final Model<Integer> placarB = Model.of(0);
    private final Model<String> status = Model.of("NAO_INICIADO");
    private final Model<String> dataHora = Model.of("");

    private static final DateTimeFormatter HTML5_DT = DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm");

    public NovoJogoPage() {
        add(new FeedbackPanel("feedback"));

        Form<Void> form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                try {
                    // converte data/hora
                    LocalDateTime ldt = null;
                    if (dataHora.getObject() != null && !dataHora.getObject().isBlank()) {
                        ldt = LocalDateTime.parse(dataHora.getObject(), HTML5_DT);
                    }

                    // monta JSON para o endpoint /api/jogos
                    JsonObject body = Json.createObjectBuilder()
                            .add("timeA", timeA.getObject())
                            .add("timeB", timeB.getObject())
                            .add("placarA", placarA.getObject() == null ? 0 : placarA.getObject())
                            .add("placarB", placarB.getObject() == null ? 0 : placarB.getObject())
                            .add("status", status.getObject()) // ex.: NAO_INICIADO | EM_ANDAMENTO | FINALIZADO
                            .add("dataHoraPartida", ldt == null ? "" : ldt.toString())
                            .build();

                    // monta URL base usando o mesmo context-root da app
                    String base = getRequest().getClientUrl().toString(); // p.ex. http://localhost:8080/app/...
                    // pega só o esquema+host+porta+contextRoot:
                    String contextRoot = getRequestCycle().getUrlRenderer().renderContextRelativeUrl("/");
                    String origin = base.substring(0, base.indexOf(contextRoot) + contextRoot.length());
                    String url = origin + "api/jogos";

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        info("Jogo cadastrado com sucesso!");
                        // limpa os campos
                        timeA.setObject("");
                        timeB.setObject("");
                        placarA.setObject(0);
                        placarB.setObject(0);
                        status.setObject("NAO_INICIADO");
                        dataHora.setObject("");
                    } else {
                        error("Falha ao cadastrar jogo: HTTP " + resp.statusCode() + " - " + resp.body());
                    }
                } catch (Exception e) {
                    error("Erro ao cadastrar jogo: " + e.getMessage());
                }
            }
        };
        add(form);

        form.add(new TextField<>("timeA", timeA).setRequired(true));
        form.add(new TextField<>("timeB", timeB).setRequired(true));

        NumberTextField<Integer> nfA = new NumberTextField<>("placarA", placarA, Integer.class);
        nfA.setMinimum(0);
        NumberTextField<Integer> nfB = new NumberTextField<>("placarB", placarB, Integer.class);
        nfB.setMinimum(0);
        form.add(nfA, nfB);

        form.add(new DropDownChoice<>("status", status,
                Model.ofList(java.util.List.of("NAO_INICIADO","EM_ANDAMENTO","FINALIZADO"))).setRequired(true));

        // campo texto que usa input type="datetime-local" no HTML
        form.add(new TextField<>("dataHora", dataHora));

        form.add(new Button("salvar"));
    }

}
