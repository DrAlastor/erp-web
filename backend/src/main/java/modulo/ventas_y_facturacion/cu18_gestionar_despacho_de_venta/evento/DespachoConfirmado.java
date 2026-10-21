package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento;

import java.math.BigDecimal;

/**
 * Se publica cuando un despacho se confirma y la mercadería sale del almacén.
 *
 * <p>Es el punto de enganche para el asiento de costo de venta (Costo de Mercadería
 * Vendida contra Inventario): cuando exista la CU26 de asientos contables, basta con un
 * {@code @TransactionalEventListener} que lo escuche, sin tocar el despacho.
 *
 * @param costoTotal suma de cantidad × costo unitario de lo despachado
 */
public record DespachoConfirmado(
        Integer despachoId,
        String numeroDespacho,
        Integer pedidoId,
        Integer sucursalId,
        BigDecimal costoTotal) {
}
