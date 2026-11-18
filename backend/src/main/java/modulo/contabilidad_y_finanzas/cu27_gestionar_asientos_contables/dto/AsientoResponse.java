package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Asiento completo con sus líneas. {@code editable} y {@code anulable} le dicen al
 * frontend qué botones mostrar; el backend vuelve a validarlo en cada operación.
 */
public record AsientoResponse(
        Long id,
        String numero,
        LocalDate fecha,
        String tipo,
        String glosa,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        String estado,
        boolean manual,
        boolean editable,
        boolean anulable,
        String periodo,
        String estadoPeriodo,
        String creadoPor,
        LocalDateTime fechaCreacion,
        List<Linea> lineas) {

    public record Linea(
            Long id,
            Long cuentaId,
            String cuentaCodigo,
            String cuentaNombre,
            String glosa,
            BigDecimal debe,
            BigDecimal haber) {}
}
