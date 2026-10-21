package modulo.ventas_y_facturacion.compartido.repository;

import jakarta.persistence.LockModeType;

import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /**
     * Bloquea el pedido mientras se despacha: dos confirmaciones simultáneas del mismo
     * pedido no pueden despachar dos veces la misma cantidad pendiente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"cliente", "sucursal"})
    @Query("select p from Pedido p where p.estado in :estados "
            + "and (:sucursalId is null or p.sucursal.id = :sucursalId) order by p.fecha, p.id")
    List<Pedido> buscarPorEstados(@Param("estados") Collection<EstadoPedido> estados,
                                  @Param("sucursalId") Integer sucursalId);
}
