package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.mapper;

import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResumenResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.DetalleCotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.PedidoGeneradoResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.Cotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.CotizacionDetalle;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.EstadoCotizacion;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CotizacionMapper {

    public CotizacionResumenResponse aResumen(Cotizacion c) {
        return new CotizacionResumenResponse(
                c.getId(),
                c.getNumeroCotizacion(),
                c.getFecha(),
                c.fechaVencimiento(),
                c.getCliente().getId(),
                c.getCliente().getRazonSocial(),
                c.getTotal(),
                c.getEstado().name());
    }

    public CotizacionResponse aRespuesta(Cotizacion c, String vendedor, LocalDate hoy) {
        return new CotizacionResponse(
                c.getId(),
                c.getNumeroCotizacion(),
                c.getFecha(),
                c.getValidezDias(),
                c.fechaVencimiento(),
                c.getCliente().getId(),
                c.getCliente().getRazonSocial(),
                c.getCliente().getNitCi(),
                c.getVendedorId(),
                vendedor,
                c.getSubtotal(),
                c.getDescuento(),
                c.descuentoGlobal(),
                c.getTotal(),
                c.getEstado().name(),
                c.getObservaciones(),
                EstadoCotizacion.MODIFICABLES.contains(c.getEstado()),
                c.estaVigente(hoy),
                c.getDetalles().stream().map(this::aDetalle).toList());
    }

    public PedidoGeneradoResponse aPedidoGenerado(Pedido p, Cotizacion c) {
        return new PedidoGeneradoResponse(
                p.getId(),
                p.getNumeroPedido(),
                p.getEstado().name(),
                p.getTotal(),
                c.getId(),
                c.getNumeroCotizacion());
    }

    private DetalleCotizacionResponse aDetalle(CotizacionDetalle d) {
        return new DetalleCotizacionResponse(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getCodigoSku(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getDescuento(),
                d.getSubtotal());
    }
}
