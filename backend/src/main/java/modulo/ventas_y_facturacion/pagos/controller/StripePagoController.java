package modulo.ventas_y_facturacion.pagos.controller;

import com.fasterxml.jackson.databind.JsonNode;
import comun.saas.SaasOnboardingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutRequestDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutResponseDTO;
import modulo.ventas_y_facturacion.pagos.service.StripePaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class StripePagoController {

    private static final Logger log = LoggerFactory.getLogger(StripePagoController.class);

    private final StripePaymentService stripePaymentService;
    private final SaasOnboardingService onboardingService;

    public StripePagoController(
            StripePaymentService stripePaymentService,
            SaasOnboardingService onboardingService) {
        this.stripePaymentService = stripePaymentService;
        this.onboardingService = onboardingService;
    }

    /**
     * Devuelve la clave pública de Stripe para que el frontend pueda inicializar Stripe.js si es necesario.
     */
    @GetMapping("/stripe/config")
    public ResponseEntity<Map<String, String>> obtenerConfiguracion() {
        return ResponseEntity.ok(Map.of(
                "publicKey", stripePaymentService.getPublicKey()
        ));
    }

    /**
     * Inicia una sesión de Stripe Checkout y devuelve la URL para redirigir al usuario.
     */
    @PostMapping("/stripe/checkout")
    public ResponseEntity<StripeCheckoutResponseDTO> crearSesionCheckout(
            @Valid @RequestBody StripeCheckoutRequestDTO request) {
        log.info("Iniciando sesión de pago Stripe para concepto: '{}', monto: {} {}",
                request.getConcepto(), request.getMonto(), request.getMoneda());
        StripeCheckoutResponseDTO response = stripePaymentService.crearSesionCheckout(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook de Stripe para notificaciones asíncronas de pagos completados.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<Map<String, String>> webhookStripe(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        JsonNode event = stripePaymentService.verifyWebhook(payload, sigHeader);
        String eventType = event.path("type").asText();
        if ("checkout.session.completed".equals(eventType)) {
            String sessionId = event.path("data").path("object").path("id").asText();
            if (!sessionId.isBlank()) {
                onboardingService.confirmStripeSession(sessionId);
            }
        }
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
