package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Arqueo del turno: desglose del efectivo esperado y montos por forma de pago.
 * {@code montoContado} y {@code diferencia} son null mientras el turno está abierto.
 */
public record ArqueoResponse(
        Long turnoId,
        BigDecimal montoApertura,
        BigDecimal ventasEfectivo,
        BigDecimal ingresosEfectivo,
        BigDecimal egresosEfectivo,
        BigDecimal efectivoEsperado,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        long cantidadMovimientos,
        List<FormaPagoArqueo> porFormaPago,
        BigDecimal montoContado,
        BigDecimal diferencia) {

    public record FormaPagoArqueo(
            Long formaPagoId, String nombre, BigDecimal ingresos, BigDecimal egresos, BigDecimal ventas) {}
}
