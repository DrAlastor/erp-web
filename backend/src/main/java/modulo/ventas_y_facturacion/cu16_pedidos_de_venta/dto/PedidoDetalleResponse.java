package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import java.math.BigDecimal;

public record PedidoDetalleResponse(
        Long id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal) {}
