package modulo.seguridad_y_auditoria.roles_y_permisos.catalogo;

/**
 * Un permiso del catálogo: el par módulo-acción con su código y su descripción.
 *
 * <p>Es la definición inmutable que vive en el código, no la fila de la tabla
 * {@code permiso}. El aprovisionamiento se encarga de que la base refleje este catálogo.
 */
public record DefinicionPermiso(String codigo, ModuloErp modulo, AccionErp accion, String descripcion) {

    public DefinicionPermiso {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del permiso es obligatorio");
        }
        if (modulo == null || accion == null) {
            throw new IllegalArgumentException("El permiso " + codigo + " necesita módulo y acción");
        }
    }
}
