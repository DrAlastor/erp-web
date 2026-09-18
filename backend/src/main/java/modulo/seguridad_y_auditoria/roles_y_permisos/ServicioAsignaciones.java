package modulo.seguridad_y_auditoria.roles_y_permisos;

import modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.BitacoraEvento;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Rol;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Usuario;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.UsuarioRol;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.UsuarioRolId;
import modulo.seguridad_y_auditoria.roles_y_permisos.error.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.roles_y_permisos.error.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.BitacoraRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.RolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.UsuarioRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.UsuarioRolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.AsignacionDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.UsuarioResumenDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Asignación de roles a usuarios. Implementa el flujo del diagrama de actividad de la
 * HU-03: validar que el rol esté activo, validar que el usuario no lo tenga ya asignado,
 * registrar la asignación, recalcular los permisos efectivos y dejar el evento en la
 * bitácora de auditoría.
 */
@Service
public class ServicioAsignaciones {

    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final BitacoraRepositorio bitacoraRepositorio;
    private final ServicioAutorizacion servicioAutorizacion;

    public ServicioAsignaciones(UsuarioRepositorio usuarioRepositorio,
                                RolRepositorio rolRepositorio,
                                UsuarioRolRepositorio usuarioRolRepositorio,
                                BitacoraRepositorio bitacoraRepositorio,
                                ServicioAutorizacion servicioAutorizacion) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.bitacoraRepositorio = bitacoraRepositorio;
        this.servicioAutorizacion = servicioAutorizacion;
    }

    /** Usuarios de la empresa, para poder elegir a quién asignarle un rol. */
    @Transactional(readOnly = true)
    public List<UsuarioResumenDto> usuariosDeLaEmpresa(UUID empresaId) {
        return usuarioRepositorio.findByEmpresaIdOrderByNombre(empresaId).stream()
                .map(UsuarioResumenDto::de)
                .toList();
    }

    /** Roles que tiene asignados un usuario. */
    @Transactional(readOnly = true)
    public List<AsignacionDto> rolesDe(UUID empresaId, Long usuarioId) {
        buscarUsuario(empresaId, usuarioId);

        return usuarioRolRepositorio.findByIdUsuarioId(usuarioId).stream()
                .map(asignacion -> new AsignacionDto(
                        asignacion.getRol().getId(),
                        asignacion.getRol().getCodigo(),
                        asignacion.getRol().getNombre(),
                        asignacion.getRol().isActivo(),
                        asignacion.getAsignadoEn()))
                .toList();
    }

    /** Asigna un rol a un usuario de la misma empresa. */
    @Transactional
    public void asignar(UUID empresaId, Long usuarioId, UUID rolId, UsuarioPrincipal actor) {
        Usuario usuario = buscarUsuario(empresaId, usuarioId);
        Rol rol = buscarRol(empresaId, rolId);

        if (!rol.isActivo()) {
            throw new ReglaDeNegocio("ROL_INACTIVO",
                    "El rol " + rol.getNombre() + " está desactivado y no se puede asignar");
        }

        if (usuarioRolRepositorio.existsById(new UsuarioRolId(usuarioId, rolId))) {
            throw new ReglaDeNegocio("ROL_YA_ASIGNADO",
                    "El usuario ya tiene asignado el rol " + rol.getNombre());
        }

        usuarioRolRepositorio.save(new UsuarioRol(usuarioId, rolId, actor == null ? null : actor.usuarioId()));

        auditar(empresaId, actor, BitacoraEvento.ROL_ASIGNADO,
                "Rol " + rol.getCodigo() + " asignado a " + usuario.getEmail());

        servicioAutorizacion.invalidarCache(usuarioId);
    }

    /** Quita un rol asignado a un usuario. */
    @Transactional
    public void quitar(UUID empresaId, Long usuarioId, UUID rolId, UsuarioPrincipal actor) {
        Usuario usuario = buscarUsuario(empresaId, usuarioId);
        Rol rol = buscarRol(empresaId, rolId);

        UsuarioRolId clave = new UsuarioRolId(usuarioId, rolId);
        if (!usuarioRolRepositorio.existsById(clave)) {
            throw new RecursoNoEncontrado("ASIGNACION_NO_ENCONTRADA",
                    "El usuario no tiene asignado ese rol");
        }

        usuarioRolRepositorio.deleteById(clave);

        auditar(empresaId, actor, BitacoraEvento.ROL_QUITADO,
                "Rol " + rol.getCodigo() + " quitado a " + usuario.getEmail());

        servicioAutorizacion.invalidarCache(usuarioId);
    }

    private Usuario buscarUsuario(UUID empresaId, Long usuarioId) {
        return usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontrado("USUARIO_NO_ENCONTRADO",
                        "No existe ese usuario en la empresa"));
    }

    private Rol buscarRol(UUID empresaId, UUID rolId) {
        return rolRepositorio.findByIdAndEmpresaId(rolId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO",
                        "No existe ese rol en la empresa"));
    }

    private void auditar(UUID empresaId, UsuarioPrincipal actor, String accion, String detalle) {
        bitacoraRepositorio.save(new BitacoraEvento(
                empresaId,
                actor == null ? null : actor.usuarioId(),
                accion,
                detalle));
    }
}
