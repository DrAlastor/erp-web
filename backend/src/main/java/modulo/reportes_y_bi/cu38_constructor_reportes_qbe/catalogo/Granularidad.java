package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo;

/** Cómo se muestra (y agrupa) un campo de fecha. */
public enum Granularidad {

    FECHA_HORA("Fecha y hora", "%s", TipoCampo.FECHA),
    DIA("Día", "CAST(%s AS DATE)", TipoCampo.FECHA),
    MES("Mes", "TO_CHAR(%s, 'YYYY-MM')", TipoCampo.TEXTO),
    ANIO("Año", "TO_CHAR(%s, 'YYYY')", TipoCampo.TEXTO);

    private final String etiqueta;
    private final String plantillaSql;
    private final TipoCampo tipoResultado;

    Granularidad(String etiqueta, String plantillaSql, TipoCampo tipoResultado) {
        this.etiqueta = etiqueta;
        this.plantillaSql = plantillaSql;
        this.tipoResultado = tipoResultado;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aplicar(String sql) {
        return String.format(plantillaSql, sql);
    }

    public TipoCampo tipoResultado() {
        return tipoResultado;
    }
}
