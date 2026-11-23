package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CU41 - SQL siempre parametrizado. Cada valor entra por {@link #valor}, {@link #fecha},
 * {@link #entero} o {@link #contiene} y vuelve como {@code ?}; ningun texto del modelo se
 * concatena al SQL. Las columnas de agrupacion salen de un mapa fijo ({@link #expresion}).
 */
public final class Consulta {

    private final StringBuilder sql = new StringBuilder();
    private final List<Object> parametros = new ArrayList<>();

    public Consulta sql(String fragmento) {
        sql.append(fragmento);
        return this;
    }

    public String valor(Object v) {
        parametros.add(v);
        return "?";
    }

    public String fecha(LocalDate f) {
        parametros.add(java.sql.Date.valueOf(f));
        return "CAST(? AS DATE)";
    }

    public String entero(int n) {
        parametros.add(n);
        return "CAST(? AS INTEGER)";
    }

    public String numero(BigDecimal n) {
        parametros.add(n);
        return "?";
    }

    /** {@code columna ILIKE '%texto%'} escapando %, _ y \ para que no funcionen como comodines. */
    public String contiene(String columna, String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        parametros.add("%" + escapado + "%");
        return columna + " ILIKE ?";
    }

    /** Agrega {@code AND columna ILIKE ?} solo si hay texto. */
    public Consulta filtroContiene(String columna, String texto) {
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND ").append(contiene(columna, texto.trim()));
        }
        return this;
    }

    /** Expresion SQL para una opcion de un enum validado; una opcion desconocida es un error de parametros. */
    public static String expresion(Map<String, String> mapa, String opcion) {
        String e = mapa.get(opcion);
        if (e == null) {
            throw new ErrorDeParametros("Opcion no valida: " + opcion);
        }
        return e;
    }

    public String texto() {
        return sql.toString();
    }

    public Object[] parametros() {
        return parametros.toArray();
    }

    @Override
    public String toString() {
        return sql.toString();
    }
}
