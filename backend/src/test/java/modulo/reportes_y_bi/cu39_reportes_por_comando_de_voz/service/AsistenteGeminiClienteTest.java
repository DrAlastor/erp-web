package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConversarRequest.Turno;
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

/** Prueba el ciclo de function calling contra un servidor que imita la API de Gemini. */
class AsistenteGeminiClienteTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> peticiones = new ArrayList<>();
    private final Deque<String[]> respuestas = new ConcurrentLinkedDeque<>();
    private HttpServer servidor;

    private AsistenteGeminiCliente cliente(String key) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1beta/models/", ex -> {
            peticiones.add(json.readTree(ex.getRequestBody()));
            assertThat(ex.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo(key);
            String[] r = respuestas.poll();
            byte[] cuerpo = r[1].getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(Integer.parseInt(r[0]), cuerpo.length);
            ex.getResponseBody().write(cuerpo);
            ex.close();
        });
        servidor.start();
        return new AsistenteGeminiCliente(RestClient.builder(), json, key, "gemini-2.5-flash",
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta", 5000);
    }

    @AfterEach
    void detener() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void ejecutaLaHerramientaQuePideGeminiYDevuelveSuRespuesta() throws Exception {
        respuestas.add(new String[]{"200", """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"consultar_reporte","args":{"metrica":"VENTAS","agrupacion":"SUCURSAL"}},
                   "thoughtSignature":"firma-123"}]}}]}"""});
        respuestas.add(new String[]{"200", """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"pensando...","thought":true},
                  {"text":"Vendiste 33.850 bolivianos, todo en la sucursal Central."}]}}]}"""});
        List<JsonNode> argumentos = new ArrayList<>();

        String texto = cliente("clave-prueba").conversar("instrucciones",
                List.of(new Turno("usuario", "hola"), new Turno("asistente", "¿En qué te ayudo?")),
                "ventas por sucursal",
                args -> {
                    argumentos.add(args);
                    return json.createObjectNode().put("titulo", "Ventas por sucursal");
                });

        assertThat(texto).isEqualTo("Vendiste 33.850 bolivianos, todo en la sucursal Central.");
        assertThat(argumentos).hasSize(1);
        assertThat(argumentos.get(0).get("agrupacion").asText()).isEqualTo("SUCURSAL");

        JsonNode primera = peticiones.get(0);
        assertThat(primera.at("/contents/0/role").asText()).isEqualTo("user");
        assertThat(primera.at("/contents/1/role").asText()).isEqualTo("model");
        assertThat(primera.at("/contents/2/parts/0/text").asText()).isEqualTo("ventas por sucursal");
        assertThat(primera.at("/tools/0/functionDeclarations/0/name").asText()).isEqualTo("consultar_reporte");
        assertThat(primera.at("/generationConfig/temperature").asDouble()).isEqualTo(0.2);

        // El turno del modelo vuelve tal cual (con su firma) seguido del resultado de la herramienta.
        JsonNode segunda = peticiones.get(1);
        assertThat(segunda.at("/contents/3/parts/0/thoughtSignature").asText()).isEqualTo("firma-123");
        assertThat(segunda.at("/contents/4/parts/0/functionResponse/name").asText()).isEqualTo("consultar_reporte");
        assertThat(segunda.at("/contents/4/parts/0/functionResponse/response/titulo").asText())
                .isEqualTo("Ventas por sucursal");
    }

    @Test
    void limiteGratuitoYFaltaDeKeySeInformanComoNoDisponible() throws Exception {
        respuestas.add(new String[]{"429", "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}"});
        AsistenteGeminiCliente conLimite = cliente("clave-prueba");
        assertThatThrownBy(() -> conLimite.conversar("i", List.of(), "ventas", a -> a))
                .isInstanceOf(AsistenteGeminiCliente.GeminiNoDisponibleException.class)
                .hasMessageContaining("límite de uso gratuito");

        AsistenteGeminiCliente sinKey = new AsistenteGeminiCliente(RestClient.builder(), json, " ",
                "gemini-2.5-flash", "http://127.0.0.1:1", 1000);
        assertThat(sinKey.habilitado()).isFalse();
        assertThatThrownBy(() -> sinKey.conversar("i", List.of(), "ventas", a -> a))
                .isInstanceOf(AsistenteGeminiCliente.GeminiNoDisponibleException.class);
    }
}
