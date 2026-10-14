package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResumenResponse(
        Long id,
        String tipoComprobante,
        Integer numeroFactura,
        LocalDateTime fecha,
        String razonSocialCliente,
        String nitCliente,
        String sucursalNombre,
        String tipoVenta,
        BigDecimal total,
        String estado) {
}
