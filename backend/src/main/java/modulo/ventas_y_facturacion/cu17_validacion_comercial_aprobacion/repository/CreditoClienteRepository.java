package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lectura de los datos de crédito de un cliente: su condición de pago y límite (tabla
 * {@code clientes}) y su deuda (tabla {@code cuentas_por_cobrar}). Son consultas de solo
 * lectura sobre tablas que administran otros casos de uso, por eso no se mapean como
 * entidades de este paquete.
 */
@Repository
@RequiredArgsConstructor
public class CreditoClienteRepository {

    private final JdbcTemplate jdbc;

    /** Datos comerciales de un cliente. */
    public record DatosCliente(Integer id, String razonSocial, String nitCi, String condicionPago,
                               BigDecimal limiteCredito, Integer diasCredito, boolean activo) {
    }

    /** Deuda de un cliente: lo que debe en total y la parte ya vencida. */
    public record DeudaCliente(BigDecimal saldoPendiente, BigDecimal saldoVencido, int cuotasVencidas) {
    }

    public Optional<DatosCliente> buscarCliente(Integer clienteId) {
        List<DatosCliente> filas = jdbc.query(
                "SELECT id, razon_social, nit_ci, condicion_pago, limite_credito, dias_credito, activo " +
                "FROM clientes WHERE id = ?",
                (rs, n) -> new DatosCliente(
                        rs.getInt("id"),
                        rs.getString("razon_social"),
                        rs.getString("nit_ci"),
                        rs.getString("condicion_pago"),
                        rs.getBigDecimal("limite_credito"),
                        (Integer) rs.getObject("dias_credito"),
                        rs.getBoolean("activo")),
                clienteId);
        return filas.stream().findFirst();
    }

    /** Razón social de varios clientes de una vez, para listados. */
    public Map<Integer, String> razonesSociales(Collection<Integer> clienteIds) {
        Map<Integer, String> nombres = new HashMap<>();
        if (clienteIds.isEmpty()) {
            return nombres;
        }
        String marcas = String.join(",", clienteIds.stream().map(id -> "?").toList());
        jdbc.query("SELECT id, razon_social FROM clientes WHERE id IN (" + marcas + ")",
                rs -> {
                    nombres.put(rs.getInt("id"), rs.getString("razon_social"));
                },
                clienteIds.toArray());
        return nombres;
    }

    /** Cuentas por cobrar abiertas del cliente; vencidas son las que vencieron antes de {@code hoy}. */
    public DeudaCliente deudaDe(Integer clienteId, LocalDate hoy) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(saldo), 0) AS pendiente, " +
                "COALESCE(SUM(CASE WHEN fecha_vencimiento < ? THEN saldo ELSE 0 END), 0) AS vencido, " +
                "COALESCE(SUM(CASE WHEN fecha_vencimiento < ? THEN 1 ELSE 0 END), 0) AS cuotas " +
                "FROM cuentas_por_cobrar " +
                "WHERE cliente_id = ? AND saldo > 0 AND UPPER(estado) NOT IN ('PAGADO', 'ANULADO', 'ANULADA')",
                (rs, n) -> new DeudaCliente(
                        rs.getBigDecimal("pendiente"),
                        rs.getBigDecimal("vencido"),
                        rs.getInt("cuotas")),
                java.sql.Date.valueOf(hoy), java.sql.Date.valueOf(hoy), clienteId);
    }
}
