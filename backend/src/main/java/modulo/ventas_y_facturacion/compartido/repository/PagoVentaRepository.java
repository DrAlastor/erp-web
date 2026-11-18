package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.PagoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {

    /** Una sesión de pago en línea solo puede respaldar una factura, aunque luego se anule. */
    boolean existsByTransaccionExterna(String transaccionExterna);

    @Query("SELECT COALESCE(SUM(p.importe), 0) FROM PagoVenta p " +
           "WHERE p.venta.turnoCajaId = :turnoId AND p.estado = 'COMPLETADO'")
    BigDecimal sumarCobrosPorTurno(@Param("turnoId") Long turnoId);

    @Query("SELECT COALESCE(SUM(p.importe), 0) FROM PagoVenta p " +
           "WHERE p.venta.turnoCajaId = :turnoId AND p.estado = 'COMPLETADO' " +
           "AND UPPER(p.formaPago.codigo) = 'EFECTIVO'")
    BigDecimal sumarCobrosEfectivoPorTurno(@Param("turnoId") Long turnoId);
}
