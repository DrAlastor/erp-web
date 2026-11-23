package modulo.inteligencia_de_negocio.cu41_agente_ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * CU41 - Cliente REST de Google Gemini (AI Studio, {@code GEMINI_API_KEY}) para el agente.
 *
 * <p>Una llamada = un turno del modelo con las declaraciones de herramientas. Gemini nunca ve la
 * base ni escribe SQL: solo devuelve {@code functionCall}s que el backend valida y ejecuta.
 * Errores: 503 de Google se reintenta una vez tras 1,5 s; 429 = cuota de Google agotada.
 */
@Component
public class GeminiAgenteCliente {

    public enum Falla { SIN_CONFIGURAR, CUOTA, NO_DISPONIBLE }

    public static class GeminiException extends RuntimeException {
        private final Falla falla;

        public GeminiException(Falla falla, String mensaje) {
            super(mensaje);
            this.falla = falla;
        }

        public Falla falla() {
            return falla;
        }
    }

    public static final int MAX_TOKENS_SALIDA = 2048;
    private static final Logger log = LoggerFactory.getLogger(GeminiAgenteCliente.class);

    private final RestClient rest;
    private final ObjectMapper json;
    private final String apiKey;
    private final String modelo;
    private final String urlBase;
    private final long esperaReintentoMs;

    @Autowired
    public GeminiAgenteCliente(RestClient.Builder builder, ObjectMapper json,
                               @Value("${gemini.api-key:}") String apiKey,
                               @Value("${agente.gemini-model:${gemini.model:gemini-flash-latest}}") String modelo,
                               @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String urlBase,
                               @Value("${agente.timeout-ms:30000}") int timeoutMs) {
        this(builder, json, apiKey, modelo, urlBase, timeoutMs, 1500);
    }

    GeminiAgenteCliente(RestClient.Builder builder, ObjectMapper json, String apiKey, String modelo, String urlBase,
                        int timeoutMs, long esperaReintentoMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(timeoutMs, 10_000));
        factory.setReadTimeout(timeoutMs);
        this.rest = builder.requestFactory(factory).build();
        this.json = json;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo == null || modelo.isBlank() ? "gemini-flash-latest" : modelo.trim();
        this.urlBase = urlBase.replaceAll("/+$", "");
        this.esperaReintentoMs = esperaReintentoMs;
    }

    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    public String modelo() {
        return modelo;
    }

    /** Cuerpo del pedido (separado para poder probarlo sin red). */
    ObjectNode cuerpo(String instrucciones, ArrayNode contenidos, ArrayNode declaraciones, boolean sinHerramientas) {
        ObjectNode c = json.createObjectNode();
        c.set("systemInstruction", json.createObjectNode()
                .set("parts", json.createArrayNode().add(json.createObjectNode().put("text", instrucciones))));
        c.set("contents", contenidos);
        c.set("tools", json.createArrayNode().add(json.createObjectNode().set("functionDeclarations", declaraciones)));
        if (sinHerramientas) {
            c.putObject("toolConfig").putObject("functionCallingConfig").put("mode", "NONE");
        }
        ObjectNode g = c.putObject("generationConfig");
        g.put("temperature", 0.2);
        g.put("maxOutputTokens", MAX_TOKENS_SALIDA);
        if (modelo.startsWith("gemini-3")) {
            // Los tokens de razonamiento se cobran como salida: razonamiento bajo.
            g.putObject("thinkingConfig").put("thinkingLevel", "low");
        }
        return c;
    }

    public JsonNode generar(String instrucciones, ArrayNode contenidos, ArrayNode declaraciones, boolean sinHerramientas) {
        if (!configurado()) {
            throw new GeminiException(Falla.SIN_CONFIGURAR,
                    "El asistente de IA no esta configurado (falta GEMINI_API_KEY en el backend).");
        }
        ObjectNode cuerpo = cuerpo(instrucciones, contenidos, declaraciones, sinHerramientas);
        for (int intento = 1; ; intento++) {
            try {
                JsonNode r = rest.post()
                        .uri(urlBase + "/models/{modelo}:generateContent", modelo)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .retrieve()
                        .body(JsonNode.class);
                if (r == null) throw new GeminiException(Falla.NO_DISPONIBLE, "El servicio de IA devolvio una respuesta vacia.");
                return r;
            } catch (RestClientResponseException e) {
                int estado = e.getStatusCode().value();
                String detalle = e.getResponseBodyAsString().replaceAll("\\s+", " ");
                log.warn("CU41: Gemini respondio HTTP {} (intento {}): {}", estado, intento,
                        detalle.length() > 300 ? detalle.substring(0, 300) : detalle);
                if (estado == 503 && intento == 1) {
                    dormir();
                    continue;
                }
                if (estado == 429) {
                    throw new GeminiException(Falla.CUOTA, "Se agoto la cuota del servicio de IA; intenta mas tarde.");
                }
                throw new GeminiException(Falla.NO_DISPONIBLE, "El servicio de IA no esta disponible (HTTP " + estado + ").");
            } catch (GeminiException e) {
                throw e;
            } catch (Exception e) {
                log.warn("CU41: no se pudo contactar a Gemini: {}", e.getMessage());
                throw new GeminiException(Falla.NO_DISPONIBLE, "No se pudo contactar al servicio de IA.");
            }
        }
    }

    private void dormir() {
        try {
            Thread.sleep(esperaReintentoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
