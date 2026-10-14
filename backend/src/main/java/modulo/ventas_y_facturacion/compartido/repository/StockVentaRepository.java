package modulo.ventas_y_facturacion.compartido.repository;

import jakarta.persistence.LockModeType;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso de Ventas a las existencias de inventario_sucursal (entidad de la CU11).
 *
 * <p>Existe aparte del repositorio de la CU11 porque las ventas necesitan bloquear la
 * fila de stock mientras la descuentan: sin el bloqueo, dos cajas podrían vender la
 * última unidad al mismo tiempo.
 */
@Repository
public interface StockVentaRepository extends JpaRepository<StockAlmacen, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockAlmacen s WHERE s.producto.id = :productoId AND s.almacen.id = :sucursalId")
    Optional<StockAlmacen> bloquear(@Param("productoId") Long productoId, @Param("sucursalId") Integer sucursalId);

    /** Lectura sin bloqueo, para validar una venta antes de cobrarla. */
    @Query("SELECT s FROM StockAlmacen s WHERE s.producto.id = :productoId AND s.almacen.id = :sucursalId")
    Optional<StockAlmacen> consultar(@Param("productoId") Long productoId, @Param("sucursalId") Integer sucursalId);

    /** Productos activos con stock en la sucursal, para el punto de venta. */
    @Query("SELECT s FROM StockAlmacen s JOIN FETCH s.producto p " +
           "WHERE s.almacen.id = :sucursalId AND p.activo = true AND s.cantidadActual > 0 AND " +
           "(:search = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(p.codigoSku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.nombre ASC")
    List<StockAlmacen> disponiblesEnSucursal(
            @Param("sucursalId") Integer sucursalId,
            @Param("search") String search,
            Pageable pageable);
}
