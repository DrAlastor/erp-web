package modulo.ventas_y_facturacion.compartido.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.compartido.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {"sucursal", "cliente", "detalles", "detalles.producto"})
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> buscarConDetalle(@Param("id") Long id);

    /**
     * Pone en fila las emisiones de comprobantes hasta el fin de la transacción (bloqueo
     * consultivo de PostgreSQL): así la numeración de notas de venta no se duplica y un mismo
     * cobro de Stripe no respalda dos comprobantes.
     */
    @Query(value = "SELECT COUNT(*) FROM (SELECT pg_advisory_xact_lock(:clave)) AS bloqueo", nativeQuery = true)
    long bloquearEmision(@Param("clave") long clave);

    @Query("SELECT MAX(v.numeroFactura) FROM Venta v WHERE v.tipoComprobante = 'NOTA_VENTA'")
    Integer ultimoNumeroNotaVenta();

    /** Bloquea la fila de la venta para que anulación y devoluciones no se pisen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> bloquear(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sucursal"})
    @Query("SELECT v FROM Venta v WHERE " +
           "(:estado IS NULL OR v.estado = :estado) AND " +
           "(:comprobante IS NULL OR v.tipoComprobante = :comprobante) AND " +
           "(:sucursalId IS NULL OR v.sucursal.id = :sucursalId) AND " +
           "(:search = '' OR LOWER(v.razonSocialCliente) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR v.nitCliente LIKE CONCAT('%', :search, '%') " +
           " OR CAST(v.numeroFactura AS string) = :search) " +
           "ORDER BY v.fecha DESC, v.id DESC")
    Page<Venta> buscar(
            @Param("search") String search,
            @Param("estado") String estado,
            @Param("comprobante") String comprobante,
            @Param("sucursalId") Integer sucursalId,
            Pageable pageable);

    /** Ventas vigentes que pueden recibir una nota de crédito (HU-20). */
    @EntityGraph(attributePaths = {"sucursal", "detalles", "detalles.producto"})
    @Query("SELECT v FROM Venta v WHERE v.estado = 'EMITIDA' AND " +
           "(:numeroFactura IS NULL OR v.numeroFactura = :numeroFactura) AND " +
           "(:nit = '' OR v.nitCliente = :nit) " +
           "ORDER BY v.fecha DESC, v.id DESC")
    List<Venta> buscarVigentes(
            @Param("numeroFactura") Integer numeroFactura,
            @Param("nit") String nit);
}
