package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo;

/**
 * Criterios por los que se puede agrupar un reporte por voz. {@link #NINGUNA} devuelve una
 * sola fila con el total general y se presenta como tarjetas KPI.
 */
public enum AgrupacionVoz {

    NINGUNA("Total general", false),
    DIA("Día", true),
    MES("Mes", true),
    SUCURSAL("Sucursal", false),
    CLIENTE("Cliente", false),
    VENDEDOR("Vendedor / cajero", false),
    PRODUCTO("Producto", false),
    CATEGORIA("Categoría", false),
    PROVEEDOR("Proveedor", false),
    TIPO("Tipo de movimiento", false),
    ESTADO("Estado", false);

    private final String etiqueta;
    private final boolean temporal;

    AgrupacionVoz(String etiqueta, boolean temporal) {
        this.etiqueta = etiqueta;
        this.temporal = temporal;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Las agrupaciones en el tiempo se ordenan cronológicamente y se grafican con líneas. */
    public boolean temporal() {
        return temporal;
    }
}
