package modulo.ventas_y_facturacion.pagos.dto;

import java.util.Map;

/**
 * Estado de una sesión de Stripe Checkout consultada directamente a la API de Stripe.
 *
 * @param montoTotal       importe cobrado en la unidad mínima de la moneda (centavos)
 * @param montoReembolsado importe ya devuelto del cargo, en centavos
 */
public record StripeSesionDTO(
        String id,
        String estadoPago,
        long montoTotal,
        long montoReembolsado,
        String moneda,
        String paymentIntentId,
        Map<String, String> metadata) {

    /** Pagada y sin reembolsos: el dinero sigue cobrado. */
    public boolean pagada() {
        return "paid".equalsIgnoreCase(estadoPago) && montoReembolsado == 0;
    }
}
