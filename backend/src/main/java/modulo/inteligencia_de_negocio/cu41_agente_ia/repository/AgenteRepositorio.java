package modulo.inteligencia_de_negocio.cu41_agente_ia.repository;

import comun.multitenancy.TenantContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CU41 - Conversaciones, mensajes y graficos guardados del agente, en la base de la empresa del
 * usuario. Toda lectura filtra por {@code usuario_id}: nadie ve conversaciones ni graficos ajenos.
 */
@Repository
public class AgenteRepositorio {

    public record Conversacion(long id, String titulo, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
    }

    public record Mensaje(long id, String rol, String contenido, String herramientasJson,
                          Integer tokensEntrada, Integer tokensSalida, LocalDateTime fecha) {
    }

    public record Grafico(long id, String titulo, String herramienta, String parametrosJson, LocalDateTime fechaCreacion) {
    }

    private final JdbcTemplate jdbc;
    private final List<String> ddl;
    private final Set<String> empresasListas = ConcurrentHashMap.newKeySet();

    public AgenteRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.ddl = cargarDdl();
    }

    private static List<String> cargarDdl() {
        try {
            String texto = new ClassPathResource("db/tenant/agente_ia.sql").getContentAsString(StandardCharsets.UTF_8);
            String sinComentarios = Arrays.stream(texto.split("\n"))
                    .filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n");
            return Arrays.stream(sinComentarios.split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db/tenant/agente_ia.sql", e);
        }
    }

    /** Crea las tablas del agente en la base de la empresa actual (una vez por empresa y proceso). */
    public void asegurarEsquema() {
        String empresa = TenantContext.current().map(t -> t.codigoEmpresa()).orElse("_bootstrap");
        if (empresasListas.contains(empresa)) return;
        ddl.forEach(jdbc::execute);
        empresasListas.add(empresa);
    }

    public boolean hayVentas() {
        Boolean r = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM ventas WHERE UPPER(estado) <> 'ANULADA')", Boolean.class);
        return Boolean.TRUE.equals(r);
    }

    /** Preguntas hechas en la empresa (todas las cuentas) en las ultimas 24 h: cuota diaria. */
    public long preguntasUltimas24h() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM bi_mensaje WHERE rol = 'USUARIO'"
                + " AND fecha > CURRENT_TIMESTAMP - INTERVAL '24 hours'", Long.class);
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------ conversaciones

    public Optional<Conversacion> conversacion(long id, long usuarioId) {
        return jdbc.query("SELECT id, titulo, fecha_creacion, fecha_actualizacion FROM bi_conversacion"
                        + " WHERE id = ? AND usuario_id = ?",
                (rs, n) -> new Conversacion(rs.getLong(1), rs.getString(2), local(rs.getTimestamp(3)), local(rs.getTimestamp(4))),
                id, usuarioId).stream().findFirst();
    }

    public List<Conversacion> conversaciones(long usuarioId, int limite) {
        return jdbc.query("SELECT id, titulo, fecha_creacion, fecha_actualizacion FROM bi_conversacion"
                        + " WHERE usuario_id = ? ORDER BY fecha_actualizacion DESC, id DESC LIMIT ?",
                (rs, n) -> new Conversacion(rs.getLong(1), rs.getString(2), local(rs.getTimestamp(3)), local(rs.getTimestamp(4))),
                usuarioId, limite);
    }

    public long crearConversacion(long usuarioId, String titulo) {
        Long id = jdbc.queryForObject("INSERT INTO bi_conversacion (usuario_id, titulo) VALUES (?, ?) RETURNING id",
                Long.class, usuarioId, titulo);
        return id == null ? 0 : id;
    }

    public void tocarConversacion(long id) {
        jdbc.update("UPDATE bi_conversacion SET fecha_actualizacion = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    public boolean borrarConversacion(long id, long usuarioId) {
        return jdbc.update("DELETE FROM bi_conversacion WHERE id = ? AND usuario_id = ?", id, usuarioId) > 0;
    }

    public void guardarMensaje(long conversacionId, String rol, String contenido, String herramientasJson,
                               Integer tokensEntrada, Integer tokensSalida) {
        jdbc.update("INSERT INTO bi_mensaje (conversacion_id, rol, contenido, herramientas, tokens_entrada, tokens_salida)"
                        + " VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?)",
                conversacionId, rol, contenido, herramientasJson, tokensEntrada, tokensSalida);
    }

    /** Mensajes en orden cronologico; con {@code ultimos} > 0 solo los ultimos N. */
    public List<Mensaje> mensajes(long conversacionId, int ultimos) {
        String sql = "SELECT id, rol, contenido, CAST(herramientas AS TEXT), tokens_entrada, tokens_salida, fecha"
                + " FROM bi_mensaje WHERE conversacion_id = ?";
        List<Mensaje> lista = jdbc.query(sql + (ultimos > 0 ? " ORDER BY id DESC LIMIT " + ultimos : " ORDER BY id"),
                (rs, n) -> new Mensaje(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        (Integer) rs.getObject(5), (Integer) rs.getObject(6), local(rs.getTimestamp(7))),
                conversacionId);
        if (ultimos > 0) {
            lista = new java.util.ArrayList<>(lista);
            java.util.Collections.reverse(lista);
        }
        return lista;
    }

    // ------------------------------------------------------------------ graficos guardados

    public long contarGraficos(long usuarioId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM bi_grafico WHERE usuario_id = ?", Long.class, usuarioId);
        return n == null ? 0 : n;
    }

    public long guardarGrafico(long usuarioId, String titulo, String herramienta, String parametrosJson) {
        Long id = jdbc.queryForObject("INSERT INTO bi_grafico (usuario_id, titulo, herramienta, parametros)"
                + " VALUES (?, ?, ?, CAST(? AS jsonb)) RETURNING id", Long.class, usuarioId, titulo, herramienta, parametrosJson);
        return id == null ? 0 : id;
    }

    public List<Grafico> graficos(long usuarioId) {
        return jdbc.query("SELECT id, titulo, herramienta, CAST(parametros AS TEXT), fecha_creacion FROM bi_grafico"
                        + " WHERE usuario_id = ? ORDER BY fecha_creacion DESC, id DESC",
                (rs, n) -> new Grafico(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), local(rs.getTimestamp(5))),
                usuarioId);
    }

    public Optional<Grafico> grafico(long id, long usuarioId) {
        return jdbc.query("SELECT id, titulo, herramienta, CAST(parametros AS TEXT), fecha_creacion FROM bi_grafico"
                        + " WHERE id = ? AND usuario_id = ?",
                (rs, n) -> new Grafico(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), local(rs.getTimestamp(5))),
                id, usuarioId).stream().findFirst();
    }

    public boolean borrarGrafico(long id, long usuarioId) {
        return jdbc.update("DELETE FROM bi_grafico WHERE id = ? AND usuario_id = ?", id, usuarioId) > 0;
    }

    private static LocalDateTime local(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
