package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity.PedidoAValidar;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity.PedidoAValidarDetalle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoAValidarRepository extends JpaRepository<PedidoAValidar, Integer> {

    /** Pedidos en un estado, o todos si no se indica; los más antiguos primero. */
    @Query("SELECT p FROM PedidoAValidar p WHERE (:estado IS NULL OR p.estado = :estado) " +
           "ORDER BY p.fechaCreacion ASC, p.id ASC")
    Page<PedidoAValidar> buscarPorEstado(@Param("estado") String estado, Pageable pageable);

    /** Bloquea el pedido mientras se decide, para que dos validadores no lo resuelvan a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PedidoAValidar p WHERE p.id = :id")
    Optional<PedidoAValidar> buscarParaDecidir(@Param("id") Integer id);

    @Query("SELECT d FROM PedidoAValidarDetalle d WHERE d.pedido.id = :pedidoId ORDER BY d.id ASC")
    List<PedidoAValidarDetalle> detallesDe(@Param("pedidoId") Integer pedidoId);

    /**
     * Unidades de un producto ya comprometidas en otros pedidos de la sucursal y todavía sin
     * salir del almacén: lo pedido por los pedidos APROBADOS o con DESPACHO_PARCIAL, menos lo
     * que sus despachos ya confirmaron (DESPACHADO o ENTREGADO). Las notas en PREPARACION no
     * movieron stock, así que su cantidad sigue reservada. Es la existencia que no se puede
     * volver a prometer.
     */
    @Query(value = """
            SELECT COALESCE(SUM(GREATEST(d.cantidad - COALESCE(x.despachado, 0), 0)), 0)
            FROM pedido_detalles d
            JOIN pedidos p ON p.id = d.pedido_id
            LEFT JOIN (
                SELECT n.pedido_id, dd.producto_id, SUM(dd.cantidad) AS despachado
                FROM notas_despacho n
                JOIN despacho_detalles dd ON dd.despacho_id = n.id
                WHERE UPPER(n.estado) IN ('DESPACHADO', 'ENTREGADO')
                GROUP BY n.pedido_id, dd.producto_id
            ) x ON x.pedido_id = p.id AND x.producto_id = d.producto_id
            WHERE d.producto_id = :productoId
              AND p.sucursal_id = :sucursalId
              AND UPPER(p.estado) IN ('APROBADO', 'DESPACHO_PARCIAL')
              AND p.id <> :excluirPedidoId
            """, nativeQuery = true)
    BigDecimal cantidadReservada(
            @Param("productoId") Integer productoId,
            @Param("sucursalId") Integer sucursalId,
            @Param("excluirPedidoId") Integer excluirPedidoId);
}
