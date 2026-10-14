package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity;

import java.util.Set;

/** Estados de la cotización (columna ESTADO de la tabla COTIZACIONES). */
public enum EstadoCotizacion {
    /** Se está armando; todavía no se entregó al cliente. */
    BORRADOR,
    /** Se generó la proforma y corre el plazo de validez. */
    EMITIDA,
    /** El cliente confirmó que quiere comprar. */
    ACEPTADA,
    /** Ya se generó el pedido de venta a partir de ella. */
    CONVERTIDA,
    /** Pasó el plazo de validez sin convertirse en pedido. */
    VENCIDA,
    /** Se anuló con un motivo registrado. */
    ANULADA;

    /** Estados en los que todavía se puede corregir el contenido. */
    public static final Set<EstadoCotizacion> MODIFICABLES = Set.of(BORRADOR, EMITIDA);

    /** Estados en los que corre el plazo de validez y se puede convertir en pedido. */
    public static final Set<EstadoCotizacion> VIGENTES = Set.of(EMITIDA, ACEPTADA);
}
