package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inicio del cobro con tarjeta en Stripe Checkout para una venta al contado que todavía
 * no se factura. Las URLs de retorno deben pertenecer a un origen permitido del frontend.
 */
public record CobroStripeRequest(
        @NotNull(message = "Los datos de la venta son obligatorios")
        @Valid
        VentaRequest venta,
        @NotBlank(message = "La URL de retorno es obligatoria")
        @Size(max = 500)
        String successUrl,
        @NotBlank(message = "La URL de cancelación es obligatoria")
        @Size(max = 500)
        String cancelUrl) {
}
