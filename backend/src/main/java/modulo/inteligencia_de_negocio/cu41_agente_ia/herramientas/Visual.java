package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.util.List;

/**
 * CU41 - Grafico que le corresponde al resultado de una herramienta (regla fija del backend).
 * {@code x} es la columna del eje/categoria, {@code series} las columnas numericas y {@code rango}
 * las columnas de la banda bajo-alto de un pronostico.
 */
public record Visual(String titulo, String tipo, String x, List<String> series, Rango rango) {

    public record Rango(String bajo, String alto) {
    }

    public static Visual linea(String titulo, String x, String... series) {
        return new Visual(titulo, "linea", x, List.of(series), null);
    }

    public static Visual barras(String titulo, String x, String... series) {
        return new Visual(titulo, "barras", x, List.of(series), null);
    }

    public static Visual torta(String titulo, String x, String serie) {
        return new Visual(titulo, "torta", x, List.of(serie), null);
    }

    public static Visual tabla(String titulo) {
        return new Visual(titulo, "tabla", null, List.of(), null);
    }

    public static Visual pronostico(String titulo, String x, String serie, String bajo, String alto) {
        return new Visual(titulo, "linea", x, List.of(serie), new Rango(bajo, alto));
    }

    /** Filas minimas para que el grafico tenga sentido. */
    public int minimoFilas() {
        return "tabla".equals(tipo) ? 1 : 2;
    }
}
