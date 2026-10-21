package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto;

/**
 * Datos mínimos de un usuario para poder elegirlo al asignar un rol. El CRUD completo de
 * usuarios es la CU-02.
 */
public record UsuarioResumenDto(Long id, String nombre, String email, boolean activo) {
}
