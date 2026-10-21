package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.repository;

import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.Cotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.EstadoCotizacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    @EntityGraph(attributePaths = {"cliente"})
    @Query("select c from Cotizacion c "
            + "where (:estado is null or c.estado = :estado) "
            + "and (:clienteId is null or c.cliente.id = :clienteId) "
            + "and (:texto = '' or lower(c.numeroCotizacion) like lower(concat('%', :texto, '%')) "
            + "     or lower(c.cliente.razonSocial) like lower(concat('%', :texto, '%')) "
            + "     or lower(c.cliente.nitCi) like lower(concat('%', :texto, '%')))")
    Page<Cotizacion> buscar(@Param("texto") String texto,
                            @Param("estado") EstadoCotizacion estado,
                            @Param("clienteId") Long clienteId,
                            Pageable pagina);

    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto"})
    @Query("select c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> buscarCompleta(@Param("id") Integer id);

    /**
     * Pasa a VENCIDA toda cotización vigente cuyo plazo ya terminó. Se ejecuta antes de
     * consultar, así el estado que ve el usuario siempre está al día sin depender de una
     * tarea programada.
     */
    @Modifying
    @Query(value = "update cotizaciones set estado = 'VENCIDA' "
            + "where estado in ('EMITIDA', 'ACEPTADA') "
            + "and fecha + validez_dias < current_date", nativeQuery = true)
    int vencerCotizacionesExpiradas();

    /**
     * Límite de crédito del cliente. La columna existe en la tabla CLIENTES pero la entidad
     * de CU13 no la mapea todavía, por eso se lee directo.
     */
    @Query(value = "select limite_credito from clientes where id = :clienteId", nativeQuery = true)
    BigDecimal limiteCreditoDe(@Param("clienteId") Long clienteId);
}
