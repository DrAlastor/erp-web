package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lecturas de solo consulta que necesita el CU16 sobre tablas de otros modulos
 * (clientes, sucursal, productos, inventario_sucursal, usuario y empleados).
 *
 * <p>Se usan consultas SQL propias en lugar de importar entidades ajenas: asi el CU no
 * depende del mapeo JPA de otros modulos y solo lee las columnas que realmente usa.
 */
@Repository
@RequiredArgsConstructor
public class PedidoVentaConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Cabecera de un pedido con los nombres ya resueltos para mostrar. */
    public record PedidoCabecera(
            Long id,
            String numeroPedido,
            LocalDate fecha,
            Long clienteId,
            String clienteRazonSocial,
            String clienteNitCi,
            Long sucursalId,
            String sucursalNombre,
            Long vendedorId,
            String vendedorNombre,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            String estado,
            Integer cantidadItems,
            String observaciones,
            LocalDateTime fechaCreacion) {}

    public record ClienteRef(Long id, String razonSocial, String nitCi, boolean activo) {}

    public record SucursalRef(Long id, String nombre, boolean activo) {}

    public record ProductoRef(
            Long id, String codigo, String nombre, BigDecimal precioVenta, boolean activo) {}

    public record ProductoStock(
            Long id, String codigo, String nombre, BigDecimal precioVenta, BigDecimal stockDisponible) {}

    private static final String SELECT_CABECERA = """
            SELECT p.id, p.numero_pedido, p.fecha, p.cliente_id, c.razon_social, c.nit_ci,
                   p.sucursal_id, s.nombre AS sucursal_nombre, p.vendedor_id,
                   COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email) AS vendedor_nombre,
                   p.subtotal, p.descuento, p.total, p.estado, p.observaciones, p.fecha_creacion,
                   (SELECT COUNT(*) FROM pedido_detalles d WHERE d.pedido_id = p.id) AS cantidad_items
            FROM pedidos p
            LEFT JOIN clientes c ON c.id = p.cliente_id
            LEFT JOIN sucursal s ON s.id = p.sucursal_id
            LEFT JOIN usuario u ON u.id = p.vendedor_id
            LEFT JOIN empleados e ON e.id = u.empleado_id
            """;

    private static final RowMapper<PedidoCabecera> CABECERA = (rs, n) -> {
        Date fecha = rs.getDate("fecha");
        Timestamp creacion = rs.getTimestamp("fecha_creacion");
        return new PedidoCabecera(
                rs.getLong("id"),
                rs.getString("numero_pedido"),
                fecha == null ? null : fecha.toLocalDate(),
                rs.getLong("cliente_id"),
                rs.getString("razon_social"),
                rs.getString("nit_ci"),
                rs.getLong("sucursal_id"),
                rs.getString("sucursal_nombre"),
                rs.getLong("vendedor_id"),
                rs.getString("vendedor_nombre"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("descuento"),
                rs.getBigDecimal("total"),
                rs.getString("estado"),
                rs.getInt("cantidad_items"),
                rs.getString("observaciones"),
                creacion == null ? null : creacion.toLocalDateTime());
    };

    /** Pagina de cabeceras filtrada por texto (numero, razon social o NIT/CI) y estado. */
    public List<PedidoCabecera> listar(String search, String estado, int limit, long offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, params);
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(
                SELECT_CABECERA + where + " ORDER BY p.id DESC LIMIT :limit OFFSET :offset",
                params,
                CABECERA);
    }

    public long contar(String search, String estado) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, params);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pedidos p LEFT JOIN clientes c ON c.id = p.cliente_id" + where,
                params,
                Long.class);
        return total == null ? 0 : total;
    }

    public Optional<PedidoCabecera> cabecera(Long id) {
        return jdbc.query(
                        SELECT_CABECERA + " WHERE p.id = :id",
                        new MapSqlParameterSource("id", id),
                        CABECERA)
                .stream()
                .findFirst();
    }

    public Optional<ClienteRef> cliente(Long id) {
        return jdbc.query(
                        "SELECT id, razon_social, nit_ci, activo FROM clientes WHERE id = :id",
                        new MapSqlParameterSource("id", id),
                        (rs, n) -> new ClienteRef(
                                rs.getLong("id"),
                                rs.getString("razon_social"),
                                rs.getString("nit_ci"),
                                rs.getBoolean("activo")))
                .stream()
                .findFirst();
    }

    public Optional<SucursalRef> sucursal(Long id) {
        return jdbc.query(
                        "SELECT id, nombre, activo FROM sucursal WHERE id = :id",
                        new MapSqlParameterSource("id", id),
                        (rs, n) -> new SucursalRef(
                                rs.getLong("id"), rs.getString("nombre"), rs.getBoolean("activo")))
                .stream()
                .findFirst();
    }

    /** Productos indexados por id; los ids inexistentes simplemente no aparecen. */
    public Map<Long, ProductoRef> productos(Collection<Long> ids) {
        Map<Long, ProductoRef> resultado = new HashMap<>();
        if (ids == null || ids.isEmpty()) return resultado;
        jdbc.query(
                "SELECT id, codigo, nombre, precio_venta, activo FROM productos WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    ProductoRef ref = new ProductoRef(
                            rs.getLong("id"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getBigDecimal("precio_venta"),
                            rs.getBoolean("activo"));
                    resultado.put(ref.id(), ref);
                });
        return resultado;
    }

    /** Stock disponible por producto en la sucursal; sin fila de inventario no hay entrada. */
    public Map<Long, BigDecimal> stock(Long sucursalId, Collection<Long> productoIds) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        if (sucursalId == null || productoIds == null || productoIds.isEmpty()) return resultado;
        jdbc.query(
                "SELECT producto_id, stock_disponible FROM inventario_sucursal "
                        + "WHERE sucursal_id = :sucursalId AND producto_id IN (:ids)",
                new MapSqlParameterSource("sucursalId", sucursalId).addValue("ids", productoIds),
                rs -> {
                    resultado.put(rs.getLong("producto_id"), rs.getBigDecimal("stock_disponible"));
                });
        return resultado;
    }

    public List<ClienteRef> clientesActivos(String search, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = " WHERE activo = TRUE";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where += " AND (LOWER(razon_social) LIKE :term ESCAPE '!' OR LOWER(nit_ci) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                "SELECT id, razon_social, nit_ci, activo FROM clientes" + where
                        + " ORDER BY razon_social LIMIT :limit",
                params,
                (rs, n) -> new ClienteRef(
                        rs.getLong("id"), rs.getString("razon_social"), rs.getString("nit_ci"), true));
    }

    public List<SucursalRef> sucursalesActivas() {
        return jdbc.query(
                "SELECT id, nombre FROM sucursal WHERE activo = TRUE ORDER BY nombre",
                new MapSqlParameterSource(),
                (rs, n) -> new SucursalRef(rs.getLong("id"), rs.getString("nombre"), true));
    }

    public List<ProductoStock> productosActivos(String search, Long sucursalId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit)
                .addValue("sucursalId", sucursalId == null ? -1L : sucursalId);
        String where = " WHERE pr.activo = TRUE";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where += " AND (LOWER(pr.codigo) LIKE :term ESCAPE '!' OR LOWER(pr.nombre) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                "SELECT pr.id, pr.codigo, pr.nombre, pr.precio_venta, "
                        + "COALESCE(i.stock_disponible, 0) AS stock_disponible "
                        + "FROM productos pr "
                        + "LEFT JOIN inventario_sucursal i ON i.producto_id = pr.id AND i.sucursal_id = :sucursalId"
                        + where + " ORDER BY pr.nombre LIMIT :limit",
                params,
                (rs, n) -> new ProductoStock(
                        rs.getLong("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio_venta"),
                        rs.getBigDecimal("stock_disponible")));
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

    private String filtros(String search, String estado, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where.append(" AND (LOWER(p.numero_pedido) LIKE :term ESCAPE '!'"
                    + " OR LOWER(c.razon_social) LIKE :term ESCAPE '!'"
                    + " OR LOWER(c.nit_ci) LIKE :term ESCAPE '!')");
        }
        if (estado != null && !estado.isBlank()) {
            params.addValue("estado", estado.trim().toUpperCase(Locale.ROOT));
            where.append(" AND p.estado = :estado");
        }
        return where.toString();
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
