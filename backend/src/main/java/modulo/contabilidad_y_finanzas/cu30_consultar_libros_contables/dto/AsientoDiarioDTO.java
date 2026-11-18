package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsientoDiarioDTO(
        Long id,
        String numeroAsiento,
        LocalDate fecha,
        String tipoAsiento,
        String glosa,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        String estado,
        List<LineaAsientoDTO> detalles
) {}
