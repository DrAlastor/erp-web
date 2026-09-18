package modulo.inventario_y_almacenes.catalogo_de_articulos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Artículo tal como lo devuelve la API.
 *
 * <p>La categoría viaja aplanada a lo que muestra el catálogo: identificador y nombre.
 */
public record ArticuloResponse(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        CategoriaResponse categoria,
        Boolean activo,
        LocalDateTime fechaCreacion) {

    /** Categoría del artículo, compartida con los movimientos de inventario. */
    public record CategoriaResponse(Integer id, String nombre, String descripcion) {
    }
}
