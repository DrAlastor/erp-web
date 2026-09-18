package modulo.seguridad_y_auditoria.roles_y_permisos.catalogo;

/**
 * Módulos del ERP sobre los que se otorgan permisos.
 *
 * <p>Son los seis de la matriz de permisos por rol de la HU-03. El orden es el de las
 * columnas del documento, porque es el que usa la pantalla de Roles y Permisos.
 */
public enum ModuloErp {

    SEGURIDAD("Seguridad y Auditoría"),
    COMERCIAL("Comercial y Ventas"),
    INVENTARIO("Inventarios y Compras"),
    CONTABILIDAD("Contabilidad"),
    FACTURACION("Facturación"),
    REPORTES("Reportes");

    private final String etiqueta;

    ModuloErp(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre del módulo tal como se muestra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }
}
