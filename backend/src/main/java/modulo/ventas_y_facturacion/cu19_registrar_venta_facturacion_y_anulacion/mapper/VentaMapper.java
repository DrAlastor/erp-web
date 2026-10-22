package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.mapper;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import modulo.ventas_y_facturacion.compartido.entity.CuentaPorCobrar;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.PagoVenta;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.entity.Venta;
import modulo.ventas_y_facturacion.compartido.entity.VentaDetalle;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CatalogosVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ClienteVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.DosificacionResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ProductoVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResumenResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.Descuento;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.SecuenciaFacturacion;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class VentaMapper {

    public VentaResponse toResponse(Venta venta, List<CuentaPorCobrar> cuentas, String leyendaSin, String asiento) {
        boolean factura = venta.esFactura();
        return new VentaResponse(
                venta.getId(),
                venta.getTipoComprobante(),
                venta.getNumeroFactura(),
                factura ? venta.getNumeroAutorizacion() : null,
                factura ? venta.getCodigoControl() : null,
                factura ? leyendaSin : null,
                venta.getFecha(),
                venta.getEstado(),
                venta.getTipoVenta(),
                venta.getSucursal().getId(),
                venta.getSucursal().getNombre(),
                venta.getCliente().getId(),
                venta.getNitCliente(),
                venta.getRazonSocialCliente(),
                venta.getCajeroId(),
                venta.getTurnoCajaId(),
                venta.getSubtotal(),
                venta.getDescuento(),
                venta.getIva(),
                venta.getIt(),
                venta.getTotal(),
                venta.getDetalles().stream().map(this::toLinea).toList(),
                venta.getPagos().stream().map(this::toPago).toList(),
                cuentas.stream().map(this::toCuota).toList(),
                venta.getMotivoAnulacion(),
                venta.getFechaAnulacion(),
                venta.getAnuladoPor(),
                asiento);
    }

    public VentaResumenResponse toResumen(Venta venta) {
        return new VentaResumenResponse(
                venta.getId(),
                venta.getTipoComprobante(),
                venta.getNumeroFactura(),
                venta.getFecha(),
                venta.getRazonSocialCliente(),
                venta.getNitCliente(),
                venta.getSucursal().getNombre(),
                venta.getTipoVenta(),
                venta.getTotal(),
                venta.getEstado());
    }

    public ProductoVentaResponse toProducto(StockAlmacen stock) {
        return new ProductoVentaResponse(
                stock.getProducto().getId(),
                stock.getProducto().getCodigoSku(),
                stock.getProducto().getNombre(),
                stock.getProducto().getPrecioVenta(),
                stock.getCantidadActual());
    }

    public ClienteVentaResponse toCliente(Cliente cliente) {
        return new ClienteVentaResponse(cliente.getId(), cliente.getRazonSocial(), cliente.getNitCi());
    }

    public CatalogosVentaResponse.Sucursal toSucursal(Almacen sucursal) {
        return new CatalogosVentaResponse.Sucursal(sucursal.getId(), sucursal.getNombre());
    }

    public CatalogosVentaResponse.FormaPago toFormaPago(FormaPago forma) {
        return new CatalogosVentaResponse.FormaPago(forma.getId(), forma.getCodigo(), forma.getNombre());
    }

    public CatalogosVentaResponse.Descuento toDescuento(Descuento descuento) {
        return new CatalogosVentaResponse.Descuento(
                descuento.getId(), descuento.getNombre(), descuento.getTipo(), descuento.getValor());
    }

    public CatalogosVentaResponse.Turno toTurno(TurnoCaja turno) {
        return new CatalogosVentaResponse.Turno(
                turno.getId(), turno.getSucursalId(), turno.getCajeroId(), turno.getFechaApertura(), turno.getNombre());
    }

    public CatalogosVentaResponse.Dosificacion toDosificacion(SecuenciaFacturacion secuencia) {
        return new CatalogosVentaResponse.Dosificacion(
                secuencia.getNumeroAutorizacion(),
                secuencia.getNumeroActual() + 1,
                secuencia.getNumeroFinal(),
                secuencia.getFechaLimite());
    }

    public DosificacionResponse toDosificacionResponse(SecuenciaFacturacion secuencia, LocalDate hoy) {
        int disponibles = Math.max(0, secuencia.getNumeroFinal() - secuencia.getNumeroActual());
        String estado;
        if (disponibles == 0) estado = "AGOTADA";
        else if (secuencia.getFechaLimite().isBefore(hoy)) estado = "VENCIDA";
        else if (!Boolean.TRUE.equals(secuencia.getActivo())) estado = "INACTIVA";
        else estado = "VIGENTE";
        return new DosificacionResponse(
                secuencia.getId(),
                secuencia.getNumeroAutorizacion(),
                secuencia.getLeyendaSin(),
                secuencia.getNumeroInicial(),
                secuencia.getNumeroFinal(),
                secuencia.getNumeroActual() >= secuencia.getNumeroInicial() ? secuencia.getNumeroActual() : null,
                disponibles,
                secuencia.getFechaLimite(),
                secuencia.getActivo(),
                estado);
    }

    private VentaResponse.Linea toLinea(VentaDetalle detalle) {
        return new VentaResponse.Linea(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getCodigoSku(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getDescuento(),
                detalle.getSubtotal());
    }

    private VentaResponse.Pago toPago(PagoVenta pago) {
        return new VentaResponse.Pago(
                pago.getId(),
                pago.getFormaPago().getId(),
                pago.getFormaPago().getNombre(),
                pago.getImporte(),
                pago.getTransaccionExterna(),
                pago.getEstado());
    }

    private VentaResponse.Cuota toCuota(CuentaPorCobrar cuenta) {
        return new VentaResponse.Cuota(
                cuenta.getId(),
                cuenta.getNumeroCuota(),
                cuenta.getMontoTotal(),
                cuenta.getMontoCobrado(),
                cuenta.getSaldo(),
                cuenta.getFechaVencimiento(),
                cuenta.getEstado());
    }
}
