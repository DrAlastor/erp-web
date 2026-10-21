package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository;

import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.TipoDato;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ColumnaReporte;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz.*;

/**
 * Lecturas del CU39 sobre las tablas operativas (ventas, inventario, kardex, cuentas por
 * cobrar y compras). Es de solo lectura y no tiene tabla propia.
 *
 * <p>Previene la inyección SQL por construcción: cada fragmento de SQL (tablas, columnas,
 * agrupaciones y medidas) es una constante de esta clase elegida por un enum, y todo valor
 * que viene del comando de voz (fechas, sucursal, producto, límite) viaja como parámetro.
 */
@Repository
@RequiredArgsConstructor
public class ReporteVozRepository {

    /** Sucursal o producto con un alias extra para reconocerlo en la frase (ciudad o código). */
    public record Entidad(Long id, String nombre, String alias) {}

    /** Filtros ya validados que se aplican a la consulta. */
    public record Filtros(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Long sucursalId,
            Long productoId,
            boolean soloCriticos,
            int limite) {}

    /** Columnas, filas y claves de las medidas que tiene sentido totalizar. */
    public record Resultado(
            List<ColumnaReporte> columnas,
            List<Map<String, Object>> filas,
            Set<String> acumulables) {}

    private record Medida(String alias, String etiqueta, String sql, TipoDato tipo, boolean acumulable) {}

    private record Definicion(
            String from,
            String condicionBase,
            String columnaFecha,
            String columnaSucursal,
            String columnaProducto,
            String condicionCriticos,
            Map<AgrupacionVoz, String> grupos,
            List<Medida> medidas) {}

    private static final String SUCURSAL_NOMBRE = "COALESCE(s.nombre, 'Sin sucursal')";
    private static final String PRODUCTO_NOMBRE = "COALESCE(p.nombre, 'Sin producto')";
    private static final String CATEGORIA_NOMBRE = "COALESCE(cat.nombre, 'Sin categoría')";

    private static final Map<MetricaVoz, Definicion> DEFINICIONES = new EnumMap<>(MetricaVoz.class);

    static {
        DEFINICIONES.put(MetricaVoz.VENTAS, new Definicion(
                """
                ventas v
                LEFT JOIN sucursal s ON s.id = v.sucursal_id
                LEFT JOIN clientes c ON c.id = v.cliente_id
                LEFT JOIN usuario u ON u.id = v.cajero_id
                LEFT JOIN empleados e ON e.id = u.empleado_id
                """,
                "UPPER(v.estado) <> 'ANULADA'",
                "v.fecha", "v.sucursal_id", null, null,
                grupos(
                        DIA, "CAST(v.fecha AS DATE)",
                        MES, "TO_CHAR(v.fecha, 'YYYY-MM')",
                        SUCURSAL, SUCURSAL_NOMBRE,
                        CLIENTE, "COALESCE(c.razon_social, v.razon_social_cliente)",
                        VENDEDOR, "COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email, 'Sin asignar')"),
                List.of(
                        new Medida("monto", "Monto vendido", "COALESCE(SUM(v.total), 0)", TipoDato.MONEDA, true),
                        new Medida("ventas", "N° de ventas", "COUNT(*)", TipoDato.NUMERO, true),
                        new Medida("ticket", "Ticket promedio", "COALESCE(AVG(v.total), 0)", TipoDato.MONEDA, false))));

        DEFINICIONES.put(MetricaVoz.PRODUCTOS_VENDIDOS, new Definicion(
                """
                venta_detalles d
                JOIN ventas v ON v.id = d.venta_id
                LEFT JOIN productos p ON p.id = d.producto_id
                LEFT JOIN categorias cat ON cat.id = p.categoria_id
                LEFT JOIN sucursal s ON s.id = v.sucursal_id
                """,
                "UPPER(v.estado) <> 'ANULADA'",
                "v.fecha", "v.sucursal_id", "d.producto_id", null,
                grupos(
                        PRODUCTO, PRODUCTO_NOMBRE,
                        CATEGORIA, CATEGORIA_NOMBRE,
                        SUCURSAL, SUCURSAL_NOMBRE,
                        DIA, "CAST(v.fecha AS DATE)",
                        MES, "TO_CHAR(v.fecha, 'YYYY-MM')"),
                List.of(
                        new Medida("unidades", "Unidades vendidas", "COALESCE(SUM(d.cantidad), 0)", TipoDato.NUMERO, true),
                        new Medida("importe", "Importe vendido", "COALESCE(SUM(d.subtotal), 0)", TipoDato.MONEDA, true))));

        DEFINICIONES.put(MetricaVoz.STOCK, new Definicion(
                """
                inventario_sucursal i
                JOIN productos p ON p.id = i.producto_id
                LEFT JOIN categorias cat ON cat.id = p.categoria_id
                LEFT JOIN sucursal s ON s.id = i.sucursal_id
                """,
                "1 = 1",
                null, "i.sucursal_id", "i.producto_id",
                "i.stock_disponible <= i.stock_minimo",
                grupos(
                        PRODUCTO, PRODUCTO_NOMBRE,
                        CATEGORIA, CATEGORIA_NOMBRE,
                        SUCURSAL, SUCURSAL_NOMBRE),
                List.of(
                        new Medida("stock", "Stock disponible", "COALESCE(SUM(i.stock_disponible), 0)", TipoDato.NUMERO, true),
                        new Medida("minimo", "Stock mínimo", "COALESCE(SUM(i.stock_minimo), 0)", TipoDato.NUMERO, true),
                        new Medida("valor", "Valor a costo", "COALESCE(SUM(i.stock_disponible * p.costo_compra), 0)", TipoDato.MONEDA, true))));

        DEFINICIONES.put(MetricaVoz.MOVIMIENTOS_KARDEX, new Definicion(
                """
                kardex_movimientos k
                LEFT JOIN productos p ON p.id = k.producto_id
                LEFT JOIN sucursal s ON s.id = k.sucursal_id
                """,
                "1 = 1",
                "k.fecha_movimiento", "k.sucursal_id", "k.producto_id", null,
                grupos(
                        TIPO, "k.tipo_movimiento",
                        PRODUCTO, PRODUCTO_NOMBRE,
                        SUCURSAL, SUCURSAL_NOMBRE,
                        DIA, "CAST(k.fecha_movimiento AS DATE)",
                        MES, "TO_CHAR(k.fecha_movimiento, 'YYYY-MM')"),
                List.of(
                        new Medida("movimientos", "N° de movimientos", "COUNT(*)", TipoDato.NUMERO, true),
                        new Medida("cantidad", "Cantidad", "COALESCE(SUM(k.cantidad), 0)", TipoDato.NUMERO, true),
                        new Medida("valor", "Valor", "COALESCE(SUM(k.cantidad * k.costo_unitario), 0)", TipoDato.MONEDA, true))));

        DEFINICIONES.put(MetricaVoz.CUENTAS_POR_COBRAR, new Definicion(
                """
                cuentas_por_cobrar x
                LEFT JOIN clientes c ON c.id = x.cliente_id
                LEFT JOIN sucursal s ON s.id = x.sucursal_id
                """,
                "1 = 1",
                "x.fecha_vencimiento", "x.sucursal_id", null,
                "x.saldo > 0 AND x.fecha_vencimiento < CURRENT_DATE",
                grupos(
                        CLIENTE, "COALESCE(c.razon_social, 'Sin cliente')",
                        ESTADO, "x.estado",
                        SUCURSAL, SUCURSAL_NOMBRE,
                        MES, "TO_CHAR(x.fecha_vencimiento, 'YYYY-MM')"),
                List.of(
                        new Medida("saldo", "Saldo pendiente", "COALESCE(SUM(x.saldo), 0)", TipoDato.MONEDA, true),
                        new Medida("cobrado", "Monto cobrado", "COALESCE(SUM(x.monto_cobrado), 0)", TipoDato.MONEDA, true),
                        new Medida("cuentas", "N° de cuentas", "COUNT(*)", TipoDato.NUMERO, true))));

        DEFINICIONES.put(MetricaVoz.COMPRAS, new Definicion(
                """
                compras co
                LEFT JOIN proveedores pr ON pr.id = co.proveedor_id
                LEFT JOIN sucursal s ON s.id = co.sucursal_id
                """,
                "UPPER(co.estado) <> 'ANULADA'",
                "co.fecha", "co.sucursal_id", null, null,
                grupos(
                        PROVEEDOR, "COALESCE(pr.razon_social, 'Sin proveedor')",
                        SUCURSAL, SUCURSAL_NOMBRE,
                        DIA, "co.fecha",
                        MES, "TO_CHAR(co.fecha, 'YYYY-MM')"),
                List.of(
                        new Medida("monto", "Monto comprado", "COALESCE(SUM(co.total), 0)", TipoDato.MONEDA, true),
                        new Medida("compras", "N° de compras", "COUNT(*)", TipoDato.NUMERO, true))));
    }

    private final NamedParameterJdbcTemplate jdbc;

    /** Ejecuta la consulta de la métrica con la agrupación y los filtros ya validados. */
    public Resultado ejecutar(MetricaVoz metrica, AgrupacionVoz agrupacion, Filtros filtros) {
        Definicion def = DEFINICIONES.get(metrica);
        boolean agrupado = agrupacion != NINGUNA;
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuilder sql = new StringBuilder("SELECT ");
        if (agrupado) {
            sql.append(def.grupos().get(agrupacion)).append(" AS grupo, ");
        }
        sql.append(String.join(", ", def.medidas().stream().map(m -> m.sql() + " AS " + m.alias()).toList()));
        sql.append(" FROM ").append(def.from()).append(" WHERE ").append(def.condicionBase());

        if (def.columnaFecha() != null && filtros.fechaInicio() != null) {
            sql.append(" AND ").append(def.columnaFecha()).append(" >= :desde");
            params.addValue("desde", Date.valueOf(filtros.fechaInicio()));
        }
        if (def.columnaFecha() != null && filtros.fechaFin() != null) {
            sql.append(" AND ").append(def.columnaFecha()).append(" < :hasta");
            params.addValue("hasta", Date.valueOf(filtros.fechaFin().plusDays(1)));
        }
        if (filtros.sucursalId() != null) {
            sql.append(" AND ").append(def.columnaSucursal()).append(" = :sucursalId");
            params.addValue("sucursalId", filtros.sucursalId());
        }
        if (def.columnaProducto() != null && filtros.productoId() != null) {
            sql.append(" AND ").append(def.columnaProducto()).append(" = :productoId");
            params.addValue("productoId", filtros.productoId());
        }
        if (def.condicionCriticos() != null && filtros.soloCriticos()) {
            sql.append(" AND ").append(def.condicionCriticos());
        }

        if (agrupado) {
            // Las series de tiempo van en orden cronológico; el resto, de mayor a menor.
            sql.append(" GROUP BY 1 ORDER BY ").append(agrupacion.temporal() ? "1 ASC" : "2 DESC, 1 ASC");
            sql.append(" LIMIT :limite");
            params.addValue("limite", filtros.limite());
        }

        List<ColumnaReporte> columnas = new ArrayList<>();
        if (agrupado) {
            columnas.add(new ColumnaReporte("grupo", agrupacion.etiqueta(),
                    agrupacion == DIA ? TipoDato.FECHA : TipoDato.TEXTO));
        }
        Set<String> acumulables = new LinkedHashSet<>();
        for (Medida m : def.medidas()) {
            columnas.add(new ColumnaReporte(m.alias(), m.etiqueta(), m.tipo()));
            if (m.acumulable()) {
                acumulables.add(m.alias());
            }
        }

        List<Map<String, Object>> filas = jdbc.queryForList(sql.toString(), params).stream()
                .map(ReporteVozRepository::normalizarFila)
                .toList();
        return new Resultado(columnas, filas, acumulables);
    }

    public List<Entidad> sucursalesActivas() {
        return jdbc.query(
                "SELECT s.id, s.nombre, c.nombre AS ciudad FROM sucursal s "
                        + "LEFT JOIN ciudad c ON c.id = s.ciudad_id WHERE s.activo = TRUE ORDER BY s.nombre",
                new MapSqlParameterSource(),
                (rs, n) -> new Entidad(rs.getLong("id"), rs.getString("nombre"), rs.getString("ciudad")));
    }

    public List<Entidad> productosActivos(int limite) {
        return jdbc.query(
                "SELECT id, nombre, codigo FROM productos WHERE activo = TRUE ORDER BY nombre LIMIT :limite",
                new MapSqlParameterSource("limite", limite),
                (rs, n) -> new Entidad(rs.getLong("id"), rs.getString("nombre"), rs.getString("codigo")));
    }

    public Optional<String> nombreSucursal(Long id) {
        return jdbc.queryForList("SELECT nombre FROM sucursal WHERE id = :id",
                        new MapSqlParameterSource("id", id), String.class)
                .stream().findFirst();
    }

    public Optional<String> nombreProducto(Long id) {
        return jdbc.queryForList("SELECT nombre FROM productos WHERE id = :id",
                        new MapSqlParameterSource("id", id), String.class)
                .stream().findFirst();
    }

    /** Fechas a {@link LocalDate} y montos a dos decimales para que el JSON sea uniforme. */
    private static Map<String, Object> normalizarFila(Map<String, Object> fila) {
        Map<String, Object> limpia = new LinkedHashMap<>();
        fila.forEach((clave, valor) -> {
            Object v = valor;
            if (valor instanceof Date d) {
                v = d.toLocalDate();
            } else if (valor instanceof Timestamp t) {
                v = t.toLocalDateTime().toLocalDate();
            } else if (valor instanceof BigDecimal b) {
                v = b.setScale(2, RoundingMode.HALF_UP);
            } else if (valor instanceof Number n && !(valor instanceof Long) && !(valor instanceof Integer)) {
                v = BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
            limpia.put(clave.toLowerCase(), v);
        });
        return limpia;
    }

    private static Map<AgrupacionVoz, String> grupos(Object... pares) {
        Map<AgrupacionVoz, String> mapa = new EnumMap<>(AgrupacionVoz.class);
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((AgrupacionVoz) pares[i], (String) pares[i + 1]);
        }
        return mapa;
    }
}
