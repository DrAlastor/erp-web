package modulo.contabilidad_y_finanzas.cu26_periodos_contables.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Consultas del CU26 sobre la entidad compartida {@link PeriodoContable}. */
@Repository
public interface PeriodosContablesRepositorio extends JpaRepository<PeriodoContable, Long> {

    List<PeriodoContable> findByGestionOrderByMesAsc(Integer gestion);

    Optional<PeriodoContable> findByGestionAndMes(Integer gestion, Integer mes);

    @Query("select distinct p.gestion from PeriodoContable p order by p.gestion desc")
    List<Integer> gestiones();

    /** Bloquea todos los periodos de la gestión, para que cierres y reaperturas no se crucen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PeriodoContable p where p.gestion = :gestion order by p.mes")
    List<PeriodoContable> bloquearGestion(@Param("gestion") Integer gestion);

    /** Cantidad de asientos por periodo de la gestión: filas [periodo_id, cantidad]. */
    @Query(value = "select a.periodo_contable_id, count(*) from asientos_contables a "
            + "join periodos_contables p on p.id = a.periodo_contable_id "
            + "where p.gestion = :gestion group by a.periodo_contable_id", nativeQuery = true)
    List<Object[]> contarAsientosPorPeriodo(@Param("gestion") Integer gestion);
}
