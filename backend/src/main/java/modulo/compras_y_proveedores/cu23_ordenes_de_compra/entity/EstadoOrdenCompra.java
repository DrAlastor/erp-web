package modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity;

import java.util.Set;

/** Estados de la orden de compra (columna ESTADO de la tabla ORDENES_COMPRA). */
public enum EstadoOrdenCompra {
    /** Se está armando, o el Administrador la rechazó para que se corrija. */
    BORRADOR,
    /** Registrada y esperando que el Administrador la apruebe o la rechace. */
    PENDIENTE,
    /** Aprobada: ya se puede imprimir y enviar al proveedor. */
    APROBADA,
    /** Se envió al proveedor. */
    ENVIADA,
    /** La compra que la cumple ya se registró (CU23/CU24). */
    RECIBIDA,
    /** Se anuló con un motivo registrado. */
    ANULADA;

    /** Estados en los que todavía se puede corregir el contenido. */
    public static final Set<EstadoOrdenCompra> MODIFICABLES = Set.of(BORRADOR, PENDIENTE);

    /** Estados que comprometen al proveedor (impiden desactivarlo). */
    public static final Set<EstadoOrdenCompra> ABIERTAS = Set.of(PENDIENTE, APROBADA, ENVIADA);
}
