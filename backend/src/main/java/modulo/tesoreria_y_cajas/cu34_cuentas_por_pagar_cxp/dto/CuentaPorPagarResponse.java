package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Detalle de una cuota por pagar con los datos de su compra y el historial de pagos. */
public record CuentaPorPagarResponse(
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
        int diasVencido,
        LocalDate fechaCompra,
        BigDecimal totalCompra,
        String sucursalNombre,
        List<PagoProveedorResponse> pagos) {}
