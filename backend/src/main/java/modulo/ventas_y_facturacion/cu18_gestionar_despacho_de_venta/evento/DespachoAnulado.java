package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento;

import java.math.BigDecimal;

/**
 * Se publica cuando se anula un despacho que ya había salido del almacén: el stock vuelve
 * y la contabilidad debe revertir el costo de venta que registró.
 */
public record DespachoAnulado(
        Integer despachoId,
        String numeroDespacho,
        Integer pedidoId,
        BigDecimal costoTotal,
        String motivo) {
}
