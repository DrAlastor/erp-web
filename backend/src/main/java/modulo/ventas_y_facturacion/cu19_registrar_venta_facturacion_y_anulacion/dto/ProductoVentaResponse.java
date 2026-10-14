package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.math.BigDecimal;

/** Producto con su precio de venta y el stock disponible en la sucursal elegida. */
public record ProductoVentaResponse(
        Long id,
        String codigo,
        String nombre,
        BigDecimal precioVenta,
        BigDecimal stockDisponible) {
}
