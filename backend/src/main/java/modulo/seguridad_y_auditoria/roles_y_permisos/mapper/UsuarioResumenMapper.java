package modulo.seguridad_y_auditoria.roles_y_permisos.mapper;

import modulo.seguridad_y_auditoria.roles_y_permisos.dto.UsuarioResumenDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Usuario;

import org.springframework.stereotype.Component;

/**
 * Convierte el usuario del RBAC en el resumen que necesita la pantalla de asignaciones.
 *
 * <p>No expone credenciales: solo lo justo para poder elegir a quién asignarle un rol. El
 * CRUD completo de usuarios es la CU-02, que tiene su propio mapper.
 */
@Component
public class UsuarioResumenMapper {

    public UsuarioResumenDto toResumen(Usuario usuario) {
        return new UsuarioResumenDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.isActivo());
    }
}
