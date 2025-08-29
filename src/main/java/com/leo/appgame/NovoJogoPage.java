package com.leo.appgame;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NovoJogoPage extends BasePage {
    private final Model<String> timeA = Model.of("");
    private final Model<String> timeB = Model.of("");
    private final Model<Integer> placarA = Model.of(0);
    private final Model<Integer> placarB = Model.of(0);
    private final Model<String> status = Model.of("EM_ANDAMENTO");
    private final Model<String> dataHora = Model.of("");

    private static final DateTimeFormatter HTML5_DT = DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm");

    public NovoJogoPage() {
        Form<Void> form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                try {
                    LocalDateTime ldt = null;

                    info(dataHora.getObject());
                    if (dataHora.getObject() != null && !dataHora.getObject().isBlank()) {
                        ldt = LocalDateTime.parse(dataHora.getObject(), HTML5_DT);
                    }

                    // monta JSON para o endpoint /api/jogos
                    JsonObject body = Json.createObjectBuilder()
                            .add("timeA", timeA.getObject())
                            .add("timeB", timeB.getObject())
                            .add("placarA", placarA.getObject() == null ? 0 : placarA.getObject())
                            .add("placarB", placarB.getObject() == null ? 0 : placarB.getObject())
                            .add("status", status.getObject())
                            .add("dataHoraPartida", ldt == null ? "" : ldt.toString())
                            .build();

                    String url;
                    try {
                        String base = getUrlBase();
                        url = base + "/api/jogos";
                    } catch (Exception e) {
                        // Fallback seguro
                        url = "http://localhost:8080/api/jogos"; // Ajuste se necessário
                        error("Erro ao montar URL. Usando fallback: " + url);
                    }

                    //todo colocar try-with-resources
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
                        status.setObject("EM_ANDAMENTO");
                        dataHora.setObject("");
                    } else {
                        error("Falha ao cadastrar jogo: HTTP " + resp.statusCode() + " - " + resp.body());
                    }
                } catch (Exception e) {
                    error("Erro ao cadastrar jogo: " + e.getMessage());
                }
            }

            private String getUrlBase() {
                ServletWebRequest servletRequest = (ServletWebRequest) getRequest();
                var req = servletRequest.getContainerRequest();

                String scheme = req.getScheme();     // "http" ou "https"
                String serverName = req.getServerName();
                int serverPort = req.getServerPort();
                String contextPath = req.getContextPath(); // "/app-game-1.0-SNAPSHOT"

                // Monta a base corretamente
                return scheme + "://" + serverName +
                        (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) +
                        contextPath;
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
                Model.ofList(java.util.List.of("EM_ANDAMENTO", "FINALIZADO"))).setRequired(true));

        form.add(new TextField<>("dataHora", dataHora));
        form.add(new Button("salvar"));
    }

}
