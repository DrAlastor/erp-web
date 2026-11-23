package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.MlClienteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CU41 - Catalogo unico de herramientas del agente: declaraciones para Gemini, validacion y
 * ejecucion segura. Es la pieza de seguridad del agente:
 * <ul>
 *   <li>Rechaza parametros no declarados (la IA no puede mandar ids de empresa ni nada extra).</li>
 *   <li>Valida tipos, enums, fechas reales, minimos/maximos y textos de hasta 100 caracteres.</li>
 *   <li>Ejecuta en una transaccion READ ONLY con {@code statement_timeout} de 10 s.</li>
 *   <li>Quita las columnas privadas y corta en 200 filas lo que se envia a Google.</li>
 *   <li>Nunca lanza: los errores vuelven como resultado para que Gemini corrija o lo diga.</li>
 * </ul>
 */
@Component
public class RegistroHerramientas {

    public static final int MAX_FILAS_MODELO = 200;
    public static final int MAX_TEXTO = 100;
    private static final Logger log = LoggerFactory.getLogger(RegistroHerramientas.class);

    /** Resultado de ejecutar una herramienta: para la pantalla y para Gemini. */
    public record Ejecucion(String nombre, Map<String, Object> parametros, boolean ok, String error,
                            ResultadoHerramienta resultado, Visual grafico, ObjectNode paraModelo) {
    }

    private final Map<String, Herramienta> catalogo = new TreeMap<>();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate soloLectura;
    private final MlClienteService ml;
    private final ObjectMapper json;

    public RegistroHerramientas(List<ProveedorHerramientas> proveedores, JdbcTemplate jdbc,
                                PlatformTransactionManager transacciones, MlClienteService ml, ObjectMapper json) {
        for (ProveedorHerramientas p : proveedores) {
            for (Herramienta h : p.herramientas()) {
                if (catalogo.put(h.nombre(), h) != null) {
                    throw new IllegalStateException("Herramienta duplicada: " + h.nombre());
                }
            }
        }
        this.jdbc = jdbc;
        this.soloLectura = new TransactionTemplate(transacciones);
        this.soloLectura.setReadOnly(true);
        this.soloLectura.setTimeout(15);
        this.ml = ml;
        this.json = json;
    }

    public Collection<Herramienta> todas() {
        return catalogo.values();
    }

    public boolean existe(String nombre) {
        return catalogo.containsKey(nombre);
    }

    // ------------------------------------------------------------------ declaraciones para Gemini

    public ArrayNode declaraciones() {
        ArrayNode lista = json.createArrayNode();
        for (Herramienta h : catalogo.values()) {
            ObjectNode d = lista.addObject();
            d.put("name", h.nombre());
            d.put("description", h.descripcion());
            if (h.parametros().isEmpty()) continue;
            ObjectNode params = d.putObject("parameters");
            params.put("type", "OBJECT");
            ObjectNode props = params.putObject("properties");
            new TreeMap<>(h.parametros()).forEach((nombre, p) -> {
                ObjectNode prop = props.putObject(nombre);
                prop.put("type", p.tipo().name());
                prop.put("description", p.descripcion());
                if (p.enumeracion() != null) {
                    ArrayNode e = prop.putArray("enum");
                    p.enumeracion().forEach(e::add);
                }
                if (p.minimo() != null) prop.put("minimum", p.minimo());
                if (p.maximo() != null) prop.put("maximum", p.maximo());
            });
            if (!h.requeridos().isEmpty()) {
                ArrayNode r = params.putArray("required");
                h.requeridos().forEach(r::add);
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------ validacion

    Map<String, Object> validar(Herramienta h, JsonNode args) {
        Map<String, Object> valores = new LinkedHashMap<>();
        JsonNode a = args == null || args.isNull() || args.isMissingNode() ? json.createObjectNode() : args;
        if (!a.isObject()) throw new ErrorDeParametros("Los argumentos deben ser un objeto.");
        for (String requerido : h.requeridos()) {
            if (!a.hasNonNull(requerido)) throw new ErrorDeParametros("Falta el parametro obligatorio '" + requerido + "'.");
        }
        Iterator<Map.Entry<String, JsonNode>> campos = a.fields();
        while (campos.hasNext()) {
            Map.Entry<String, JsonNode> campo = campos.next();
            String nombre = campo.getKey();
            JsonNode v = campo.getValue();
            Parametro p = h.parametros().get(nombre);
            if (p == null) {
                throw new ErrorDeParametros("El parametro '" + nombre + "' no existe en " + h.nombre()
                        + ". Parametros validos: " + h.parametros().keySet());
            }
            if (v == null || v.isNull()) continue;
            valores.put(nombre, convertir(nombre, p, v));
        }
        return valores;
    }

    private Object convertir(String nombre, Parametro p, JsonNode v) {
        switch (p.tipo()) {
            case STRING -> {
                if (!v.isTextual()) throw new ErrorDeParametros("'" + nombre + "' debe ser texto.");
                String t = v.asText().trim();
                if (t.length() > MAX_TEXTO) throw new ErrorDeParametros("'" + nombre + "' supera " + MAX_TEXTO + " caracteres.");
                if (t.isEmpty()) return null;
                if (p.enumeracion() != null && !p.enumeracion().contains(t)) {
                    throw new ErrorDeParametros("'" + nombre + "' debe ser uno de " + p.enumeracion() + ".");
                }
                if (p.esFecha()) {
                    try {
                        LocalDate.parse(t);
                    } catch (DateTimeParseException e) {
                        throw new ErrorDeParametros("'" + nombre + "' debe ser una fecha real AAAA-MM-DD.");
                    }
                }
                return t;
            }
            case INTEGER -> {
                if (!v.isNumber() || (v.isFloatingPointNumber() && v.asDouble() != Math.rint(v.asDouble()))) {
                    throw new ErrorDeParametros("'" + nombre + "' debe ser un numero entero.");
                }
                rango(nombre, p, v.asDouble());
                return v.asInt();
            }
            case NUMBER -> {
                if (!v.isNumber()) throw new ErrorDeParametros("'" + nombre + "' debe ser un numero.");
                rango(nombre, p, v.asDouble());
                return v.asDouble();
            }
            case BOOLEAN -> {
                if (!v.isBoolean()) throw new ErrorDeParametros("'" + nombre + "' debe ser true o false.");
                return v.asBoolean();
            }
            default -> throw new ErrorDeParametros("Tipo no soportado.");
        }
    }

    private static void rango(String nombre, Parametro p, double n) {
        if ((p.minimo() != null && n < p.minimo()) || (p.maximo() != null && n > p.maximo())) {
            throw new ErrorDeParametros("'" + nombre + "' debe estar entre " + p.minimo().intValue()
                    + " y " + p.maximo().intValue() + ".");
        }
    }

    // ------------------------------------------------------------------ ejecucion

    public Ejecucion ejecutar(String nombre, JsonNode args, TenantDescriptor empresa, LocalDate hoy) {
        Herramienta h = catalogo.get(nombre);
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (h == null) {
            return fallo(nombre, parametros, "La herramienta '" + nombre + "' no existe.");
        }
        try {
            parametros = validar(h, args);
            Argumentos argumentos = new Argumentos(parametros);
            ContextoHerramienta contexto = new ContextoHerramienta(empresa, hoy, this::consultar, ml);
            ResultadoHerramienta r = soloLectura.execute(estado -> {
                jdbc.execute("SET TRANSACTION READ ONLY");
                jdbc.execute("SET LOCAL statement_timeout = 10000");
                return h.ejecutor().ejecutar(argumentos, contexto);
            });
            Visual v = h.visual().apply(argumentos);
            Visual grafico = v != null && r.filas().size() >= v.minimoFilas() ? v : null;
            return new Ejecucion(nombre, parametros, true, null, r, grafico, paraModelo(r));
        } catch (ErrorDeParametros e) {
            return fallo(nombre, parametros, e.getMessage());
        } catch (Exception e) {
            log.warn("CU41: fallo la herramienta {} con {}: {}", nombre, parametros, e.toString());
            return fallo(nombre, parametros, "No se pudo obtener el dato por un error interno.");
        }
    }

    private Ejecucion fallo(String nombre, Map<String, Object> parametros, String error) {
        ObjectNode m = json.createObjectNode();
        m.put("error", error);
        return new Ejecucion(nombre, parametros, false, error, null, null, m);
    }

    /** Lo que se envia a Gemini: sin columnas privadas y como maximo 200 filas. */
    ObjectNode paraModelo(ResultadoHerramienta r) {
        ObjectNode m = json.createObjectNode();
        ArrayNode filas = m.putArray("filas");
        List<Map<String, Object>> datos = r.filas();
        int n = Math.min(datos.size(), MAX_FILAS_MODELO);
        for (int i = 0; i < n; i++) {
            Map<String, Object> fila = new LinkedHashMap<>(datos.get(i));
            r.columnasPrivadas().forEach(fila::remove);
            filas.add(json.valueToTree(fila));
        }
        m.put("total_filas", datos.size());
        if (datos.size() > MAX_FILAS_MODELO) {
            m.put("aviso", "Se muestran las primeras " + MAX_FILAS_MODELO + " de " + datos.size() + " filas.");
        }
        if (r.resumen() != null && !r.resumen().isEmpty()) m.set("resumen", json.valueToTree(r.resumen()));
        if (r.nota() != null) m.put("nota", r.nota());
        if (r.disponible() != null) m.put("disponible", r.disponible());
        return m;
    }

    List<Map<String, Object>> consultar(Consulta q) {
        return jdbc.query(q.texto(), (ResultSet rs) -> {
            List<Map<String, Object>> filas = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    fila.put(meta.getColumnLabel(i), valor(rs, i));
                }
                filas.add(fila);
            }
            return filas;
        }, q.parametros());
    }

    private static Object valor(ResultSet rs, int i) throws SQLException {
        Object v = rs.getObject(i);
        if (v instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (v instanceof Timestamp t) return t.toLocalDateTime().toString();
        if (v == null || v instanceof Number || v instanceof String || v instanceof Boolean) return v;
        return v.toString(); // jsonb, uuid, etc. (PGobject.toString() devuelve su valor)
    }
}
