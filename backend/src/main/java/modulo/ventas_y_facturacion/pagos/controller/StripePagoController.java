package modulo.ventas_y_facturacion.pagos.controller;

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

    public StripePagoController(StripePaymentService stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
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
        log.info("Webhook recibido de Stripe. Longitud payload: {}", payload.length());
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
