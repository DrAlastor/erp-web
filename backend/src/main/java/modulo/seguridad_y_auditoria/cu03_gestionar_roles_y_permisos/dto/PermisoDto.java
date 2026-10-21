package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto;

/**
 * Un permiso del catálogo tal como lo consume la pantalla de Roles y Permisos. Incluye las
 * etiquetas legibles para no tener que traducir los códigos en el frontend.
 */
public record PermisoDto(
        String codigo,
        String modulo,
        String accion,
        String etiquetaModulo,
        String etiquetaAccion,
        String descripcion) {
}
