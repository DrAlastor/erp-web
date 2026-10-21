package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo;

import java.util.List;

import static modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz.*;

/**
 * Catálogo cerrado de reportes que se pueden pedir por voz. Lo que interprete N8N o el
 * intérprete local siempre termina en uno de estos valores: nunca se arma SQL a partir del
 * texto dictado.
 */
public enum MetricaVoz {

    VENTAS("Ventas", true, false, false, NINGUNA,
            List.of(NINGUNA, DIA, MES, SUCURSAL, CLIENTE, VENDEDOR)),
    PRODUCTOS_VENDIDOS("Productos vendidos", true, true, false, PRODUCTO,
            List.of(PRODUCTO, CATEGORIA, SUCURSAL, DIA, MES, NINGUNA)),
    STOCK("Stock de inventario", false, true, true, PRODUCTO,
            List.of(PRODUCTO, CATEGORIA, SUCURSAL, NINGUNA)),
    MOVIMIENTOS_KARDEX("Movimientos de Kardex", true, true, false, TIPO,
            List.of(TIPO, PRODUCTO, SUCURSAL, DIA, MES, NINGUNA)),
    CUENTAS_POR_COBRAR("Cuentas por cobrar", true, false, true, CLIENTE,
            List.of(CLIENTE, ESTADO, SUCURSAL, MES, NINGUNA)),
    COMPRAS("Compras", true, false, false, NINGUNA,
            List.of(NINGUNA, PROVEEDOR, SUCURSAL, DIA, MES));

    private final String etiqueta;
    private final boolean usaFechas;
    private final boolean admiteProducto;
    private final boolean admiteCriticos;
    private final AgrupacionVoz agrupacionPorDefecto;
    private final List<AgrupacionVoz> agrupaciones;

    MetricaVoz(String etiqueta, boolean usaFechas, boolean admiteProducto, boolean admiteCriticos,
               AgrupacionVoz agrupacionPorDefecto, List<AgrupacionVoz> agrupaciones) {
        this.etiqueta = etiqueta;
        this.usaFechas = usaFechas;
        this.admiteProducto = admiteProducto;
        this.admiteCriticos = admiteCriticos;
        this.agrupacionPorDefecto = agrupacionPorDefecto;
        this.agrupaciones = agrupaciones;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** El stock es una foto del momento: no se filtra por fechas. */
    public boolean usaFechas() {
        return usaFechas;
    }

    public boolean admiteProducto() {
        return admiteProducto;
    }

    /** Stock bajo el mínimo o cuentas vencidas con saldo, según la métrica. */
    public boolean admiteCriticos() {
        return admiteCriticos;
    }

    public AgrupacionVoz agrupacionPorDefecto() {
        return agrupacionPorDefecto;
    }

    public List<AgrupacionVoz> agrupaciones() {
        return agrupaciones;
    }

    public boolean admite(AgrupacionVoz agrupacion) {
        return agrupaciones.contains(agrupacion);
    }
}
