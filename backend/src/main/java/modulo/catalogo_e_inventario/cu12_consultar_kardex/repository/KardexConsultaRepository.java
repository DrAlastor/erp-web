package modulo.catalogo_e_inventario.cu12_consultar_kardex.repository;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lectura del Kardex para su consulta. Los movimientos salen siempre en orden cronológico,
 * porque el saldo y el costo de cada fila dependen de todas las anteriores.
 */
@Repository
public interface KardexConsultaRepository extends JpaRepository<KardexMovimiento, Long> {

    @Query("SELECT k FROM KardexMovimiento k " +
           "JOIN FETCH k.producto p " +
           "JOIN FETCH k.almacen a " +
           "WHERE p.id = :productoId AND a.id = :almacenId " +
           "ORDER BY k.fecha ASC, k.id ASC")
    List<KardexMovimiento> historialDeProducto(
            @Param("productoId") Long productoId,
            @Param("almacenId") Integer almacenId);

    @Query("SELECT k FROM KardexMovimiento k " +
           "JOIN FETCH k.producto p " +
           "JOIN FETCH k.almacen a " +
           "WHERE a.id = :almacenId " +
           "ORDER BY p.id ASC, k.fecha ASC, k.id ASC")
    List<KardexMovimiento> historialDeAlmacen(@Param("almacenId") Integer almacenId);
}
