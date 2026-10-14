package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.mapper;

import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoDetalleResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoVenta;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoVentaDetalle;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.PedidoCabecera;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.ProductoRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Traduce cabeceras y lineas del pedido a los DTOs de la API. Los nombres de cliente,
 * sucursal, vendedor y producto llegan ya resueltos desde el repositorio de consultas.
 */
@Component
public class PedidoVentaMapper {

    public PedidoResumenResponse toResumen(PedidoCabecera c) {
        return new PedidoResumenResponse(
                c.id(),
                c.numeroPedido(),
                c.fecha(),
                c.clienteId(),
                c.clienteRazonSocial(),
                c.clienteNitCi(),
                c.sucursalId(),
                c.sucursalNombre(),
                c.vendedorId(),
                c.vendedorNombre(),
                c.subtotal(),
                c.descuento(),
                c.total(),
                c.estado(),
                c.cantidadItems());
    }

    public PedidoResponse toResponse(
            PedidoCabecera c, PedidoVenta pedido, Map<Long, ProductoRef> productos) {
        List<PedidoDetalleResponse> detalles = pedido.getDetalles().stream()
                .map(d -> toDetalle(d, productos.get(d.getProductoId())))
                .toList();
        return new PedidoResponse(
                c.id(),
                c.numeroPedido(),
                c.fecha(),
                c.clienteId(),
                c.clienteRazonSocial(),
                c.clienteNitCi(),
                c.sucursalId(),
                c.sucursalNombre(),
                c.vendedorId(),
                c.vendedorNombre(),
                c.subtotal(),
                c.descuento(),
                c.total(),
                c.estado(),
                detalles.size(),
                c.observaciones(),
                c.fechaCreacion(),
                detalles);
    }

    private PedidoDetalleResponse toDetalle(PedidoVentaDetalle d, ProductoRef producto) {
        return new PedidoDetalleResponse(
                d.getId(),
                d.getProductoId(),
                producto == null ? null : producto.codigo(),
                producto == null ? null : producto.nombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getDescuento(),
                d.getSubtotal());
    }
}
