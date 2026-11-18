package modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PeriodoResponse(
        Long id,
        Integer gestion,
        Integer mes,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado,
        LocalDateTime fechaCierre,
        long cantidadAsientos) {
}
