package modulo.inteligencia_de_negocio.cu41_agente_ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba el cliente contra un servidor que imita la API de Gemini (no gasta cuota). */
class GeminiAgenteClienteTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> peticiones = new ArrayList<>();
    private final List<String> rutas = new ArrayList<>();
    private final Deque<String[]> respuestas = new ConcurrentLinkedDeque<>();
    private HttpServer servidor;

    private GeminiAgenteCliente cliente(String key, String modelo) throws IOException {
        return cliente(key, modelo, "");
    }

    private GeminiAgenteCliente cliente(String key, String modelo, String respaldo) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1beta/models/", ex -> {
            rutas.add(ex.getRequestURI().getPath());
            peticiones.add(json.readTree(ex.getRequestBody()));
            String[] r = respuestas.poll();
            byte[] b = r[1].getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(Integer.parseInt(r[0]), b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        servidor.start();
        return new GeminiAgenteCliente(RestClient.builder(), json, key, modelo, respaldo,
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta", 5000, 10);
    }

    @AfterEach
    void detener() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void configuraHerramientasRazonamientoBajoYTopeDeSalida() throws Exception {
        GeminiAgenteCliente c = cliente("k", "gemini-3.5-flash-lite");
        JsonNode cuerpo = c.cuerpo("sistema", json.createArrayNode(), json.createArrayNode().add(json.createObjectNode().put("name", "x")), false);
        assertThat(cuerpo.path("generationConfig").path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("low");
        assertThat(cuerpo.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(2048);
        assertThat(cuerpo.path("generationConfig").path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(cuerpo.path("tools").get(0).path("functionDeclarations")).hasSize(1);
        assertThat(cuerpo.has("toolConfig")).isFalse();
        JsonNode cierre = c.cuerpo("s", json.createArrayNode(), json.createArrayNode(), true);
        assertThat(cierre.path("toolConfig").path("functionCallingConfig").path("mode").asText()).isEqualTo("NONE");
        GeminiAgenteCliente viejo = cliente("k", "gemini-2.5-flash");
        assertThat(viejo.cuerpo("s", json.createArrayNode(), json.createArrayNode(), false)
                .path("generationConfig").has("thinkingConfig")).isFalse();
    }

    @Test
    void reintentaUnaVezAnte503() throws Exception {
        // Cuerpo con saltos de linea y sangria como los de Gemini: al compactarlo queda mas corto que el original.
        respuestas.add(new String[]{"503", "{\n  \"error\": {\n    \"code\": 503,\n    \"message\": \"The model is overloaded.\",\n    \"status\": \"UNAVAILABLE\"\n  }\n}\n"});
        respuestas.add(new String[]{"200", "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}"});
        JsonNode r = cliente("k", "gemini-3.5-flash-lite").generar("s", json.createArrayNode(), json.createArrayNode(), false);
        assertThat(r.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText()).isEqualTo("ok");
        assertThat(peticiones).hasSize(2);
    }

    @Test
    void dos503SeguidosONoDisponibleY429EsCuota() throws Exception {
        respuestas.add(new String[]{"503", "{}"});
        respuestas.add(new String[]{"503", "{}"});
        GeminiAgenteCliente c = cliente("k", "gemini-3.5-flash-lite");
        assertThatThrownBy(() -> c.generar("s", json.createArrayNode(), json.createArrayNode(), false))
                .isInstanceOfSatisfying(GeminiAgenteCliente.GeminiException.class,
                        e -> assertThat(e.falla()).isEqualTo(GeminiAgenteCliente.Falla.NO_DISPONIBLE));
        respuestas.add(new String[]{"429", "{}"});
        assertThatThrownBy(() -> c.generar("s", json.createArrayNode(), json.createArrayNode(), false))
                .isInstanceOfSatisfying(GeminiAgenteCliente.GeminiException.class,
                        e -> assertThat(e.falla()).isEqualTo(GeminiAgenteCliente.Falla.CUOTA));
    }

    @Test
    void modeloSaturadoPasaAlDeRespaldoPeroLaCuotaNo() throws Exception {
        // Principal: 503 + 503 (con reintento) -> respaldo: 429 (la cuota es de la clave, no del modelo).
        respuestas.add(new String[]{"503", "{}"});
        respuestas.add(new String[]{"503", "{}"});
        respuestas.add(new String[]{"429", "{}"});
        GeminiAgenteCliente c = cliente("k", "gemini-flash-latest", "gemini-flash-lite-latest");
        assertThatThrownBy(() -> c.generar("s", json.createArrayNode(), json.createArrayNode(), false))
                .isInstanceOfSatisfying(GeminiAgenteCliente.GeminiException.class,
                        e -> assertThat(e.falla()).isEqualTo(GeminiAgenteCliente.Falla.CUOTA));
        assertThat(rutas).hasSize(3);
        assertThat(rutas.get(2)).endsWith("/gemini-flash-lite-latest:generateContent");

        respuestas.add(new String[]{"503", "{}"});
        respuestas.add(new String[]{"503", "{}"});
        respuestas.add(new String[]{"200", "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}"});
        JsonNode r = c.generar("s", json.createArrayNode(), json.createArrayNode(), false);
        assertThat(r.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText()).isEqualTo("ok");
        assertThat(rutas.get(5)).endsWith("/gemini-flash-lite-latest:generateContent");
    }

    @Test
    void sinApiKeyNoLlama() throws Exception {
        GeminiAgenteCliente c = cliente("", "gemini-3.5-flash-lite");
        assertThat(c.configurado()).isFalse();
        assertThatThrownBy(() -> c.generar("s", json.createArrayNode(), json.createArrayNode(), false))
                .isInstanceOfSatisfying(GeminiAgenteCliente.GeminiException.class,
                        e -> assertThat(e.falla()).isEqualTo(GeminiAgenteCliente.Falla.SIN_CONFIGURAR));
        assertThat(peticiones).isEmpty();
    }
}
