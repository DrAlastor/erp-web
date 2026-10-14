package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.repository;

import jakarta.persistence.LockModeType;

import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.NotaDespacho;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotaDespachoRepository extends JpaRepository<NotaDespacho, Integer> {

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente", "sucursal"})
    @Query("select n from NotaDespacho n "
            + "where (:estado is null or n.estado = :estado) "
            + "and (:pedidoId is null or n.pedido.id = :pedidoId) "
            + "and (:sucursalId is null or n.sucursal.id = :sucursalId) "
            + "and (:texto = '' or lower(n.numeroDespacho) like lower(concat('%', :texto, '%')) "
            + "     or lower(n.pedido.numeroPedido) like lower(concat('%', :texto, '%')) "
            + "     or lower(n.pedido.cliente.razonSocial) like lower(concat('%', :texto, '%')))")
    Page<NotaDespacho> buscar(@Param("texto") String texto,
                              @Param("estado") EstadoDespacho estado,
                              @Param("pedidoId") Integer pedidoId,
                              @Param("sucursalId") Integer sucursalId,
                              Pageable pagina);

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente", "sucursal", "detalles", "detalles.producto"})
    @Query("select n from NotaDespacho n where n.id = :id")
    Optional<NotaDespacho> buscarCompleta(@Param("id") Integer id);

    /** Bloquea la nota: una misma nota no se confirma ni anula dos veces en paralelo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotaDespacho n where n.id = :id")
    Optional<NotaDespacho> bloquear(@Param("id") Integer id);

    /**
     * Suma por producto de lo que figura en las notas del pedido que están en los estados
     * indicados, sin contar la nota {@code excluirId}. Devuelve pares [productoId, cantidad].
     */
    @Query("select d.producto.id, sum(d.cantidad) from DespachoDetalle d "
            + "where d.despacho.pedido.id = :pedidoId "
            + "and d.despacho.estado in :estados "
            + "and (:excluirId is null or d.despacho.id <> :excluirId) "
            + "group by d.producto.id")
    List<Object[]> sumarPorProducto(@Param("pedidoId") Integer pedidoId,
                                    @Param("estados") Collection<EstadoDespacho> estados,
                                    @Param("excluirId") Integer excluirId);
}
