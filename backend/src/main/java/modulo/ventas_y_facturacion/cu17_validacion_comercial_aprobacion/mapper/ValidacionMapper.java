package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.mapper;

import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity.PedidoAValidar;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.LineaPedidoResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.ReglaValidacionResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Regla;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Traduce el pedido y los resultados de las reglas a los DTO de la validación comercial. */
@Component
public class ValidacionMapper {

    public PedidoResumenResponse toResumen(PedidoAValidar pedido, String clienteNombre) {
        return PedidoResumenResponse.builder()
                .id(pedido.getId())
                .numeroPedido(pedido.getNumeroPedido())
                .fecha(pedido.getFecha())
                .clienteId(pedido.getClienteId())
                .clienteNombre(clienteNombre)
                .vendedorId(pedido.getVendedorId())
                .sucursalId(pedido.getSucursalId())
                .subtotal(pedido.getSubtotal())
                .descuento(pedido.getDescuento())
                .total(pedido.getTotal())
                .estado(pedido.getEstado())
                .build();
    }

    public ReglaValidacionResponse toRegla(Regla regla) {
        return ReglaValidacionResponse.builder()
                .codigo(regla.codigo())
                .nombre(regla.nombre())
                .cumple(regla.cumple())
                .bloqueante(regla.bloqueante())
                .detalle(regla.detalle())
                .build();
    }

    public LineaPedidoResponse toLinea(
            Integer productoId,
            String productoNombre,
            String productoSku,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal subtotal,
            BigDecimal stockFisico,
            BigDecimal reservado,
            BigDecimal disponible,
            boolean cubierta
    ) {
        return LineaPedidoResponse.builder()
                .productoId(productoId)
                .productoNombre(productoNombre)
                .productoSku(productoSku)
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .descuento(descuento)
                .subtotal(subtotal)
                .stockFisico(stockFisico)
                .reservado(reservado)
                .disponible(disponible)
                .cubierta(cubierta)
                .build();
    }
}
