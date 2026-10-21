package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tarjetas de resumen de la CU34. La antiguedad agrupa el saldo de las cuotas vencidas por
 * dias de atraso (0-30, 31-60, 61-90, 90+) y siempre trae los cuatro rangos en ese orden.
 */
public record ResumenCxpResponse(
        BigDecimal saldoTotal,
        BigDecimal vencido,
        BigDecimal porVencer7Dias,
        BigDecimal pagadoMes,
        long cantidadPendientes,
        long cantidadVencidas,
        List<RangoAntiguedad> antiguedad,
        List<ProveedorSaldo> topProveedores) {

    public record RangoAntiguedad(String rango, BigDecimal monto, long cantidad) {}

    public record ProveedorSaldo(Long proveedorId, String razonSocial, BigDecimal saldo) {}
}
