package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.math.BigDecimal;

/** Sesión de Stripe Checkout creada: el frontend redirige a {@code checkoutUrl}. */
public record CobroStripeResponse(String sessionId, String checkoutUrl, BigDecimal total) {
}
