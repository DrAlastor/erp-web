package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.mapper;

import modulo.ventas_y_facturacion.compartido.entity.Venta;
import modulo.ventas_y_facturacion.compartido.entity.VentaDetalle;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResumenResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.VentaDevolucionResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCredito;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCreditoDetalle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class NotaCreditoMapper {

    /** @param devueltas cantidad ya devuelta por id de línea de la factura */
    public VentaDevolucionResponse toVentaDevolucion(Venta venta, Map<Long, BigDecimal> devueltas) {
        return new VentaDevolucionResponse(
                venta.getId(),
                venta.getTipoComprobante(),
                venta.getNumeroFactura(),
                venta.esFactura() ? venta.getNumeroAutorizacion() : null,
                venta.getFecha(),
                venta.getNitCliente(),
                venta.getRazonSocialCliente(),
                venta.getSucursal().getNombre(),
                venta.getTipoVenta(),
                venta.getTotal(),
                venta.getDetalles().stream()
                        .map(detalle -> toLinea(detalle, devueltas.getOrDefault(detalle.getId(), BigDecimal.ZERO)))
                        .toList());
    }

    public NotaCreditoResponse toResponse(NotaCredito nota, BigDecimal aplicadoCxc, BigDecimal reembolsado,
                                          String asiento) {
        Venta venta = nota.getVenta();
        return new NotaCreditoResponse(
                nota.getId(),
                nota.getNumeroNota(),
                nota.getFecha(),
                nota.getMotivo(),
                nota.getEstado(),
                nota.getTotal(),
                nota.getObservaciones(),
                venta.getId(),
                venta.getTipoComprobante(),
                venta.getNumeroFactura(),
                venta.getNitCliente(),
                venta.getRazonSocialCliente(),
                venta.getSucursal().getNombre(),
                venta.getTipoVenta(),
                nota.getEmitidoPor(),
                nota.getDetalles().stream().map(this::toLinea).toList(),
                aplicadoCxc,
                reembolsado,
                asiento);
    }

    public NotaCreditoResumenResponse toResumen(NotaCredito nota) {
        return new NotaCreditoResumenResponse(
                nota.getId(),
                nota.getNumeroNota(),
                nota.getFecha(),
                nota.getVenta().getTipoComprobante(),
                nota.getVenta().getNumeroFactura(),
                nota.getVenta().getRazonSocialCliente(),
                nota.getVenta().getNitCliente(),
                nota.getMotivo(),
                nota.getTotal(),
                nota.getEstado());
    }

    private VentaDevolucionResponse.Linea toLinea(VentaDetalle detalle, BigDecimal devuelta) {
        BigDecimal disponible = detalle.getCantidad().subtract(devuelta).max(BigDecimal.ZERO);
        return new VentaDevolucionResponse.Linea(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getCodigoSku(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                devuelta,
                disponible,
                detalle.getSubtotal().divide(detalle.getCantidad(), 2, RoundingMode.HALF_UP));
    }

    private NotaCreditoResponse.Linea toLinea(NotaCreditoDetalle detalle) {
        return new NotaCreditoResponse.Linea(
                detalle.getId(),
                detalle.getVentaDetalleId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getCodigoSku(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }
}
