package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.math.BigDecimal;
import java.util.List;

public record CuentaMayorDTO(
        Long cuentaId,
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        BigDecimal saldoAnterior,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        BigDecimal saldoFinal,
        List<MovimientoMayorDTO> movimientos
) {}
