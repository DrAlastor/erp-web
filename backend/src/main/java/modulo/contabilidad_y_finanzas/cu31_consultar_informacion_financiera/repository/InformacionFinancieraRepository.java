package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository;

import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InformacionFinancieraRepository extends JpaRepository<AsientoDetalle, Long> {

    @Query("""
        SELECT d.cuentaContable.id as cuentaId,
               COALESCE(SUM(d.debe), 0) as totalDebe,
               COALESCE(SUM(d.haber), 0) as totalHaber
        FROM AsientoDetalle d
        JOIN d.asiento a
        WHERE a.fecha <= :fechaCorte
          AND UPPER(a.estado) = 'MAYORIZADO'
        GROUP BY d.cuentaContable.id
    """)
    List<SaldoCuentaProjection> obtenerSaldosAcumuladosAFecha(
            @Param("fechaCorte") LocalDate fechaCorte
    );

    @Query("""
        SELECT d.cuentaContable.id as cuentaId,
               COALESCE(SUM(d.debe), 0) as totalDebe,
               COALESCE(SUM(d.haber), 0) as totalHaber
        FROM AsientoDetalle d
        JOIN d.asiento a
        WHERE a.fecha >= :fechaInicio
          AND a.fecha <= :fechaFin
          AND UPPER(a.estado) = 'MAYORIZADO'
        GROUP BY d.cuentaContable.id
    """)
    List<SaldoCuentaProjection> obtenerMovimientosPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
