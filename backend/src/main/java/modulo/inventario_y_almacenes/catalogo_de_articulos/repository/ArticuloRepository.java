package modulo.inventario_y_almacenes.catalogo_de_articulos.repository;

import modulo.inventario_y_almacenes.catalogo_de_articulos.entity.Articulo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo de artículos. Las consultas traen la categoría con {@code LEFT JOIN FETCH} porque
 * la asociación es perezosa y la respuesta la necesita fuera de la transacción.
 */
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria ORDER BY a.nombre")
    List<Articulo> findAllWithCategoria();

    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria WHERE a.id = :id")
    Optional<Articulo> findByIdWithCategoria(@Param("id") Long id);

    @Query("""
            SELECT a FROM Articulo a
            LEFT JOIN FETCH a.categoria
            WHERE a.categoria.id = :categoriaId
            ORDER BY a.nombre
            """)
    List<Articulo> findByCategoriaIdWithCategoria(@Param("categoriaId") Integer categoriaId);
}
