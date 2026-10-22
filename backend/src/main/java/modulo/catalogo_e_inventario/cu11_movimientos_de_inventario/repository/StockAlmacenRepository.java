package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlmacenRepository extends JpaRepository<StockAlmacen, Long> {

    Optional<StockAlmacen> findByProductoIdAndAlmacenId(Long productoId, Integer almacenId);

    List<StockAlmacen> findByProductoId(Long productoId);

    List<StockAlmacen> findByAlmacenId(Integer almacenId);

    Page<StockAlmacen> findByAlmacenId(Integer almacenId, Pageable pageable);

    @Query(value = "SELECT s FROM StockAlmacen s " +
           "JOIN FETCH s.producto p " +
           "JOIN FETCH s.almacen a " +
           "WHERE (:almacenId IS NULL OR a.id = :almacenId) AND " +
           "(:productoId IS NULL OR p.id = :productoId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.codigoSku) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT count(s) FROM StockAlmacen s " +
           "WHERE (:almacenId IS NULL OR s.almacen.id = :almacenId) AND " +
           "(:productoId IS NULL OR s.producto.id = :productoId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(s.producto.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.producto.codigoSku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockAlmacen> buscarStock(
            @Param("almacenId") Integer almacenId,
            @Param("productoId") Long productoId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Consulta para Alertas de Stock Mínimo:
     * Encuentra todas las existencias donde la cantidad actual es menor o igual al stock mínimo.
     */
    @Query("SELECT s FROM StockAlmacen s " +
           "JOIN FETCH s.producto p " +
           "JOIN FETCH s.almacen a " +
           "WHERE (:almacenId IS NULL OR a.id = :almacenId) AND " +
           "s.cantidadActual <= COALESCE(s.stockMinimo, p.stockMinimo, 2.0) " +
           "ORDER BY (COALESCE(s.stockMinimo, p.stockMinimo, 2.0) - s.cantidadActual) DESC")
    List<StockAlmacen> findAlertasStockMinimo(@Param("almacenId") Integer almacenId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE StockAlmacen s SET s.stockMinimo = :nuevoMinimo WHERE s.producto.id = :productoId")
    void actualizarStockMinimoPorProducto(@Param("productoId") Long productoId, @Param("nuevoMinimo") java.math.BigDecimal nuevoMinimo);
}
