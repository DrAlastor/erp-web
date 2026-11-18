package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoMayorDTO(
        LocalDate fecha,
        String numeroAsiento,
        String tipoAsiento,
        String glosa,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldoAcumulado
) {}
