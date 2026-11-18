package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository;

import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.SaldoCuentaProjection;
import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LibroMayorRepository extends JpaRepository<AsientoDetalle, Long> {

    @Query("""
        SELECT COALESCE(SUM(d.debe), 0)
        FROM AsientoDetalle d
        JOIN d.asiento a
        WHERE d.cuentaContable.id = :cuentaId
          AND a.fecha < :fechaCorte
          AND UPPER(a.estado) = 'MAYORIZADO'
    """)
    BigDecimal sumDebeAnterior(
            @Param("cuentaId") Long cuentaId,
            @Param("fechaCorte") LocalDate fechaCorte
    );

    @Query("""
        SELECT COALESCE(SUM(d.haber), 0)
        FROM AsientoDetalle d
        JOIN d.asiento a
        WHERE d.cuentaContable.id = :cuentaId
          AND a.fecha < :fechaCorte
          AND UPPER(a.estado) = 'MAYORIZADO'
    """)
    BigDecimal sumHaberAnterior(
            @Param("cuentaId") Long cuentaId,
            @Param("fechaCorte") LocalDate fechaCorte
    );

    @Query("""
        SELECT d FROM AsientoDetalle d
        JOIN FETCH d.asiento a
        JOIN FETCH d.cuentaContable cc
        WHERE d.cuentaContable.id = :cuentaId
          AND a.fecha >= :fechaInicio
          AND a.fecha <= :fechaFin
          AND UPPER(a.estado) = 'MAYORIZADO'
        ORDER BY a.fecha ASC, a.numeroAsiento ASC, d.id ASC
    """)
    List<AsientoDetalle> findMovimientosMayor(
            @Param("cuentaId") Long cuentaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
        SELECT d.cuentaContable.id as cuentaId,
               COALESCE(SUM(d.debe), 0) as totalDebe,
               COALESCE(SUM(d.haber), 0) as totalHaber
        FROM AsientoDetalle d
        JOIN d.asiento a
        WHERE a.fecha < :fechaCorte
          AND UPPER(a.estado) = 'MAYORIZADO'
        GROUP BY d.cuentaContable.id
    """)
    List<SaldoCuentaProjection> sumSaldosAnterioresBatch(@Param("fechaCorte") LocalDate fechaCorte);

    @Query("""
        SELECT d FROM AsientoDetalle d
        JOIN FETCH d.asiento a
        JOIN FETCH d.cuentaContable cc
        WHERE a.fecha >= :fechaInicio
          AND a.fecha <= :fechaFin
          AND UPPER(a.estado) = 'MAYORIZADO'
        ORDER BY cc.codigo ASC, a.fecha ASC, a.numeroAsiento ASC, d.id ASC
    """)
    List<AsientoDetalle> findMovimientosMayorBatch(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
