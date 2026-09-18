package modulo.inventario_y_almacenes.catalogo_de_articulos.mapper;

import modulo.inventario_y_almacenes.catalogo_de_articulos.dto.ArticuloRequest;
import modulo.inventario_y_almacenes.catalogo_de_articulos.dto.ArticuloResponse;
import modulo.inventario_y_almacenes.catalogo_de_articulos.entity.Articulo;
import modulo.inventario_y_almacenes.compartido.entity.Categoria;

import org.springframework.stereotype.Component;

/**
 * Traduce entre la entidad {@link Articulo} y los DTO del catálogo.
 *
 * <p>La categoría se resuelve en el servicio (con la entidad compartida del inventario) y el
 * mapper solo se encarga de la conversión, incluida la categoría nula de un artículo sin
 * clasificar.
 */
@Component
public class ArticuloMapper {

    public ArticuloResponse toResponse(Articulo articulo) {
        Categoria categoria = articulo.getCategoria();
        return new ArticuloResponse(
                articulo.getId(),
                articulo.getSku(),
                articulo.getNombre(),
                articulo.getDescripcion(),
                articulo.getPrecio(),
                articulo.getStock(),
                articulo.getImagenUrl(),
                categoria == null ? null
                        : new ArticuloResponse.CategoriaResponse(
                                categoria.getId(), categoria.getNombre(), categoria.getDescripcion()),
                articulo.getActivo(),
                articulo.getFechaCreacion());
    }

    /** Arma la entidad del alta; la categoría la asigna el servicio. */
    public Articulo toEntity(ArticuloRequest request) {
        Articulo articulo = new Articulo();
        aplicar(articulo, request);
        articulo.setActivo(request.activo() == null || request.activo());
        return articulo;
    }

    /** Copia los campos editables del DTO sobre una entidad existente. */
    public void aplicar(Articulo articulo, ArticuloRequest request) {
        articulo.setSku(request.sku().trim());
        articulo.setNombre(request.nombre().trim());
        articulo.setDescripcion(request.descripcion());
        articulo.setPrecio(request.precio());
        articulo.setStock(request.stock() == null ? 0 : request.stock());
        articulo.setImagenUrl(request.imagenUrl());
        if (request.activo() != null) {
            articulo.setActivo(request.activo());
        }
    }
}
