package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** CU41 - Piezas compartidas por las herramientas: periodo, SQL base de ventas y agrupaciones. */
public final class Comunes {

    private Comunes() {
    }

    public record Periodo(LocalDate desde, LocalDate hasta) {
        public Map<String, Object> comoMapa() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("periodo_desde", desde.toString());
            m.put("periodo_hasta", hasta.toString());
            return m;
        }
    }

    /** Periodo pedido; sin fechas: desde el primer dia del mes de hace 12 meses hasta hoy. */
    public static Periodo periodo(Argumentos a, ContextoHerramienta c) {
        return periodo(a, c, "desde", "hasta");
    }

    public static Periodo periodo(Argumentos a, ContextoHerramienta c, String claveDesde, String claveHasta) {
        LocalDate hasta = a.fecha(claveHasta, c.hoy());
        LocalDate desde = a.fecha(claveDesde, c.hoy().minusMonths(12).withDayOfMonth(1));
        if (desde.isAfter(hasta)) {
            throw new ErrorDeParametros("La fecha '" + claveDesde + "' no puede ser posterior a '" + claveHasta + "'.");
        }
        return new Periodo(desde, hasta);
    }

    /** Importe NETO de la linea: prorratea el descuento de cabecera de la venta. */
    public static final String IMPORTE = "(d.subtotal * COALESCE(v.total / NULLIF(v.subtotal, 0), 1))";
    /** Costo con el costo de compra ACTUAL del producto (no hay costo historico por venta). */
    public static final String COSTO = "(d.cantidad * COALESCE(p.costo_compra, 0))";

    public static final String FROM_VENTAS = " FROM ventas v"
            + " JOIN venta_detalles d ON d.venta_id = v.id"
            + " LEFT JOIN productos p ON p.id = d.producto_id"
            + " LEFT JOIN categorias c ON c.id = p.categoria_id"
            + " LEFT JOIN sucursal s ON s.id = v.sucursal_id";

    /** {@code WHERE} de ventas validas en el periodo, con filtros opcionales de texto. */
    public static Consulta whereVentas(Consulta q, Periodo p, Argumentos a) {
        q.sql(" WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + q.fecha(p.desde())
                + " AND v.fecha < " + q.fecha(p.hasta()) + " + 1");
        q.filtroContiene("s.nombre", a.texto("sucursal"));
        q.filtroContiene("c.nombre", a.texto("categoria"));
        q.filtroContiene("p.nombre", a.texto("producto"));
        return q;
    }

    /** Agrupaciones permitidas (enum) y su expresion SQL fija. */
    public static final Map<String, String> AGRUPACIONES = Map.ofEntries(
            Map.entry("total", "'Total'"),
            Map.entry("dia", "to_char(v.fecha, 'YYYY-MM-DD')"),
            Map.entry("semana", "to_char(date_trunc('week', v.fecha), 'YYYY-MM-DD')"),
            Map.entry("mes", "to_char(v.fecha, 'YYYY-MM')"),
            Map.entry("dia_semana", "(EXTRACT(ISODOW FROM v.fecha)::int || '-' || CASE EXTRACT(ISODOW FROM v.fecha)::int"
                    + " WHEN 1 THEN 'lunes' WHEN 2 THEN 'martes' WHEN 3 THEN 'miercoles' WHEN 4 THEN 'jueves'"
                    + " WHEN 5 THEN 'viernes' WHEN 6 THEN 'sabado' ELSE 'domingo' END)"),
            Map.entry("sucursal", "COALESCE(s.nombre, 'Sin sucursal')"),
            Map.entry("categoria", "COALESCE(NULLIF(TRIM(c.nombre), ''), 'Sin categoria')"),
            Map.entry("producto", "COALESCE(p.nombre, 'Producto ' || d.producto_id)"),
            Map.entry("tipo_venta", "UPPER(v.tipo_venta)"));

    public static final String[] OPCIONES_AGRUPAR =
            {"total", "dia", "semana", "mes", "dia_semana", "sucursal", "categoria", "producto", "tipo_venta"};

    public static final Parametro AGRUPAR = Parametro.opciones(
            "Como agrupar. total = un solo numero; dia/semana/mes = serie en el tiempo; dia_semana = patron semanal;"
                    + " sucursal/categoria/producto/tipo_venta = comparacion entre grupos.", OPCIONES_AGRUPAR);

    private static final Set<String> TIEMPO = Set.of("dia", "semana", "mes");
    private static final Set<String> POCAS_PARTES = Set.of("tipo_venta");

    /** Linea si se agrupa por tiempo, torta si son pocas partes de un todo, barras en lo demas; nada si es total. */
    public static Visual visualAgrupado(String agrupar, String titulo, String serie) {
        if (agrupar == null || "total".equals(agrupar)) return null;
        if (TIEMPO.contains(agrupar)) return Visual.linea(titulo, "grupo", serie);
        if (POCAS_PARTES.contains(agrupar)) return Visual.torta(titulo, "grupo", serie);
        return Visual.barras(titulo, "grupo", serie);
    }

    /** Orden natural del grupo: cronologico para tiempo, por metrica para el resto. */
    public static String orden(String agrupar, String metrica) {
        if (agrupar == null || TIEMPO.contains(agrupar) || "dia_semana".equals(agrupar)) return " ORDER BY grupo";
        return " ORDER BY " + metrica + " DESC NULLS LAST";
    }
}
