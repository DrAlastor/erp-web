package modulo.inventario_y_almacenes.movimientos_de_inventario.mapper;

import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.AlertaStockMinimoResponse;
import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.AlmacenSimpleResponse;
import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.MovimientoInventarioResponse;
import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.ProductoSimpleResponse;
import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.StockAlmacenResponse;
import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.Almacen;
import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.KardexMovimiento;
import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.Producto;
import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.StockAlmacen;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Traduce las entidades del inventario a los DTO de la HU-06.
 *
 * <p>Acá viven las derivaciones que el servicio no debería tener que recordar: el saldo
 * valorado del Kardex y la comparación contra el stock mínimo, que marca las alertas de
 * reposición y su nivel de criticidad.
 */
@Component
public class MovimientoInventarioMapper {

    public MovimientoInventarioResponse toMovimientoResponse(KardexMovimiento movimiento) {
        return MovimientoInventarioResponse.builder()
                .id(movimiento.getId())
                .productoId(movimiento.getProducto().getId())
                .productoNombre(movimiento.getProducto().getNombre())
                .productoSku(movimiento.getProducto().getCodigoSku())
                .unidadMedida(movimiento.getProducto().getUnidadMedida())
                .almacenId(movimiento.getAlmacen().getId())
                .almacenNombre(movimiento.getAlmacen().getNombre())
                .tipoMovimiento(movimiento.getTipoMovimiento())
                .cantidad(movimiento.getCantidad())
                .costoUnitario(movimiento.getCostoUnitario())
                .saldoCantidad(movimiento.getSaldoCantidad())
                .saldoValorado(movimiento.getSaldoValorado())
                .referenciaDoc(movimiento.getReferenciaDoc())
                .motivo(movimiento.getMotivo())
                .fecha(movimiento.getFecha())
                .creadoPor(movimiento.getCreadoPor())
                .build();
    }

    /** Existencia por almacén con la marca de stock bajo mínimo. */
    public StockAlmacenResponse toStockResponse(StockAlmacen stock) {
        BigDecimal actual = cantidadDe(stock);
        int minimo = stockMinimoDe(stock.getProducto());

        return StockAlmacenResponse.builder()
                .id(stock.getId())
                .productoId(stock.getProducto().getId())
                .productoNombre(stock.getProducto().getNombre())
                .productoSku(stock.getProducto().getCodigoSku())
                .codigoBarra(stock.getProducto().getCodigoBarra())
                .unidadMedida(stock.getProducto().getUnidadMedida())
                .precioVenta(stock.getProducto().getPrecioVenta())
                .costoPromedio(stock.getProducto().getCostoPromedio())
                .stockMinimo(minimo)
                .almacenId(stock.getAlmacen().getId())
                .almacenNombre(stock.getAlmacen().getNombre())
                .cantidadActual(actual)
                .bajoStockMinimo(actual.compareTo(BigDecimal.valueOf(minimo)) <= 0)
                .build();
    }

    /**
     * Alerta de reposición: el déficit contra el mínimo y su criticidad. Se considera
     * agotado si no queda existencia, crítico si queda la mitad del mínimo o menos, y alerta
     * en el resto de los casos.
     */
    public AlertaStockMinimoResponse toAlertaResponse(StockAlmacen stock) {
        BigDecimal actual = cantidadDe(stock);
        int minimo = stockMinimoDe(stock.getProducto());
        BigDecimal minDecimal = BigDecimal.valueOf(minimo);

        String criticidad;
        if (actual.compareTo(BigDecimal.ZERO) <= 0) {
            criticidad = "AGOTADO";
        } else if (actual.compareTo(minDecimal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) <= 0) {
            criticidad = "CRITICO";
        } else {
            criticidad = "ALERTA";
        }

        return AlertaStockMinimoResponse.builder()
                .productoId(stock.getProducto().getId())
                .productoNombre(stock.getProducto().getNombre())
                .productoSku(stock.getProducto().getCodigoSku())
                .unidadMedida(stock.getProducto().getUnidadMedida())
                .almacenId(stock.getAlmacen().getId())
                .almacenNombre(stock.getAlmacen().getNombre())
                .cantidadActual(actual)
                .stockMinimo(minimo)
                .deficit(minDecimal.subtract(actual).max(BigDecimal.ZERO))
                .nivelCriticidad(criticidad)
                .build();
    }

    public ProductoSimpleResponse toProductoSimpleResponse(Producto producto) {
        return ProductoSimpleResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .codigoSku(producto.getCodigoSku())
                .codigoBarra(producto.getCodigoBarra())
                .unidadMedida(producto.getUnidadMedida())
                .costoPromedio(producto.getCostoPromedio())
                .stockMinimo(producto.getStockMinimo())
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin Categoría")
                .build();
    }

    public AlmacenSimpleResponse toAlmacenSimpleResponse(Almacen almacen) {
        return AlmacenSimpleResponse.builder()
                .id(almacen.getId())
                .nombre(almacen.getNombre())
                .direccion(almacen.getDireccion())
                .esPrincipal(almacen.getEsPrincipal())
                .build();
    }

    private BigDecimal cantidadDe(StockAlmacen stock) {
        return stock.getCantidadActual() != null ? stock.getCantidadActual() : BigDecimal.ZERO;
    }

    private int stockMinimoDe(Producto producto) {
        return producto.getStockMinimo() != null ? producto.getStockMinimo() : 0;
    }
}
