package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NotaCreditoResumenResponse(
        Long id,
        String numeroNota,
        LocalDate fecha,
        String tipoComprobante,
        Integer numeroFactura,
        String razonSocialCliente,
        String nitCliente,
        String motivo,
        BigDecimal total,
        String estado) {
}
