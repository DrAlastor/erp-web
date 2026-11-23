package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service;

import com.fasterxml.jackson.databind.JsonNode;
import comun.multitenancy.TenantDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CU40 - Cliente HTTP del servicio interno de Machine Learning (ml-service, FastAPI).
 *
 * <p>El servicio no se publica a internet: solo lo llama el backend con la clave compartida
 * {@code X-Clave-Interna} ({@code ML_CLAVE_INTERNA}). Sin clave o sin URL el ML se considera
 * "no configurado" y no se llama. La empresa siempre sale del token del usuario, nunca del cliente.
 */
@Component
public class MlClienteService {

    /** El servicio de ML no esta configurado, no responde o rechazo el pedido. */
    public static class MlNoDisponibleException extends RuntimeException {
        public MlNoDisponibleException(String mensaje) {
            super(mensaje);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MlClienteService.class);

    private final RestClient rest;
    private final String url;
    private final String clave;

    public MlClienteService(
            RestClient.Builder builder,
            @Value("${ml.url:http://localhost:8000}") String url,
            @Value("${ml.clave-interna:}") String clave,
            @Value("${ml.timeout-ms:15000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(timeoutMs, 10_000));
        factory.setReadTimeout(timeoutMs);
        this.rest = builder.requestFactory(factory).build();
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.clave = clave == null ? "" : clave.trim();
    }

    public boolean configurado() {
        return !url.isEmpty() && !clave.isEmpty();
    }

    /** Encola el entrenamiento de la empresa. Devuelve false si ya estaba en cola. */
    public boolean entrenar(TenantDescriptor empresa) {
        JsonNode r = post("/entrenar", empresa(empresa));
        return r != null && r.path("encolado").asBoolean(false);
    }

    /**
     * Pronostico al momento para horizontes mas largos que los precalculados. Nunca lanza:
     * ante cualquier fallo devuelve {@code {"disponible": false, "motivo": ...}}.
     */
    public JsonNode pronostico(TenantDescriptor empresa, String ambito, String claveSerie,
                               String frecuencia, int horizonte) {
        Map<String, Object> cuerpo = empresa(empresa);
        cuerpo.put("ambito", ambito);
        cuerpo.put("clave", claveSerie);
        cuerpo.put("frecuencia", frecuencia);
        cuerpo.put("horizonte", horizonte);
        try {
            JsonNode r = post("/pronostico", cuerpo);
            if (r == null) throw new MlNoDisponibleException("Respuesta vacia");
            return r;
        } catch (MlNoDisponibleException e) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                    .put("disponible", false)
                    .put("motivo", e.getMessage());
        }
    }

    private Map<String, Object> empresa(TenantDescriptor empresa) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idEmpresa", empresa.empresaId());
        cuerpo.put("codigoEmpresa", empresa.codigoEmpresa());
        return cuerpo;
    }

    private JsonNode post(String ruta, Object cuerpo) {
        if (!configurado()) {
            throw new MlNoDisponibleException("El servicio de Machine Learning no esta configurado.");
        }
        try {
            return rest.post()
                    .uri(url + ruta)
                    .header("X-Clave-Interna", clave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.warn("CU40: el servicio de ML respondio HTTP {} en {}", e.getStatusCode().value(), ruta);
            throw new MlNoDisponibleException(e.getStatusCode().value() == 401
                    ? "El servicio de Machine Learning rechazo la clave interna."
                    : "El servicio de Machine Learning respondio con un error (HTTP " + e.getStatusCode().value() + ").");
        } catch (Exception e) {
            log.warn("CU40: no se pudo contactar al servicio de ML en {}: {}", ruta, e.getMessage());
            throw new MlNoDisponibleException("No se pudo contactar al servicio de Machine Learning.");
        }
    }
}
