package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository;

import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CompraSinCuentaResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.PagoProveedorResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ProveedorOpcionResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ResumenCxpResponse.ProveedorSaldo;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.CuentaPorPagar;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.EstadoCuentaPorPagar.SaldoVencido;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lecturas de solo consulta de la CU34: cuotas con su compra, proveedor y sucursal, pagos
 * con forma de pago y usuario, agregados del resumen y opciones de los formularios.
 *
 * <p>Como en la CU16, se usa SQL propio para leer tablas de otros modulos (compras,
 * proveedores, sucursal, formas_pago, usuario, empleados) sin depender de su mapeo JPA.
 * Las cuotas ANULADA (si otra rama las usara) no cuentan en los agregados.
 */
@Repository
@RequiredArgsConstructor
public class CuentaPorPagarConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Cuota con los datos de compra, proveedor y sucursal ya resueltos. */
    public record CuentaFila(
            Long id,
            Long compraId,
            String numeroFactura,
            Long proveedorId,
            String proveedorRazonSocial,
            String proveedorNitCi,
            Integer numeroCuota,
            BigDecimal montoTotal,
            BigDecimal montoPagado,
            BigDecimal saldo,
            LocalDate fechaVencimiento,
            String estado,
            LocalDate fechaCompra,
            BigDecimal totalCompra,
            String sucursalNombre) {}

    public record Totales(
            BigDecimal saldoTotal,
            BigDecimal vencido,
            BigDecimal porVencer,
            long cantidadPendientes,
            long cantidadVencidas) {}

    private static final String FROM_CUENTAS = """
             FROM cuentas_por_pagar cx
             LEFT JOIN compras co ON co.id = cx.compra_id
             LEFT JOIN proveedores pv ON pv.id = cx.proveedor_id
            """;

    private static final String SELECT_CUENTA = """
            SELECT cx.id, cx.compra_id, co.numero_factura, cx.proveedor_id, pv.razon_social,
                   pv.nit_ci, cx.numero_cuota, cx.monto_total, cx.monto_pagado, cx.saldo,
                   cx.fecha_vencimiento, cx.estado, co.fecha AS fecha_compra,
                   co.total AS total_compra, s.nombre AS sucursal_nombre
            """ + FROM_CUENTAS + " LEFT JOIN sucursal s ON s.id = co.sucursal_id";

    /** Condicion de cuota con deuda viva (no pagada ni anulada). */
    private static final String CON_SALDO = " cx.saldo > 0 AND UPPER(cx.estado) <> 'ANULADA'";

    private static final RowMapper<CuentaFila> CUENTA = (rs, n) -> new CuentaFila(
            rs.getLong("id"),
            rs.getLong("compra_id"),
            rs.getString("numero_factura"),
            rs.getLong("proveedor_id"),
            rs.getString("razon_social"),
            rs.getString("nit_ci"),
            rs.getInt("numero_cuota"),
            rs.getBigDecimal("monto_total"),
            rs.getBigDecimal("monto_pagado"),
            rs.getBigDecimal("saldo"),
            fecha(rs.getDate("fecha_vencimiento")),
            rs.getString("estado"),
            fecha(rs.getDate("fecha_compra")),
            rs.getBigDecimal("total_compra"),
            rs.getString("sucursal_nombre"));

    /** Pagina ordenada por vencimiento asc e id asc. */
    public List<CuentaFila> listar(
            String search, String estado, Long proveedorId, LocalDate hoy, int limit, long offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, proveedorId, hoy, params);
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(
                SELECT_CUENTA + where
                        + " ORDER BY cx.fecha_vencimiento ASC, cx.id ASC LIMIT :limit OFFSET :offset",
                params,
                CUENTA);
    }

    public long contar(String search, String estado, Long proveedorId, LocalDate hoy) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, proveedorId, hoy, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM_CUENTAS + where, params, Long.class);
        return total == null ? 0 : total;
    }

    public Optional<CuentaFila> cuenta(Long id) {
        return jdbc.query(SELECT_CUENTA + " WHERE cx.id = :id", new MapSqlParameterSource("id", id), CUENTA)
                .stream()
                .findFirst();
    }

    public List<CuentaFila> cuotasDeCompra(Long compraId) {
        return jdbc.query(
                SELECT_CUENTA + " WHERE cx.compra_id = :compraId ORDER BY cx.numero_cuota, cx.id",
                new MapSqlParameterSource("compraId", compraId),
                CUENTA);
    }

    /** Historial de pagos de la cuota, del mas reciente al mas antiguo. */
    public List<PagoProveedorResponse> pagos(Long cuentaId) {
        return jdbc.query(
                """
                SELECT pg.id, pg.forma_pago_id, fp.nombre AS forma_pago_nombre, pg.monto,
                       pg.fecha_pago, pg.numero_recibo, pg.transaccion_externa, pg.observaciones,
                       COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email) AS usuario_nombre,
                       pg.fecha_creacion
                FROM pagos_proveedor pg
                LEFT JOIN formas_pago fp ON fp.id = pg.forma_pago_id
                LEFT JOIN usuario u ON u.id = pg.usuario_id
                LEFT JOIN empleados e ON e.id = u.empleado_id
                WHERE pg.cuenta_por_pagar_id = :id
                ORDER BY pg.fecha_pago DESC, pg.id DESC
                """,
                new MapSqlParameterSource("id", cuentaId),
                (rs, n) -> {
                    Timestamp creacion = rs.getTimestamp("fecha_creacion");
                    return new PagoProveedorResponse(
                            rs.getLong("id"),
                            rs.getLong("forma_pago_id"),
                            rs.getString("forma_pago_nombre"),
                            rs.getBigDecimal("monto"),
                            fecha(rs.getDate("fecha_pago")),
                            rs.getString("numero_recibo"),
                            rs.getString("transaccion_externa"),
                            rs.getString("observaciones"),
                            rs.getString("usuario_nombre"),
                            creacion == null ? null : creacion.toLocalDateTime());
                });
    }

    /** Saldo total, vencido, por vencer hasta {@code hasta} y cantidades de cuotas con saldo. */
    public Totales totales(LocalDate hoy, LocalDate hasta) {
        return jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(cx.saldo), 0) AS saldo_total,
                       COALESCE(SUM(CASE WHEN cx.fecha_vencimiento < :hoy THEN cx.saldo END), 0) AS vencido,
                       COALESCE(SUM(CASE WHEN cx.fecha_vencimiento BETWEEN :hoy AND :hasta
                                         THEN cx.saldo END), 0) AS por_vencer,
                       COUNT(*) AS cantidad,
                       COUNT(CASE WHEN cx.fecha_vencimiento < :hoy THEN 1 END) AS vencidas
                FROM cuentas_por_pagar cx
                WHERE""" + CON_SALDO,
                new MapSqlParameterSource("hoy", Date.valueOf(hoy)).addValue("hasta", Date.valueOf(hasta)),
                (rs, n) -> new Totales(
                        rs.getBigDecimal("saldo_total"),
                        rs.getBigDecimal("vencido"),
                        rs.getBigDecimal("por_vencer"),
                        rs.getLong("cantidad"),
                        rs.getLong("vencidas")));
    }

    /** Suma de los pagos con fecha de pago en [desde, hasta). */
    public BigDecimal pagado(LocalDate desde, LocalDate hastaExclusive) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(monto), 0) FROM pagos_proveedor "
                        + "WHERE fecha_pago >= :desde AND fecha_pago < :hasta",
                new MapSqlParameterSource("desde", Date.valueOf(desde))
                        .addValue("hasta", Date.valueOf(hastaExclusive)),
                BigDecimal.class);
    }

    public List<SaldoVencido> saldosVencidos(LocalDate hoy) {
        return jdbc.query(
                "SELECT cx.fecha_vencimiento, cx.saldo FROM cuentas_por_pagar cx WHERE"
                        + CON_SALDO + " AND cx.fecha_vencimiento < :hoy",
                new MapSqlParameterSource("hoy", Date.valueOf(hoy)),
                (rs, n) -> new SaldoVencido(fecha(rs.getDate("fecha_vencimiento")), rs.getBigDecimal("saldo")));
    }

    public List<ProveedorSaldo> topProveedores(int limit) {
        return jdbc.query(
                "SELECT cx.proveedor_id, pv.razon_social, SUM(cx.saldo) AS saldo"
                        + " FROM cuentas_por_pagar cx LEFT JOIN proveedores pv ON pv.id = cx.proveedor_id"
                        + " WHERE" + CON_SALDO
                        + " GROUP BY cx.proveedor_id, pv.razon_social ORDER BY saldo DESC LIMIT :limit",
                new MapSqlParameterSource("limit", limit),
                (rs, n) -> new ProveedorSaldo(
                        rs.getLong("proveedor_id"), rs.getString("razon_social"), rs.getBigDecimal("saldo")));
    }

    /** Compras a credito, no anuladas y sin cuotas, de la mas reciente a la mas antigua. */
    public List<CompraSinCuentaResponse> comprasSinCuenta(String search, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = "";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where = " AND (LOWER(co.numero_factura) LIKE :term ESCAPE '!'"
                    + " OR LOWER(pv.razon_social) LIKE :term ESCAPE '!'"
                    + " OR LOWER(pv.nit_ci) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                """
                SELECT co.id, co.numero_factura, pv.razon_social, co.fecha, co.total, pv.dias_credito
                FROM compras co
                JOIN proveedores pv ON pv.id = co.proveedor_id
                WHERE UPPER(co.tipo_pago) = 'CREDITO'
                  AND UPPER(co.estado) NOT LIKE 'ANULAD%'
                  AND NOT EXISTS (SELECT 1 FROM cuentas_por_pagar cx WHERE cx.compra_id = co.id)
                """ + where + " ORDER BY co.fecha DESC, co.id DESC LIMIT :limit",
                params,
                (rs, n) -> new CompraSinCuentaResponse(
                        rs.getLong("id"),
                        rs.getString("numero_factura"),
                        rs.getString("razon_social"),
                        fecha(rs.getDate("fecha")),
                        rs.getBigDecimal("total"),
                        rs.getObject("dias_credito", Integer.class)));
    }

    public List<ProveedorOpcionResponse> proveedoresActivos(String search, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = " WHERE activo = TRUE";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where += " AND (LOWER(razon_social) LIKE :term ESCAPE '!' OR LOWER(nit_ci) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                "SELECT id, razon_social, nit_ci FROM proveedores" + where
                        + " ORDER BY razon_social LIMIT :limit",
                params,
                (rs, n) -> new ProveedorOpcionResponse(
                        rs.getLong("id"), rs.getString("razon_social"), rs.getString("nit_ci")));
    }

    /** Indica si el id corresponde a una fila activa de la tabla {@code usuario}. */
    public boolean existeUsuarioActivo(Long usuarioId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", usuarioId),
                Integer.class);
        return n != null && n > 0;
    }

    /** Busca el id en {@code usuario} por el nombre de cuenta, que en este ERP es el correo. */
    public Optional<Long> usuarioActivoPorEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return jdbc.queryForList(
                        "SELECT id FROM usuario WHERE LOWER(email) = LOWER(:email) AND activo = TRUE",
                        new MapSqlParameterSource("email", email.trim()),
                        Long.class)
                .stream()
                .findFirst();
    }

    private String filtros(
            String search, String estado, Long proveedorId, LocalDate hoy, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where.append(" AND (LOWER(pv.razon_social) LIKE :term ESCAPE '!'"
                    + " OR LOWER(pv.nit_ci) LIKE :term ESCAPE '!'"
                    + " OR LOWER(co.numero_factura) LIKE :term ESCAPE '!')");
        }
        if (estado != null && !estado.isBlank()) {
            String valor = estado.trim().toUpperCase(Locale.ROOT);
            if (CuentaPorPagar.VENCIDA.equals(valor)) {
                params.addValue("hoy", Date.valueOf(hoy));
                where.append(" AND").append(CON_SALDO).append(" AND cx.fecha_vencimiento < :hoy");
            } else {
                params.addValue("estado", valor);
                where.append(" AND UPPER(cx.estado) = :estado");
            }
        }
        if (proveedorId != null) {
            params.addValue("proveedorId", proveedorId);
            where.append(" AND cx.proveedor_id = :proveedorId");
        }
        return where.toString();
    }

    private static LocalDate fecha(Date valor) {
        return valor == null ? null : valor.toLocalDate();
    }

    private String likeTerm(String search) {
        return "%"
                + search.trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                + "%";
    }
}
