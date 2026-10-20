package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import java.math.BigDecimal;

public record DetalleCotizacionResponse(
        Integer id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal) {
}
