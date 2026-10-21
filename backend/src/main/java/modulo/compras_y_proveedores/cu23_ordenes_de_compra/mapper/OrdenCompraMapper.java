package modulo.compras_y_proveedores.cu23_ordenes_de_compra.mapper;

import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OpcionesOrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResumenResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.SugerenciaReposicionResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompraDetalle;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OrdenCompraMapper {

    public OrdenCompraResumenResponse aResumen(OrdenCompra o) {
        return new OrdenCompraResumenResponse(
                o.getId(),
                o.getNumeroOrden(),
                o.getFecha(),
                o.getFechaEntrega(),
                o.getProveedor().getId(),
                o.getProveedor().getRazonSocial(),
                o.getTotal(),
                o.getEstado().name(),
                o.getCreadoPor());
    }

    public OrdenCompraResponse aRespuesta(OrdenCompra o, String creadoPor, Long usuarioActual) {
        return new OrdenCompraResponse(
                o.getId(),
                o.getNumeroOrden(),
                o.getFecha(),
                o.getFechaEntrega(),
                o.getProveedor().getId(),
                o.getProveedor().getRazonSocial(),
                o.getProveedor().getNitCi(),
                o.getProveedor().getCondicionPago() == null ? null : o.getProveedor().getCondicionPago().name(),
                o.getProveedor().getDiasCredito(),
                o.getCreadoPor(),
                creadoPor,
                o.getSubtotal(),
                o.getDescuento(),
                o.getTotal(),
                o.getEstado().name(),
                o.getObservaciones(),
                o.getFechaCreacion(),
                o.getCreadoPor() != null && o.getCreadoPor().equals(usuarioActual),
                o.getDetalles().stream().map(this::aDetalle).toList());
    }

    private OrdenCompraResponse.Detalle aDetalle(OrdenCompraDetalle d) {
        return new OrdenCompraResponse.Detalle(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getCodigoSku(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getCostoUnitario(),
                d.getSubtotal());
    }

    /** Fila nativa (id, código, nombre, disponible, mínimo, último costo). */
    public SugerenciaReposicionResponse aSugerencia(Object[] fila) {
        BigDecimal disponible = decimal(fila[3]);
        BigDecimal minimo = decimal(fila[4]);
        return new SugerenciaReposicionResponse(
                ((Number) fila[0]).longValue(),
                (String) fila[1],
                (String) fila[2],
                disponible,
                minimo,
                minimo.subtract(disponible).max(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP),
                decimal(fila[5]));
    }

    /** Fila nativa (id, código, nombre, último costo). */
    public OpcionesOrdenCompraResponse.ProductoComprable aProducto(Object[] fila) {
        return new OpcionesOrdenCompraResponse.ProductoComprable(
                ((Number) fila[0]).longValue(),
                (String) fila[1],
                (String) fila[2],
                decimal(fila[3]));
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
