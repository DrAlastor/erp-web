package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo;

/**
 * Función de agregación de una columna. Como en la fila "Total" de un QBE: si alguna
 * columna tiene agregación, las columnas que no la tienen pasan a ser el agrupamiento.
 */
public enum Agregacion {

    SUMA("SUM", "Suma"),
    PROMEDIO("AVG", "Promedio"),
    CONTEO("COUNT", "Conteo"),
    MINIMO("MIN", "Mínimo"),
    MAXIMO("MAX", "Máximo");

    private final String funcionSql;
    private final String etiqueta;

    Agregacion(String funcionSql, String etiqueta) {
        this.funcionSql = funcionSql;
        this.etiqueta = etiqueta;
    }

    public String funcionSql() {
        return funcionSql;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Suma y promedio solo tienen sentido sobre números; mínimo y máximo también sobre fechas. */
    public boolean admite(TipoCampo tipo) {
        return switch (this) {
            case CONTEO -> true;
            case SUMA, PROMEDIO -> tipo.numerico();
            case MINIMO, MAXIMO -> tipo.numerico() || tipo == TipoCampo.FECHA;
        };
    }

    /** Tipo de la columna resultante. */
    public TipoCampo tipoResultado(TipoCampo tipo) {
        return this == CONTEO ? TipoCampo.NUMERO : tipo;
    }
}
