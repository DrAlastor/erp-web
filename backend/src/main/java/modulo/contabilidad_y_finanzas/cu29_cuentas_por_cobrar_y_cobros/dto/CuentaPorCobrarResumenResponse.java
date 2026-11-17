package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del listado de cuotas por cobrar, con la venta y el cliente resueltos. */
public record CuentaPorCobrarResumenResponse(
        Long id,
        Long ventaId,
        String documento,
        Long clienteId,
        String clienteRazonSocial,
        String clienteNitCi,
        Integer numeroCuota,
        BigDecimal montoTotal,
        BigDecimal montoCobrado,
        BigDecimal saldo,
        LocalDate fechaVencimiento,
        String estado,
        boolean vencida,
        long diasVencida,
        String sucursal) {}
