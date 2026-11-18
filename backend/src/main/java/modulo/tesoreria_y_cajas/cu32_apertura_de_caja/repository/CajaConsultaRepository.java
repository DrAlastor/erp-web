package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Consultas de la caja sobre tablas que no tienen entidad JPA en este módulo
 * (COBROS de cuentas por cobrar, del esquema Tenant_Esquema.sql).
 */
@Repository
@RequiredArgsConstructor
public class CajaConsultaRepository {

    private final JdbcTemplate jdbc;

    /** Cobros de cuentas por cobrar recibidos en efectivo dentro del turno. */
    public BigDecimal cobrosCxcEfectivo(Long turnoId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(c.monto), 0)
                FROM cobros c JOIN formas_pago f ON f.id = c.forma_pago_id
                WHERE c.turno_caja_id = ? AND UPPER(f.codigo) = 'EFECTIVO'
                """, BigDecimal.class, turnoId);
    }
}
