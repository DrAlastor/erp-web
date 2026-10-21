package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila del listado de cuentas por pagar. {@code vencida} y {@code diasVencido} se derivan
 * de saldo y vencimiento; no estan guardados.
 */
public record CuentaPorPagarResumenResponse(
        Long id,
        Long compraId,
        String numeroFactura,
        Long proveedorId,
        String proveedorRazonSocial,
        String proveedorNitCi,
        Integer numeroCuota,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldo,
        LocalDate fechaVencimiento,
        String estado,
        boolean vencida,
        int diasVencido) {}
