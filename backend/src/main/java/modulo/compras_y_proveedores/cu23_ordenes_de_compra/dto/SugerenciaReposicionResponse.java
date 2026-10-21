package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import java.math.BigDecimal;

/**
 * Producto cuyo stock en la sucursal está por debajo del mínimo.
 *
 * @param cantidadSugerida lo que falta para volver a llegar al mínimo
 * @param ultimoCosto      costo de su última compra (o el del catálogo si nunca se compró)
 */
public record SugerenciaReposicionResponse(
        Long productoId,
        String codigo,
        String producto,
        BigDecimal stockDisponible,
        BigDecimal stockMinimo,
        BigDecimal cantidadSugerida,
        BigDecimal ultimoCosto) {
}
