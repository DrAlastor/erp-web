package modulo.inventario_y_existencia.movimiento_inventario.repository;

import modulo.inventario_y_existencia.movimiento_inventario.entity.KardexMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {

    @Query("SELECT k FROM KardexMovimiento k " +
           "JOIN FETCH k.producto p " +
           "JOIN FETCH k.almacen a " +
           "WHERE (:productoId IS NULL OR p.id = :productoId) AND " +
           "(:almacenId IS NULL OR a.id = :almacenId) AND " +
           "(:tipoMovimiento IS NULL OR :tipoMovimiento = '' OR k.tipoMovimiento = :tipoMovimiento) AND " +
           "(CAST(:fechaInicio AS timestamp) IS NULL OR k.fecha >= :fechaInicio) AND " +
           "(CAST(:fechaFin AS timestamp) IS NULL OR k.fecha <= :fechaFin) " +
           "ORDER BY k.fecha DESC, k.id DESC")
    Page<KardexMovimiento> buscarMovimientos(
            @Param("productoId") Long productoId,
            @Param("almacenId") Integer almacenId,
            @Param("tipoMovimiento") String tipoMovimiento,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    List<KardexMovimiento> findByProductoIdAndAlmacenIdOrderByFechaAscIdAsc(Long productoId, Integer almacenId);
}
