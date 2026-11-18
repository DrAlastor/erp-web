package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Consultas del CU25 sobre la entidad compartida {@link CuentaContable}. */
@Repository
public interface PlanCuentasRepositorio extends JpaRepository<CuentaContable, Long> {

    List<CuentaContable> findAllByOrderByCodigoAsc();

    @Query("select c from CuentaContable c "
            + "where (:texto = '' or lower(c.codigo) like lower(concat('%', :texto, '%')) "
            + "       or lower(c.nombre) like lower(concat('%', :texto, '%'))) "
            + "and (:tipo = '' or c.tipo = :tipo) "
            + "and (:activo is null or c.activo = :activo) "
            + "and (:soloMovimiento = false or c.permiteMovimiento = true)")
    Page<CuentaContable> buscar(@Param("texto") String texto,
                                @Param("tipo") String tipo,
                                @Param("activo") Boolean activo,
                                @Param("soloMovimiento") boolean soloMovimiento,
                                Pageable pagina);

    boolean existsByCodigo(String codigo);

    boolean existsByPadreId(Long padreId);

    boolean existsByPadreIdAndActivoTrue(Long padreId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaContable c where c.id = :id")
    Optional<CuentaContable> bloquear(@Param("id") Long id);

    @Query(value = "select exists(select 1 from asiento_detalles where cuenta_contable_id = :id)", nativeQuery = true)
    boolean tieneMovimientos(@Param("id") Long id);

    /** Ids de las cuentas que tienen al menos una línea de asiento, para marcar el árbol de una vez. */
    @Query(value = "select distinct cuenta_contable_id from asiento_detalles", nativeQuery = true)
    List<Number> idsConMovimientos();
}
