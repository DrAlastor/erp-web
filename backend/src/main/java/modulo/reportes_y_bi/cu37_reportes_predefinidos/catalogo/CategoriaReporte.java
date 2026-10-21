package modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo;

/**
 * Agrupa los reportes y decide quién los ve: cada categoría exige un permiso del catálogo
 * de la HU-03 ({@code MODULO_ACCION}).
 *
 * <p>Ventas usa {@code FACTURACION_CONSULTAR} y no {@code COMERCIAL_CONSULTAR}, porque este
 * último lo recibe cualquier rol como acceso operativo (ver {@code ServicioAutorizacion}) y
 * el Almacenero terminaría viendo las ventas.
 */
public enum CategoriaReporte {

    VENTAS("Ventas", "FACTURACION", "CONSULTAR"),
    INVENTARIO("Inventario", "INVENTARIO", "CONSULTAR");

    private final String etiqueta;
    private final String modulo;
    private final String accion;

    CategoriaReporte(String etiqueta, String modulo, String accion) {
        this.etiqueta = etiqueta;
        this.modulo = modulo;
        this.accion = accion;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String modulo() {
        return modulo;
    }

    public String accion() {
        return accion;
    }

    /** Código del permiso que exige, por ejemplo {@code FACTURACION_CONSULTAR}. */
    public String permiso() {
        return modulo + "_" + accion;
    }
}
