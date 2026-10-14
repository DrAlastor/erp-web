package modulo.ventas_y_facturacion.compartido.entity;

import java.util.Set;

/**
 * Estados del pedido de venta (columna ESTADO de la tabla PEDIDOS).
 *
 * <p>El pedido nace PENDIENTE cuando se convierte una cotización (CU15), lo aprueba o
 * rechaza la validación comercial (CU17) y avanza con los despachos (CU18).
 */
public enum EstadoPedido {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    DESPACHO_PARCIAL,
    DESPACHADO,
    /**
     * Pedido cerrado (despachado y facturado). Lo usan los datos semilla de ERP_Asus; si no
     * estuviera acá, leer esos pedidos rompería con un 500.
     */
    COMPLETADO,
    ANULADO;

    /** Estados desde los que el almacén puede despachar mercadería. */
    public static final Set<EstadoPedido> DESPACHABLES = Set.of(APROBADO, DESPACHO_PARCIAL);
}
