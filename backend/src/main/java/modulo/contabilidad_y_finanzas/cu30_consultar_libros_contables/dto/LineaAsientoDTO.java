package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.math.BigDecimal;

public record LineaAsientoDTO(
        Long id,
        Long cuentaId,
        String cuentaCodigo,
        String cuentaNombre,
        String glosa,
        BigDecimal debe,
        BigDecimal haber
) {}
