package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository;

import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoVentaRepository extends JpaRepository<PedidoVenta, Long> {

    @EntityGraph(attributePaths = "detalles")
    @Query("SELECT p FROM PedidoVenta p WHERE p.id = :id")
    Optional<PedidoVenta> findConDetalles(@Param("id") Long id);

    /**
     * Mayor correlativo usado en el año indicado por los numeros con formato
     * {@code PED-<yyyy>-<n>}. Los numeros con otro formato se ignoran.
     */
    @Query(
            value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_pedido FROM :desde) AS INTEGER)), 0) "
                    + "FROM pedidos WHERE numero_pedido ~ :patron",
            nativeQuery = true)
    int maxCorrelativo(@Param("desde") int desde, @Param("patron") String patron);
}
