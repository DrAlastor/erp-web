package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.time.LocalDate;

public record PeriodoContableSimpleDTO(
        Long id,
        Integer gestion,
        Integer mes,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {}
