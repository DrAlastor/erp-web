package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository;

import jakarta.persistence.LockModeType;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Bloqueo de las existencias que un pedido va a reservar. Al aprobar, dos validadores que
 * deciden a la vez pedidos distintos del mismo producto se turnan en lugar de reservar el
 * mismo stock libre dos veces. Las filas salen siempre ordenadas por producto, para que dos
 * aprobaciones que comparten productos no se bloqueen mutuamente.
 */
@Repository
public interface ExistenciaBloqueoRepository extends JpaRepository<StockAlmacen, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockAlmacen s " +
           "WHERE s.almacen.id = :sucursalId AND s.producto.id IN :productoIds " +
           "ORDER BY s.producto.id ASC")
    List<StockAlmacen> bloquear(
            @Param("sucursalId") Integer sucursalId,
            @Param("productoIds") Collection<Long> productoIds);
}
