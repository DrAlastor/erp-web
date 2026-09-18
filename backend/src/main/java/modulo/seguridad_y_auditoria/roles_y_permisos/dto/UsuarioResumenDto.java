package modulo.seguridad_y_auditoria.roles_y_permisos.dto;

/**
 * Datos mínimos de un usuario para poder elegirlo al asignar un rol. El CRUD completo de
 * usuarios es la CU-02.
 *
 * <p>La conversión desde la entidad la hace
 * {@link modulo.seguridad_y_auditoria.roles_y_permisos.mapper.UsuarioResumenMapper}.
 */
public record UsuarioResumenDto(Long id, String nombre, String email, boolean activo) {
}
