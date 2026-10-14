package modulo.ventas_y_facturacion.pagos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutRequestDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutResponseDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeSesionDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Servicio de integración con la API REST oficial de Stripe.
 * Utiliza comunicación HTTP REST nativa de Spring Boot para máxima compatibilidad
 * tanto en el despliegue en la nube (AWS) como en desarrollo local, sin dependencias
 * externas que causen errores en el Language Server del IDE.
 */
@Service
public class StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    private static final String STRIPE_API_CHECKOUT_URL = "https://api.stripe.com/v1/checkout/sessions";
    private static final String STRIPE_API_REFUNDS_URL = "https://api.stripe.com/v1/refunds";

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.public-key:}")
    private String publicKey;

    @Value("${app.frontend-url:https://erp-contable-comercial.online}")
    private String defaultFrontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            log.info("Stripe REST Service inicializado correctamente con clave de prueba");
        } else {
            log.warn("Clave secreta de Stripe no configurada.");
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    /** Indica si hay una clave secreta configurada para hablar con Stripe. */
    public boolean estaConfigurado() {
        return secretKey != null && !secretKey.isBlank();
    }

    /**
     * Crea una sesión segura en Stripe Checkout vía API REST oficial.
     * Funciona tanto para el despliegue en la nube (CloudFront) como en localhost.
     */
    public StripeCheckoutResponseDTO crearSesionCheckout(StripeCheckoutRequestDTO request) {
        try {
            // Conversión de monto a centavos (ej: 10.00 USD -> 1000 centavos)
            long unitAmountCents = request.getMonto()
                    .multiply(new BigDecimal("100"))
                    .longValue();

            String moneda = (request.getMoneda() != null && !request.getMoneda().isBlank())
                    ? request.getMoneda().toLowerCase()
                    : "usd";

            String baseUrl = (defaultFrontendUrl != null && !defaultFrontendUrl.isBlank())
                    ? defaultFrontendUrl.replaceAll("/+$", "")
                    : "https://erp-contable-comercial.online";

            String successUrl = (request.getSuccessUrl() != null && !request.getSuccessUrl().isBlank())
                    ? request.getSuccessUrl()
                    : baseUrl + "/app?pago=exito&session_id={CHECKOUT_SESSION_ID}";

            String cancelUrl = (request.getCancelUrl() != null && !request.getCancelUrl().isBlank())
                    ? request.getCancelUrl()
                    : baseUrl + "/app?pago=cancelado";

            // Headers con autenticación Bearer oficial de Stripe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(secretKey);

            // Parámetros form-urlencoded requeridos por Stripe Checkout API
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("mode", "payment");
            body.add("success_url", successUrl);
            body.add("cancel_url", cancelUrl);

            body.add("line_items[0][price_data][currency]", moneda);
            body.add("line_items[0][price_data][unit_amount]", String.valueOf(unitAmountCents));
            body.add("line_items[0][price_data][product_data][name]", request.getConcepto());
            if (request.getDescripcion() != null && !request.getDescripcion().isBlank()) {
                body.add("line_items[0][price_data][product_data][description]", request.getDescripcion());
            }
            body.add("line_items[0][quantity]", "1");

            if (request.getClienteEmail() != null && !request.getClienteEmail().isBlank()) {
                body.add("customer_email", request.getClienteEmail());
            }

            if (request.getReferenciaId() != null && !request.getReferenciaId().isBlank()) {
                body.add("metadata[referenciaId]", request.getReferenciaId());
            }

            if (request.getMetadata() != null) {
                for (Map.Entry<String, String> entry : request.getMetadata().entrySet()) {
                    body.add("metadata[" + entry.getKey() + "]", entry.getValue());
                }
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    STRIPE_API_CHECKOUT_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String sessionId = root.path("id").asText();
            String checkoutUrl = root.path("url").asText();
            String paymentStatus = root.path("payment_status").asText("unpaid");

            log.info("Sesión Stripe Checkout creada con éxito: id={}, url={}", sessionId, checkoutUrl);

            return StripeCheckoutResponseDTO.builder()
                    .sessionId(sessionId)
                    .checkoutUrl(checkoutUrl)
                    .estado(paymentStatus)
                    .referenciaId(request.getReferenciaId())
                    .publicKey(publicKey)
                    .build();

        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP al comunicarse con Stripe API (Status: {}): {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Error en Stripe API: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Error general procesando sesión Stripe: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al procesar pago con Stripe: " + ex.getMessage(), ex);
        }
    }

    /**
     * Consulta una sesión de Checkout en Stripe. Es la fuente de verdad para saber si un
     * pago se completó: nunca se confía en lo que diga el navegador.
     */
    public StripeSesionDTO consultarSesion(String sessionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            // Se expande el cargo para saber si el pago ya fue reembolsado.
            ResponseEntity<String> response = restTemplate.exchange(
                    STRIPE_API_CHECKOUT_URL + "/{id}?expand[]={expand}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class,
                    sessionId,
                    "payment_intent.latest_charge");

            JsonNode root = objectMapper.readTree(response.getBody());
            Map<String, String> metadata = new java.util.HashMap<>();
            root.path("metadata").fields().forEachRemaining(e -> metadata.put(e.getKey(), e.getValue().asText()));
            JsonNode intent = root.path("payment_intent");
            return new StripeSesionDTO(
                    root.path("id").asText(),
                    root.path("payment_status").asText(),
                    root.path("amount_total").asLong(),
                    intent.path("latest_charge").path("amount_refunded").asLong(0),
                    root.path("currency").asText(),
                    intent.isObject() ? intent.path("id").asText(null) : null,
                    metadata);
        } catch (HttpStatusCodeException ex) {
            log.error("Stripe rechazó la consulta de la sesión {} (Status: {}): {}",
                    sessionId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("La sesión de pago " + sessionId + " no existe en Stripe", ex);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo consultar la sesión de pago en Stripe: " + ex.getMessage(), ex);
        }
    }

    /** Reembolsa por completo el cobro asociado a un PaymentIntent. */
    public void reembolsar(String paymentIntentId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(secretKey);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("payment_intent", paymentIntentId);
            restTemplate.exchange(STRIPE_API_REFUNDS_URL, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            log.info("Reembolso Stripe registrado para el PaymentIntent {}", paymentIntentId);
        } catch (HttpStatusCodeException ex) {
            log.error("Stripe rechazó el reembolso de {} (Status: {}): {}",
                    paymentIntentId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Stripe rechazó el reembolso del pago " + paymentIntentId, ex);
        }
    }
}
