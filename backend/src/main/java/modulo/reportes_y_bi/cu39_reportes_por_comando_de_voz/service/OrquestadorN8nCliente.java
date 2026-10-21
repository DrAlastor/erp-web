package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import com.fasterxml.jackson.databind.JsonNode;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Entidad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente del flujo de N8N que interpreta los comandos de voz (HU39.2).
 *
 * <p>Envía la frase junto con el contexto que el flujo necesita para resolverla (fecha de
 * referencia, sucursales, productos y el catálogo de métricas) y espera de vuelta una
 * {@link ConsultaReporteVoz} en JSON. La respuesta se trata como dato no confiable: solo se
 * aceptan valores del catálogo y tipos estrictos, y ante cualquier error el servicio cae al
 * intérprete local.
 */
@Component
public class OrquestadorN8nCliente {

    private static final Logger log = LoggerFactory.getLogger(OrquestadorN8nCliente.class);

    private final String webhookUrl;
    private final String token;
    private final RestClient restClient;

    public OrquestadorN8nCliente(
            RestClient.Builder builder,
            @Value("${n8n.reportes-voz.webhook-url:}") String webhookUrl,
            @Value("${n8n.reportes-voz.token:}") String token,
            @Value("${n8n.reportes-voz.timeout-ms:8000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = builder.requestFactory(factory).build();
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.token = token == null ? "" : token.trim();
    }

    public boolean habilitado() {
        return !webhookUrl.isEmpty();
    }

    /** La consulta que propone N8N, o vacío si no está configurado, falla o no entendió la frase. */
    public Optional<ConsultaReporteVoz> interpretar(
            String texto, List<Entidad> sucursales, List<Entidad> productos, LocalDate hoy) {
        if (!habilitado()) {
            return Optional.empty();
        }
        try {
            JsonNode respuesta = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (!token.isEmpty()) h.set("X-ERP-Token", token);
                    })
                    .body(peticion(texto, sucursales, productos, hoy))
                    .retrieve()
                    .body(JsonNode.class);
            return convertir(respuesta);
        } catch (Exception e) {
            log.warn("CU39: el flujo de N8N no respondió correctamente ({}); se usa el intérprete local",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> peticion(
            String texto, List<Entidad> sucursales, List<Entidad> productos, LocalDate hoy) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("texto", texto);
        cuerpo.put("fechaReferencia", hoy.toString());
        cuerpo.put("sucursales", sucursales.stream()
                .map(s -> Map.of("id", s.id(), "nombre", s.nombre(), "ciudad", s.alias() == null ? "" : s.alias()))
                .toList());
        cuerpo.put("productos", productos.stream()
                .map(p -> Map.of("id", p.id(), "nombre", p.nombre(), "codigo", p.alias() == null ? "" : p.alias()))
                .toList());
        cuerpo.put("metricas", Arrays.stream(MetricaVoz.values())
                .map(m -> Map.of(
                        "codigo", m.name(),
                        "etiqueta", m.etiqueta(),
                        "usaFechas", m.usaFechas(),
                        "admiteProducto", m.admiteProducto(),
                        "admiteCriticos", m.admiteCriticos(),
                        "agrupaciones", m.agrupaciones().stream().map(Enum::name).toList()))
                .toList());
        return cuerpo;
    }

    /**
     * Acepta el objeto directo, envuelto en {@code consulta} o como primer elemento de una
     * lista (según cómo esté configurado el nodo "Respond to Webhook").
     */
    static Optional<ConsultaReporteVoz> convertir(JsonNode nodo) {
        if (nodo != null && nodo.isArray()) {
            nodo = nodo.isEmpty() ? null : nodo.get(0);
        }
        if (nodo != null && nodo.hasNonNull("consulta")) {
            nodo = nodo.get("consulta");
        }
        if (nodo == null || !nodo.isObject()) {
            return Optional.empty();
        }
        MetricaVoz metrica = enumerado(MetricaVoz.class, nodo.path("metrica").asText(null));
        if (metrica == null) {
            return Optional.empty();
        }
        return Optional.of(ConsultaReporteVoz.builder()
                .metrica(metrica)
                .agrupacion(enumerado(AgrupacionVoz.class, nodo.path("agrupacion").asText(null)))
                .fechaInicio(fecha(nodo.path("fechaInicio").asText(null)))
                .fechaFin(fecha(nodo.path("fechaFin").asText(null)))
                .sucursalId(entero(nodo.get("sucursalId")))
                .productoId(entero(nodo.get("productoId")))
                .soloCriticos(nodo.path("soloCriticos").isBoolean() ? nodo.get("soloCriticos").asBoolean() : null)
                .limite(nodo.path("limite").canConvertToInt() && nodo.path("limite").isNumber()
                        ? nodo.get("limite").asInt() : null)
                .build());
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim().length() > 10 ? valor.trim().substring(0, 10) : valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long entero(JsonNode valor) {
        return valor != null && valor.isIntegralNumber() ? valor.asLong() : null;
    }
}
