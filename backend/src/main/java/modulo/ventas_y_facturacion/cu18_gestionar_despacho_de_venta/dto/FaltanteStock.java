package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.math.BigDecimal;

/** Producto que no alcanza en la sucursal: cuánto se pide y cuánto hay. */
public record FaltanteStock(
        Long productoId,
        String productoNombre,
        BigDecimal requerido,
        BigDecimal disponible) {
}
