package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import java.math.BigDecimal;

/** Pedido de venta que se creó al convertir una cotización. */
public record PedidoGeneradoResponse(
        Integer pedidoId,
        String numeroPedido,
        String estado,
        BigDecimal total,
        Integer cotizacionId,
        String numeroCotizacion) {
}
