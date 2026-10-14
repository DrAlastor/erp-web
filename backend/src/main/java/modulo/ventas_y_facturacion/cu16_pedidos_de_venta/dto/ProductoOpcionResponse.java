package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import java.math.BigDecimal;

/** Producto activo seleccionable, con el stock disponible en la sucursal elegida. */
public record ProductoOpcionResponse(
        Long id, String codigo, String nombre, BigDecimal precioVenta, BigDecimal stockDisponible) {}
