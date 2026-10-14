package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.repository;

import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {

    boolean existsByVentaIdAndEstado(Long ventaId, String estado);

    @Query("SELECT MAX(n.numeroNota) FROM NotaCredito n WHERE n.numeroNota LIKE :prefijo")
    String ultimoNumero(@Param("prefijo") String prefijo);

    @EntityGraph(attributePaths = {"venta", "venta.sucursal", "detalles", "detalles.producto"})
    @Query("SELECT n FROM NotaCredito n WHERE n.id = :id")
    Optional<NotaCredito> buscarConDetalle(@Param("id") Long id);

    @EntityGraph(attributePaths = {"venta", "venta.sucursal"})
    @Query("SELECT n FROM NotaCredito n WHERE " +
           "(:search = '' OR LOWER(n.numeroNota) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR n.venta.nitCliente LIKE CONCAT('%', :search, '%') " +
           " OR LOWER(n.venta.razonSocialCliente) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR CAST(n.venta.numeroFactura AS string) = :search) " +
           "ORDER BY n.fechaCreacion DESC, n.id DESC")
    Page<NotaCredito> buscar(@Param("search") String search, Pageable pageable);

    /**
     * Cantidad ya devuelta por cada línea de la factura, contando solo notas vigentes.
     * Cada fila es {ventaDetalleId, cantidadDevuelta}.
     */
    @Query("SELECT d.ventaDetalleId, SUM(d.cantidad) FROM NotaCreditoDetalle d " +
           "WHERE d.notaCredito.venta.id IN :ventaIds AND d.notaCredito.estado = 'EMITIDA' " +
           "AND d.ventaDetalleId IS NOT NULL GROUP BY d.ventaDetalleId")
    List<Object[]> cantidadesDevueltas(@Param("ventaIds") List<Long> ventaIds);
}
