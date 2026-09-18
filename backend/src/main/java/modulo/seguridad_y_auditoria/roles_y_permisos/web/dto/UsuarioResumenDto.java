package modulo.seguridad_y_auditoria.roles_y_permisos.web.dto;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Usuario;

import java.util.UUID;

/**
 * Datos mínimos de un usuario para poder elegirlo al asignar un rol. El CRUD completo de
 * usuarios es la CU-02.
 */
public record UsuarioResumenDto(Long id, String nombre, String email, boolean activo) {

    public static UsuarioResumenDto de(Usuario usuario) {
        return new UsuarioResumenDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.isActivo());
    }
}
