package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Compra a credito, no anulada, que todavia no tiene cuotas por pagar. */
public record CompraSinCuentaResponse(
        Long compraId,
        String numeroFactura,
        String proveedorRazonSocial,
        LocalDate fecha,
        BigDecimal total,
        Integer diasCredito) {}
