package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.SecuenciaFacturacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SecuenciaFacturacionRepository extends JpaRepository<SecuenciaFacturacion, Long> {

    String VIGENTES = "SELECT s FROM SecuenciaFacturacion s WHERE s.activo = true " +
            "AND s.fechaLimite >= :hoy AND s.numeroActual < s.numeroFinal ORDER BY s.id ASC";

    /** Dosificación vigente, bloqueada para asignar el siguiente número sin duplicarlo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(VIGENTES)
    List<SecuenciaFacturacion> bloquearVigentes(@Param("hoy") LocalDate hoy, Pageable pageable);

    @Query(VIGENTES)
    List<SecuenciaFacturacion> vigentes(@Param("hoy") LocalDate hoy, Pageable pageable);

    List<SecuenciaFacturacion> findAllByOrderByIdDesc();

    List<SecuenciaFacturacion> findByActivoTrue();

    boolean existsByNumeroAutorizacion(String numeroAutorizacion);
}
