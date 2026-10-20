package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity;

/** Estados de la nota de despacho (columna ESTADO de la tabla NOTAS_DESPACHO). */
public enum EstadoDespacho {
    /** Registrada, todavía no salió mercadería: no movió stock. */
    PREPARACION,
    /** Confirmada: se descontó el stock y se registró la salida en el Kardex. */
    DESPACHADO,
    /** El cliente recibió la mercadería. */
    ENTREGADO,
    /** Anulada con motivo; si había salido mercadería, se revirtió. */
    ANULADO
}
