package modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo;

/**
 * Columna de un reporte.
 *
 * @param clave      nombre del campo en cada fila; coincide con el alias de la consulta SQL
 * @param etiqueta   encabezado que se muestra
 * @param tipo       cómo se formatea el valor
 * @param totalizar  si la fila de totales suma esta columna
 */
public record Columna(String clave, String etiqueta, Tipo tipo, boolean totalizar) {

    public enum Tipo {
        TEXTO,
        FECHA,
        ENTERO,
        DECIMAL,
        MONEDA
    }

    public static Columna texto(String clave, String etiqueta) {
        return new Columna(clave, etiqueta, Tipo.TEXTO, false);
    }

    public static Columna fecha(String clave, String etiqueta) {
        return new Columna(clave, etiqueta, Tipo.FECHA, false);
    }

    public static Columna cantidad(String clave, String etiqueta, boolean totalizar) {
        return new Columna(clave, etiqueta, Tipo.DECIMAL, totalizar);
    }

    public static Columna conteo(String clave, String etiqueta) {
        return new Columna(clave, etiqueta, Tipo.ENTERO, true);
    }

    public static Columna moneda(String clave, String etiqueta, boolean totalizar) {
        return new Columna(clave, etiqueta, Tipo.MONEDA, totalizar);
    }
}
