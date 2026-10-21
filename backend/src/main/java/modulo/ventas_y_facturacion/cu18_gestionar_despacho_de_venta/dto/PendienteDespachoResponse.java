package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.math.BigDecimal;

/**
 * Situación de un producto del pedido.
 *
 * @param comprometido    lo que ya figura en notas no anuladas (en preparación o despachadas)
 * @param pendiente       lo que todavía se puede despachar
 * @param stockDisponible existencias en la sucursal del pedido
 */
public record PendienteDespachoResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        BigDecimal pedido,
        BigDecimal comprometido,
        BigDecimal pendiente,
        BigDecimal stockDisponible) {
}
