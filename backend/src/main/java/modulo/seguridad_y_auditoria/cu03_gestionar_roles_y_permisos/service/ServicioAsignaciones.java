package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service;

import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.AsignacionDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.UsuarioResumenDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Asignación de roles a usuarios directamente sincronizado con la tabla usuario de ERP_Asus.
 */
@Service
public class ServicioAsignaciones {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public ServicioAsignaciones(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /** Usuarios de la empresa registrados en la base de datos. */
    @Transactional(readOnly = true)
    public List<UsuarioResumenDto> usuariosDeLaEmpresa(UUID empresaId) {
        return usuarioRepository.findAll().stream()
                .map(u -> new UsuarioResumenDto(
                        u.getId(),
                        u.getFullname(),
                        u.getEmail(),
                        Boolean.TRUE.equals(u.getActivo())))
                .toList();
    }

    /** Roles que tiene asignados un usuario en la tabla usuario. */
    @Transactional(readOnly = true)
    public List<AsignacionDto> rolesDe(UUID empresaId, Long usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (usuario.getRol() == null) {
            return Collections.emptyList();
        }
        Rol rol = usuario.getRol();
        return List.of(new AsignacionDto(
                String.valueOf(rol.getId()),
                rol.getNombre().toUpperCase(),
                rol.getNombre(),
                Boolean.TRUE.equals(rol.getActivo()),
                OffsetDateTime.now()));
    }

    /** Asigna un rol a un usuario. */
    @Transactional
    public void asignar(UUID empresaId, Long usuarioId, String rolId, UsuarioPrincipal actor) {
        Usuario usuario = buscarUsuario(usuarioId);
        Rol rol = buscarRol(rolId);

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new ReglaDeNegocio("ROL_INACTIVO",
                    "El rol " + rol.getNombre() + " está desactivado y no se puede asignar");
        }

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
    }

    /** Quita el rol asignado a un usuario. */
    @Transactional
    public void quitar(UUID empresaId, Long usuarioId, String rolId, UsuarioPrincipal actor) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (usuario.getRol() != null && String.valueOf(usuario.getRol().getId()).equals(rolId)) {
            usuario.setRol(null);
            usuarioRepository.save(usuario);
        }
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontrado("USUARIO_NO_ENCONTRADO",
                        "No existe el usuario con ID " + usuarioId));
    }

    private Rol buscarRol(String rolId) {
        try {
            Integer id = Integer.parseInt(rolId);
            return rolRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO",
                            "No existe el rol con ID " + rolId));
        } catch (NumberFormatException e) {
            return rolRepository.findByNombre(rolId)
                    .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO",
                            "No existe el rol " + rolId));
        }
    }
}
