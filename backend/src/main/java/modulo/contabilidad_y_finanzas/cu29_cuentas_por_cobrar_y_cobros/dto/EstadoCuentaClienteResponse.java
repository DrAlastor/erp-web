package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

import java.math.BigDecimal;
import java.util.List;

/** Estado de cuenta de un cliente: todas sus cuotas y lo que debe hoy. */
public record EstadoCuentaClienteResponse(
        Long clienteId,
        String razonSocial,
        String nitCi,
        BigDecimal limiteCredito,
        BigDecimal totalFacturadoCredito,
        BigDecimal totalCobrado,
        BigDecimal saldo,
        BigDecimal vencido,
        List<CuentaPorCobrarResumenResponse> cuotas) {}
