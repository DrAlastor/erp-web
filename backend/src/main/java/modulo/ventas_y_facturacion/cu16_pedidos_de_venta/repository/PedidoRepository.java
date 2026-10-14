package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.Pedido;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoDetalle;
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
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /** Pedidos en un estado, o todos si no se indica; los más antiguos primero. */
    @Query("SELECT p FROM Pedido p WHERE (:estado IS NULL OR p.estado = :estado) " +
           "ORDER BY p.fechaCreacion ASC, p.id ASC")
    Page<Pedido> buscarPorEstado(@Param("estado") String estado, Pageable pageable);

    /** Bloquea el pedido mientras se decide, para que dos validadores no lo resuelvan a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarParaDecidir(@Param("id") Integer id);

    @Query("SELECT d FROM PedidoDetalle d WHERE d.pedido.id = :pedidoId ORDER BY d.id ASC")
    List<PedidoDetalle> detallesDe(@Param("pedidoId") Integer pedidoId);

    /**
     * Unidades de un producto ya comprometidas en otros pedidos aprobados de la sucursal y
     * todavía sin despachar. Es la existencia reservada que no se puede volver a prometer.
     */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM PedidoDetalle d " +
           "WHERE d.productoId = :productoId AND d.pedido.sucursalId = :sucursalId " +
           "AND d.pedido.estado = 'APROBADO' AND d.pedido.id <> :excluirPedidoId")
    BigDecimal cantidadReservada(
            @Param("productoId") Integer productoId,
            @Param("sucursalId") Integer sucursalId,
            @Param("excluirPedidoId") Integer excluirPedidoId);
}
