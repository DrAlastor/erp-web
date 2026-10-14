package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.mapper;

import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResumenResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DetalleDespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.DespachoDetalle;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.NotaDespacho;

import org.springframework.stereotype.Component;

@Component
public class DespachoMapper {

    public DespachoResumenResponse aResumen(NotaDespacho n) {
        return new DespachoResumenResponse(
                n.getId(),
                n.getNumeroDespacho(),
                n.getFecha(),
                n.getPedido().getId(),
                n.getPedido().getNumeroPedido(),
                n.getPedido().getCliente().getRazonSocial(),
                n.getSucursal().getNombre(),
                n.getEstado().name());
    }

    public DespachoResponse aRespuesta(NotaDespacho n, String despachadoPor) {
        return new DespachoResponse(
                n.getId(),
                n.getNumeroDespacho(),
                n.getFecha(),
                n.getEstado().name(),
                n.getPedido().getId(),
                n.getPedido().getNumeroPedido(),
                n.getPedido().getEstado().name(),
                n.getPedido().getCliente().getId(),
                n.getPedido().getCliente().getRazonSocial(),
                n.getSucursal().getId(),
                n.getSucursal().getNombre(),
                n.getDespachadoPor(),
                despachadoPor,
                n.getConductor(),
                n.getPlaca(),
                n.getObservaciones(),
                n.getEstado() == EstadoDespacho.PREPARACION,
                n.getEstado() == EstadoDespacho.DESPACHADO,
                n.getEstado() == EstadoDespacho.PREPARACION || n.getEstado() == EstadoDespacho.DESPACHADO,
                n.getDetalles().stream().map(this::aDetalle).toList());
    }

    private DetalleDespachoResponse aDetalle(DespachoDetalle d) {
        return new DetalleDespachoResponse(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getCodigoSku(),
                d.getProducto().getNombre(),
                d.getCantidad());
    }
}
