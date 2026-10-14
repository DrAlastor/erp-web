package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.time.LocalDate;

/**
 * Dosificación del SIN. {@code estado} se calcula: VIGENTE, INACTIVA, VENCIDA o AGOTADA.
 * {@code ultimoNumero} es la última factura emitida con ella.
 */
public record DosificacionResponse(
        Long id,
        String numeroAutorizacion,
        String leyendaSin,
        Integer numeroInicial,
        Integer numeroFinal,
        Integer ultimoNumero,
        Integer disponibles,
        LocalDate fechaLimite,
        Boolean activo,
        String estado) {
}
