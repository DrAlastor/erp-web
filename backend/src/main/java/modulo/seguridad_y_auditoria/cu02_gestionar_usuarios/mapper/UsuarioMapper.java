package modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.mapper;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.RolAsignadoDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.entity.Rol;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Traduce entre la entidad {@link Usuario} del módulo compartido y los DTO de la CU-02.
 *
 * <p>El mapeo vive acá y no en el DTO para que la respuesta de la API no dependa del modelo
 * de persistencia: si mañana cambia una columna, cambia solo este archivo.
 */
@Component
public class UsuarioMapper {

    /** Arma la entidad del alta; la contraseña la cifra el servicio, no el mapper. */
    public Usuario toEntity(UsuarioRequest request, UUID empresaId, String actor) {
        Usuario usuario = new Usuario();
        String usr = request.username() != null && !request.username().trim().isBlank()
                ? request.username().trim()
                : request.email().trim().split("@")[0];
        usuario.setUsername(usr);
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setFullname(request.fullname().trim());
        usuario.setEmpresaId(empresaId);
        usuario.setCreatedBy(actor);
        if (request.empleadoId() != null) {
            usuario.setEmpleadoId(request.empleadoId());
        }
        return usuario;
    }

    public UsuarioResponse toResponse(Usuario usuario, List<RolAsignadoDto> roles) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getFullname(),
                usuario.getEnable(),
                roles.stream().map(this::toRolResponse).toList(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt());
    }

    public UsuarioResponse.RolResponse toRolResponse(RolAsignadoDto rol) {
        return new UsuarioResponse.RolResponse(rol.rolId(), rol.nombre());
    }

    public UsuarioResponse.RolResponse toRolResponse(Rol rol) {
        return new UsuarioResponse.RolResponse(rol.getId(), rol.getNombre());
    }
}
