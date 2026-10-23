package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConversarRequest.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Cliente del asistente conversacional del CU39 sobre la API de Google Gemini.
 *
 * <p>Usa function calling: Gemini no ve la base de datos ni escribe SQL; cuando necesita
 * cifras pide la herramienta {@code consultar_reporte} con una consulta estructurada, el
 * backend la valida contra el catálogo y la ejecuta con SQL fijo, y Gemini redacta la
 * respuesta con esos datos. La API key se lee de {@code GEMINI_API_KEY}.
 */
@Component
public class AsistenteGeminiCliente {

    /** Gemini no está configurado, alcanzó el límite gratuito o respondió con error. */
    public static class GeminiNoDisponibleException extends RuntimeException {
        public GeminiNoDisponibleException(String mensaje) {
            super(mensaje);
        }
    }

    static final String HERRAMIENTA = "consultar_reporte";
    private static final int MAXIMO_PASOS = 5;
    private static final Logger log = LoggerFactory.getLogger(AsistenteGeminiCliente.class);

    private final ObjectMapper json;
    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final String urlBase;

    public AsistenteGeminiCliente(
            RestClient.Builder builder,
            ObjectMapper json,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-flash-latest}") String modelo,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String urlBase,
            @Value("${gemini.timeout-ms:20000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = builder.requestFactory(factory).build();
        this.json = json;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo == null || modelo.isBlank() ? "gemini-flash-latest" : modelo.trim();
        this.urlBase = urlBase.replaceAll("/+$", "");
    }

    public boolean habilitado() {
        return !apiKey.isEmpty();
    }

    /**
     * Conversa con Gemini hasta obtener una respuesta en texto. Cada vez que Gemini pide la
     * herramienta, se ejecuta {@code herramienta} con sus argumentos y se le devuelve el
     * resultado.
     *
     * @throws GeminiNoDisponibleException si no está configurado o la API falla
     */
    public String conversar(String instrucciones, List<Turno> historial, String mensaje,
                            Function<JsonNode, JsonNode> herramienta) {
        if (!habilitado()) {
            throw new GeminiNoDisponibleException("El asistente IA no está configurado.");
        }
        ArrayNode contenidos = json.createArrayNode();
        for (Turno t : historial == null ? List.<Turno>of() : historial) {
            if (t.texto() == null || t.texto().isBlank()) continue;
            contenidos.add(contenido("asistente".equals(t.rol()) ? "model" : "user", t.texto()));
        }
        contenidos.add(contenido("user", mensaje));

        for (int paso = 0; paso < MAXIMO_PASOS; paso++) {
            JsonNode respuesta = llamar(instrucciones, contenidos);
            JsonNode contenido = respuesta.path("candidates").path(0).path("content");
            if (!contenido.has("parts")) {
                String motivo = respuesta.path("candidates").path(0).path("finishReason").asText(
                        respuesta.path("promptFeedback").path("blockReason").asText("sin contenido"));
                throw new GeminiNoDisponibleException("Gemini no devolvió una respuesta (" + motivo + ").");
            }
            // Se devuelve el turno del modelo tal cual: puede traer firmas de razonamiento.
            contenidos.add(contenido);

            ArrayNode respuestasHerramienta = json.createArrayNode();
            StringBuilder texto = new StringBuilder();
            for (JsonNode parte : contenido.path("parts")) {
                if (parte.has("functionCall")) {
                    JsonNode llamada = parte.get("functionCall");
                    JsonNode resultado = HERRAMIENTA.equals(llamada.path("name").asText())
                            ? herramienta.apply(llamada.path("args"))
                            : json.createObjectNode().put("error", "Herramienta desconocida.");
                    ObjectNode functionResponse = json.createObjectNode();
                    functionResponse.put("name", llamada.path("name").asText());
                    functionResponse.set("response", resultado);
                    respuestasHerramienta.add(json.createObjectNode().set("functionResponse", functionResponse));
                } else if (parte.has("text") && !parte.path("thought").asBoolean(false)) {
                    texto.append(parte.get("text").asText());
                }
            }
            if (respuestasHerramienta.isEmpty()) {
                return texto.toString().trim();
            }
            ObjectNode turnoHerramienta = json.createObjectNode().put("role", "user");
            turnoHerramienta.set("parts", respuestasHerramienta);
            contenidos.add(turnoHerramienta);
        }
        throw new GeminiNoDisponibleException("El asistente necesitó demasiados pasos para responder.");
    }

    private JsonNode llamar(String instrucciones, ArrayNode contenidos) {
        ObjectNode cuerpo = json.createObjectNode();
        cuerpo.set("systemInstruction", json.createObjectNode()
                .set("parts", json.createArrayNode().add(json.createObjectNode().put("text", instrucciones))));
        cuerpo.set("contents", contenidos);
        cuerpo.set("tools", json.createArrayNode().add(json.createObjectNode()
                .set("functionDeclarations", json.createArrayNode().add(declaracionHerramienta()))));
        ObjectNode generacion = cuerpo.putObject("generationConfig");
        generacion.put("temperature", 0.2);
        // Holgado porque en los modelos con razonamiento los tokens de pensamiento también cuentan.
        generacion.put("maxOutputTokens", 4096);
        try {
            JsonNode respuesta = restClient.post()
                    .uri(urlBase + "/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null) {
                throw new GeminiNoDisponibleException("Gemini devolvió una respuesta vacía.");
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            log.warn("CU39: Gemini respondió HTTP {}: {}", e.getStatusCode().value(),
                    e.getResponseBodyAsString().replaceAll("\\s+", " "));
            throw new GeminiNoDisponibleException(e.getStatusCode().value() == 429
                    ? "Se alcanzó el límite de uso gratuito de Gemini; intenta en un minuto."
                    : "Gemini respondió con un error (HTTP " + e.getStatusCode().value() + ").");
        } catch (GeminiNoDisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.warn("CU39: no se pudo contactar a Gemini: {}", e.getMessage());
            throw new GeminiNoDisponibleException("No se pudo contactar al asistente IA.");
        }
    }

    private ObjectNode contenido(String rol, String texto) {
        ObjectNode c = json.createObjectNode().put("role", rol);
        c.set("parts", json.createArrayNode().add(json.createObjectNode().put("text", texto)));
        return c;
    }

    /** La única herramienta: los mismos parámetros que la consulta estructurada del CU39. */
    private ObjectNode declaracionHerramienta() {
        ObjectNode d = json.createObjectNode();
        d.put("name", HERRAMIENTA);
        d.put("description", """
                Consulta los datos del ERP y devuelve un reporte. Úsala siempre que necesites cifras; \
                nunca inventes datos. Métricas: VENTAS (facturas: monto, cantidad, ticket promedio), \
                PRODUCTOS_VENDIDOS (unidades e importe por producto o categoría), STOCK (existencias \
                actuales; no usa fechas), MOVIMIENTOS_KARDEX (entradas y salidas de inventario), \
                CUENTAS_POR_COBRAR (saldos de clientes), COMPRAS (compras a proveedores). Para comparar \
                periodos, llámala una vez por periodo.""");
        ObjectNode params = d.putObject("parameters");
        params.put("type", "OBJECT");
        ObjectNode props = params.putObject("properties");
        enumerado(props, "metrica", "Qué reporte consultar.",
                Arrays.stream(MetricaVoz.values()).map(Enum::name).toList());
        enumerado(props, "agrupacion", "Cómo agrupar. NINGUNA da el total general; DIA y MES son series en el tiempo.",
                Arrays.stream(AgrupacionVoz.values()).map(Enum::name).toList());
        props.putObject("fechaInicio").put("type", "STRING").put("description", "Inicio del rango, AAAA-MM-DD (inclusive).");
        props.putObject("fechaFin").put("type", "STRING").put("description", "Fin del rango, AAAA-MM-DD (inclusive).");
        props.putObject("sucursalId").put("type", "INTEGER").put("description", "Id de la sucursal; omitir para todas.");
        props.putObject("productoId").put("type", "INTEGER")
                .put("description", "Id del producto; solo PRODUCTOS_VENDIDOS, STOCK y MOVIMIENTOS_KARDEX.");
        props.putObject("soloCriticos").put("type", "BOOLEAN")
                .put("description", "STOCK: solo bajo el mínimo. CUENTAS_POR_COBRAR: solo vencidas con saldo.");
        props.putObject("limite").put("type", "INTEGER").put("description", "Cantidad máxima de filas en rankings (1 a 100).");
        params.set("required", json.createArrayNode().add("metrica"));
        return d;
    }

    private static void enumerado(ObjectNode props, String nombre, String descripcion, List<String> valores) {
        ObjectNode p = props.putObject(nombre);
        p.put("type", "STRING");
        p.put("description", descripcion);
        ArrayNode lista = p.putArray("enum");
        valores.forEach(v -> lista.add(v.toUpperCase(Locale.ROOT)));
    }
}
