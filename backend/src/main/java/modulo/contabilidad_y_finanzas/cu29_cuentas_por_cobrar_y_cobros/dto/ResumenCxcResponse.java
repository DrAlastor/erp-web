package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

import java.math.BigDecimal;
import java.util.List;

/** Tablero de cartera: totales, antigüedad de lo vencido y clientes con más saldo. */
public record ResumenCxcResponse(
        BigDecimal saldoTotal,
        BigDecimal vencido,
        BigDecimal porVencer7Dias,
        BigDecimal cobradoMes,
        long cuotasConSaldo,
        long cuotasVencidas,
        List<BigDecimal> antiguedad,
        List<ClienteSaldo> topClientes) {

    /** antiguedad: saldos vencidos en tramos 1-30, 31-60, 61-90 y mas de 90 días. */
    public record ClienteSaldo(Long clienteId, String razonSocial, BigDecimal saldo) {}
}
