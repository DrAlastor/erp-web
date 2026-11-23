package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.repository;

import lombok.RequiredArgsConstructor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.Anomalia;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.ClienteSegmento;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.Ejecucion;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.Opcion;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.PuntoPronostico;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.ReglaAsociacion;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.Segmento;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.SugerenciaCompra;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CU40 - Lecturas de solo lectura sobre las tablas {@code ml_*} de la base de la empresa
 * (database-per-tenant: el {@link NamedParameterJdbcTemplate} ya apunta a la base del usuario).
 *
 * <p>Las tablas las crea el servicio de ML en su primer entrenamiento; mientras no existan,
 * {@link #existenResultados()} es false y el servicio no consulta el resto.
 */
@Repository
@RequiredArgsConstructor
public class ResultadosMlRepository {

    /** Misma regla de venta valida y categoria que usa el servicio de ML (ml-service/app/datos.py). */
    private static final String VENTA_VALIDA = "UPPER(v.estado) <> 'ANULADA'";
    private static final String CATEGORIA = "COALESCE(NULLIF(TRIM(c.nombre), ''), 'Sin categoria')";

    private final NamedParameterJdbcTemplate jdbc;

    public boolean existenResultados() {
        Boolean existe = jdbc.queryForObject(
                "SELECT to_regclass('public.ml_ejecucion') IS NOT NULL", new MapSqlParameterSource(), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public record EjecucionConMetricas(Ejecucion ejecucion, String metricasJson) {
    }

    /** Ultima ejecucion terminada o en curso. */
    public Optional<EjecucionConMetricas> ultimaEjecucion() {
        return jdbc.query(
                "SELECT id, estado, inicio, fin, error, CAST(metricas AS TEXT) AS metricas"
                        + " FROM ml_ejecucion ORDER BY inicio DESC, id DESC LIMIT 1",
                new MapSqlParameterSource(),
                (rs, n) -> new EjecucionConMetricas(
                        new Ejecucion(
                                rs.getLong("id"),
                                rs.getString("estado"),
                                toLocal(rs.getTimestamp("inicio")),
                                toLocal(rs.getTimestamp("fin")),
                                rs.getString("error")),
                        rs.getString("metricas"))).stream().findFirst();
    }

    /** Ultima ejecucion que termino con resultados (OK o PARCIAL): sus metricas describen las tablas. */
    public Optional<String> metricasVigentes() {
        return jdbc.query(
                "SELECT CAST(metricas AS TEXT) FROM ml_ejecucion WHERE estado IN ('OK', 'PARCIAL')"
                        + " ORDER BY inicio DESC, id DESC LIMIT 1",
                new MapSqlParameterSource(),
                (rs, n) -> rs.getString(1)).stream().findFirst();
    }

    // ------------------------------------------------------------------ pronostico

    public List<PuntoPronostico> pronostico(String ambito, String clave, String frecuencia, int limite) {
        return jdbc.query(
                "SELECT periodo, valor, bajo, alto FROM ml_pronostico"
                        + " WHERE ambito = :ambito AND clave = :clave AND frecuencia = :frecuencia"
                        + " ORDER BY periodo LIMIT :limite",
                new MapSqlParameterSource("ambito", ambito).addValue("clave", clave)
                        .addValue("frecuencia", frecuencia).addValue("limite", limite),
                (rs, n) -> new PuntoPronostico(
                        rs.getDate("periodo").toLocalDate(),
                        rs.getBigDecimal("valor"),
                        rs.getBigDecimal("bajo"),
                        rs.getBigDecimal("alto")));
    }

    public Optional<String> modeloDeSerie(String ambito, String clave, String frecuencia) {
        return jdbc.query(
                "SELECT modelo FROM ml_pronostico WHERE ambito = :ambito AND clave = :clave"
                        + " AND frecuencia = :frecuencia LIMIT 1",
                new MapSqlParameterSource("ambito", ambito).addValue("clave", clave).addValue("frecuencia", frecuencia),
                (rs, n) -> rs.getString(1)).stream().findFirst();
    }

    /**
     * Ventas reales (suma de lineas) por periodo en [desde, hasta). Mensual = primer dia del
     * mes, semanal = lunes (ISO), igual que el servicio de ML. Los periodos sin ventas no vienen.
     */
    public Map<LocalDate, BigDecimal> historiaVentas(String frecuencia, LocalDate desde, LocalDate hasta,
                                                     Long sucursalId, String categoria) {
        String unidad = "SEMANAL".equals(frecuencia) ? "week" : "month";
        MapSqlParameterSource p = new MapSqlParameterSource("desde", Date.valueOf(desde))
                .addValue("hasta", Date.valueOf(hasta));
        StringBuilder sql = new StringBuilder(
                "SELECT CAST(date_trunc('" + unidad + "', v.fecha) AS DATE) AS periodo, SUM(d.subtotal) AS total"
                        + " FROM ventas v JOIN venta_detalles d ON d.venta_id = v.id"
                        + " LEFT JOIN productos p ON p.id = d.producto_id"
                        + " LEFT JOIN categorias c ON c.id = p.categoria_id"
                        + " WHERE " + VENTA_VALIDA
                        + " AND v.fecha >= :desde AND v.fecha < :hasta");
        if (sucursalId != null) {
            sql.append(" AND v.sucursal_id = :sucursalId");
            p.addValue("sucursalId", sucursalId);
        }
        if (categoria != null) {
            sql.append(" AND ").append(CATEGORIA).append(" = :categoria");
            p.addValue("categoria", categoria);
        }
        sql.append(" GROUP BY 1 ORDER BY 1");
        Map<LocalDate, BigDecimal> resultado = new LinkedHashMap<>();
        jdbc.query(sql.toString(), p, rs -> {
            resultado.put(rs.getDate("periodo").toLocalDate(), rs.getBigDecimal("total"));
        });
        return resultado;
    }

    // ------------------------------------------------------------------ clientes

    public record FilaRiesgo(long clienteId, String cliente, String nitCi, BigDecimal probabilidad, String nivel,
                             String factoresJson) {
    }

    public List<FilaRiesgo> clientesEnRiesgo(String nivel, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource("limite", limite);
        String filtro = "";
        if (nivel != null) {
            filtro = " WHERE r.nivel = :nivel";
            p.addValue("nivel", nivel);
        }
        return jdbc.query(
                "SELECT r.cliente_id, cl.razon_social, cl.nit_ci, r.probabilidad, r.nivel,"
                        + " CAST(r.factores AS TEXT) AS factores"
                        + " FROM ml_riesgo_abandono r LEFT JOIN clientes cl ON cl.id = r.cliente_id"
                        + filtro
                        + " ORDER BY r.probabilidad DESC, r.cliente_id LIMIT :limite",
                p,
                (rs, n) -> new FilaRiesgo(
                        rs.getLong("cliente_id"),
                        rs.getString("razon_social"),
                        rs.getString("nit_ci"),
                        rs.getBigDecimal("probabilidad"),
                        rs.getString("nivel"),
                        rs.getString("factores")));
    }

    public Map<String, Long> conteoPorNivel() {
        Map<String, Long> conteo = new LinkedHashMap<>();
        conteo.put("ALTO", 0L);
        conteo.put("MEDIO", 0L);
        conteo.put("BAJO", 0L);
        jdbc.query("SELECT nivel, COUNT(*) AS n FROM ml_riesgo_abandono GROUP BY nivel",
                new MapSqlParameterSource(), rs -> {
                    conteo.put(rs.getString("nivel"), rs.getLong("n"));
                });
        return conteo;
    }

    public List<Segmento> segmentos() {
        return jdbc.query(
                "SELECT segmento, COUNT(*) AS clientes, AVG(recencia_dias) AS recencia, AVG(frecuencia) AS frecuencia,"
                        + " ROUND(AVG(monto), 2) AS monto_promedio, ROUND(SUM(monto), 2) AS monto_total"
                        + " FROM ml_segmento_cliente GROUP BY segmento ORDER BY monto_total DESC",
                new MapSqlParameterSource(),
                (rs, n) -> new Segmento(
                        rs.getString("segmento"),
                        rs.getLong("clientes"),
                        rs.getDouble("recencia"),
                        rs.getDouble("frecuencia"),
                        rs.getBigDecimal("monto_promedio"),
                        rs.getBigDecimal("monto_total")));
    }

    public List<ClienteSegmento> clientesDeSegmento(String segmento, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource("limite", limite);
        String filtro = "";
        if (segmento != null) {
            filtro = " WHERE s.segmento = :segmento";
            p.addValue("segmento", segmento);
        }
        return jdbc.query(
                "SELECT s.cliente_id, cl.razon_social, s.segmento, s.recencia_dias, s.frecuencia, s.monto"
                        + " FROM ml_segmento_cliente s LEFT JOIN clientes cl ON cl.id = s.cliente_id"
                        + filtro + " ORDER BY s.monto DESC, s.cliente_id LIMIT :limite",
                p,
                (rs, n) -> new ClienteSegmento(
                        rs.getLong("cliente_id"),
                        rs.getString("razon_social"),
                        rs.getString("segmento"),
                        rs.getInt("recencia_dias"),
                        rs.getInt("frecuencia"),
                        rs.getBigDecimal("monto")));
    }

    // ------------------------------------------------------------------ productos y sucursales

    public List<ReglaAsociacion> reglas(String producto, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource("limite", limite);
        String filtro = "";
        if (producto != null) {
            filtro = " WHERE a.producto_a ILIKE :producto";
            p.addValue("producto", "%" + producto.replace("%", "\\%").replace("_", "\\_") + "%");
        }
        return jdbc.query(
                "SELECT a.producto_a_id, a.producto_a, a.producto_b_id, a.producto_b, a.soporte, a.confianza,"
                        + " a.lift, a.ventas FROM ml_asociacion a" + filtro
                        + " ORDER BY a.lift DESC, a.confianza DESC LIMIT :limite",
                p,
                (rs, n) -> new ReglaAsociacion(
                        rs.getLong("producto_a_id"),
                        rs.getString("producto_a"),
                        rs.getLong("producto_b_id"),
                        rs.getString("producto_b"),
                        rs.getBigDecimal("soporte"),
                        rs.getBigDecimal("confianza"),
                        rs.getBigDecimal("lift"),
                        rs.getInt("ventas")));
    }

    public List<Anomalia> anomalias(LocalDate desde, LocalDate hasta, Long sucursalId, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource("limite", limite);
        StringBuilder filtro = new StringBuilder(" WHERE 1 = 1");
        if (desde != null) {
            filtro.append(" AND a.fecha >= :desde");
            p.addValue("desde", Date.valueOf(desde));
        }
        if (hasta != null) {
            filtro.append(" AND a.fecha <= :hasta");
            p.addValue("hasta", Date.valueOf(hasta));
        }
        if (sucursalId != null) {
            filtro.append(" AND a.sucursal_id = :sucursalId");
            p.addValue("sucursalId", sucursalId);
        }
        return jdbc.query(
                "SELECT a.fecha, a.sucursal_id, s.nombre, a.metrica, a.valor_real, a.valor_esperado, a.desviacion,"
                        + " a.gravedad FROM ml_anomalia a LEFT JOIN sucursal s ON s.id = a.sucursal_id"
                        + filtro + " ORDER BY a.fecha DESC, ABS(a.desviacion) DESC LIMIT :limite",
                p,
                (rs, n) -> new Anomalia(
                        rs.getDate("fecha").toLocalDate(),
                        rs.getLong("sucursal_id"),
                        rs.getString("nombre"),
                        rs.getString("metrica"),
                        rs.getBigDecimal("valor_real"),
                        rs.getBigDecimal("valor_esperado"),
                        rs.getBigDecimal("desviacion"),
                        rs.getString("gravedad")));
    }

    public List<SugerenciaCompra> sugerencias(Long sucursalId, String categoria, int limite) {
        MapSqlParameterSource p = new MapSqlParameterSource("limite", limite);
        StringBuilder filtro = new StringBuilder(" WHERE 1 = 1");
        if (sucursalId != null) {
            filtro.append(" AND g.sucursal_id = :sucursalId");
            p.addValue("sucursalId", sucursalId);
        }
        if (categoria != null) {
            filtro.append(" AND ").append(CATEGORIA).append(" = :categoria");
            p.addValue("categoria", categoria);
        }
        return jdbc.query(
                "SELECT g.producto_id, p.codigo, p.nombre, " + CATEGORIA + " AS categoria, g.sucursal_id,"
                        + " s.nombre AS sucursal, g.demanda_esperada, g.stock_actual, g.stock_seguridad,"
                        + " g.cantidad_sugerida, ROUND(g.cantidad_sugerida * COALESCE(p.costo_compra, 0), 2) AS costo,"
                        + " g.motivo"
                        + " FROM ml_sugerencia_compra g"
                        + " LEFT JOIN productos p ON p.id = g.producto_id"
                        + " LEFT JOIN categorias c ON c.id = p.categoria_id"
                        + " LEFT JOIN sucursal s ON s.id = g.sucursal_id"
                        + filtro + " ORDER BY costo DESC, g.cantidad_sugerida DESC LIMIT :limite",
                p,
                (rs, n) -> new SugerenciaCompra(
                        rs.getLong("producto_id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getLong("sucursal_id"),
                        rs.getString("sucursal"),
                        rs.getBigDecimal("demanda_esperada"),
                        rs.getBigDecimal("stock_actual"),
                        rs.getBigDecimal("stock_seguridad"),
                        rs.getInt("cantidad_sugerida"),
                        rs.getBigDecimal("costo"),
                        rs.getString("motivo")));
    }

    // ------------------------------------------------------------------ opciones

    public List<Opcion> sucursales() {
        return jdbc.query("SELECT id, nombre FROM sucursal WHERE activo = TRUE ORDER BY nombre",
                new MapSqlParameterSource(), (rs, n) -> new Opcion(rs.getLong("id"), rs.getString("nombre")));
    }

    public List<String> categorias() {
        return jdbc.query(
                "SELECT DISTINCT " + CATEGORIA + " AS categoria FROM productos p"
                        + " LEFT JOIN categorias c ON c.id = p.categoria_id WHERE p.activo = TRUE ORDER BY 1",
                new MapSqlParameterSource(), (rs, n) -> rs.getString("categoria"));
    }

    public Optional<String> nombreSucursal(long id) {
        return jdbc.query("SELECT nombre FROM sucursal WHERE id = :id", new MapSqlParameterSource("id", id),
                (rs, n) -> rs.getString(1)).stream().findFirst();
    }

    private static java.time.LocalDateTime toLocal(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
