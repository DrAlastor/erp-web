package modulo.seguridad_y_auditoria.roles_y_permisos.service;

import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.AccionErp;
import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.CatalogoPermisos;
import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.ModuloErp;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.BitacoraEvento;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Permiso;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Rol;
import modulo.seguridad_y_auditoria.roles_y_permisos.exception.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.roles_y_permisos.exception.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.roles_y_permisos.mapper.PermisoMapper;
import modulo.seguridad_y_auditoria.roles_y_permisos.mapper.RolMapper;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.BitacoraRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.PermisoRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.RolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.PermisoDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.RolDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administración de los roles de una empresa: consultarlos, editar su matriz de permisos y
 * activarlos o desactivarlos.
 *
 * <p>Los siete roles son de sistema, así que no se crean ni se borran. Toda operación va
 * siempre acotada a la empresa de quien la pide: un rol de otra empresa "no existe".
 */
@Service
public class ServicioRoles {

    /** El rol que administra la seguridad; tiene protecciones especiales. */
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    /**
     * Permisos que el rol Administrador no puede perder. Sin ellos nadie podría volver a
     * entrar a la pantalla de Roles y Permisos ni corregir el error: el sistema quedaría
     * sin forma de administrarse.
     */
    private static final Set<String> PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR = Set.of(
            CatalogoPermisos.codigo(ModuloErp.SEGURIDAD, AccionErp.CONSULTAR),
            CatalogoPermisos.codigo(ModuloErp.SEGURIDAD, AccionErp.MODIFICAR));

    private final RolRepositorio rolRepositorio;
    private final PermisoRepositorio permisoRepositorio;
    private final BitacoraRepositorio bitacoraRepositorio;
    private final ServicioAutorizacion servicioAutorizacion;
    private final RolMapper rolMapper;
    private final PermisoMapper permisoMapper;

    public ServicioRoles(RolRepositorio rolRepositorio,
                         PermisoRepositorio permisoRepositorio,
                         BitacoraRepositorio bitacoraRepositorio,
                         ServicioAutorizacion servicioAutorizacion,
                         RolMapper rolMapper,
                         PermisoMapper permisoMapper) {
        this.rolRepositorio = rolRepositorio;
        this.permisoRepositorio = permisoRepositorio;
        this.bitacoraRepositorio = bitacoraRepositorio;
        this.servicioAutorizacion = servicioAutorizacion;
        this.rolMapper = rolMapper;
        this.permisoMapper = permisoMapper;
    }

    /** Roles de la empresa con su matriz de permisos. */
    @Transactional(readOnly = true)
    public List<RolDto> listar(UUID empresaId) {
        return rolRepositorio.findByEmpresaIdOrderByNombre(empresaId).stream()
                .map(rolMapper::toDto)
                .toList();
    }

    /** Catálogo completo de permisos, para dibujar las columnas de la matriz. */
    @Transactional(readOnly = true)
    public List<PermisoDto> catalogo() {
        return permisoRepositorio.findAllByOrderByModuloAscAccionAsc().stream()
                .map(permisoMapper::toDto)
                .toList();
    }

    /**
     * Reemplaza por completo la matriz de permisos de un rol y recalcula los permisos
     * efectivos de todos sus usuarios.
     */
    @Transactional
    public RolDto reemplazarMatriz(UUID empresaId, UUID rolId, Collection<String> codigos, UsuarioPrincipal actor) {
        Rol rol = buscarRol(empresaId, rolId);

        validarQueLosPermisosExistan(codigos);
        validarQueElAdministradorConserveLaSeguridad(rol, codigos);

        Set<Permiso> permisos = new LinkedHashSet<>(permisoRepositorio.findByCodigoIn(codigos));
        rol.reemplazarPermisos(permisos);
        rolRepositorio.save(rol);

        auditar(empresaId, actor, BitacoraEvento.MATRIZ_MODIFICADA,
                "Matriz del rol " + rol.getCodigo() + " actualizada a " + permisos.size() + " permisos");

        // El paso "recalcular los permisos efectivos" del diagrama de actividad.
        servicioAutorizacion.invalidarCacheDeRol(rolId);

        return rolMapper.toDto(rol);
    }

    /** Activa o desactiva un rol. Un rol inactivo deja de otorgar permisos a todos sus usuarios. */
    @Transactional
    public RolDto cambiarEstado(UUID empresaId, UUID rolId, boolean activo, UsuarioPrincipal actor) {
        Rol rol = buscarRol(empresaId, rolId);

        if (!activo && ROL_ADMINISTRADOR.equals(rol.getCodigo())) {
            throw new ReglaDeNegocio("ADMINISTRADOR_NO_SE_DESACTIVA",
                    "El rol Administrador no se puede desactivar: dejaría al sistema sin quien administre "
                            + "los roles y permisos");
        }

        rol.setActivo(activo);
        rolRepositorio.save(rol);

        auditar(empresaId, actor, BitacoraEvento.ROL_ESTADO_CAMBIADO,
                "Rol " + rol.getCodigo() + (activo ? " activado" : " desactivado"));

        servicioAutorizacion.invalidarCacheDeRol(rolId);

        return rolMapper.toDto(rol);
    }

    private Rol buscarRol(UUID empresaId, UUID rolId) {
        return rolRepositorio.findByIdAndEmpresaId(rolId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO",
                        "No existe ese rol en la empresa"));
    }

    private void validarQueLosPermisosExistan(Collection<String> codigos) {
        for (String codigo : codigos) {
            if (!CatalogoPermisos.existe(codigo)) {
                throw new ReglaDeNegocio("PERMISO_DESCONOCIDO",
                        "El permiso " + codigo + " no existe en el catálogo del ERP");
            }
        }
    }

    private void validarQueElAdministradorConserveLaSeguridad(Rol rol, Collection<String> codigos) {
        if (!ROL_ADMINISTRADOR.equals(rol.getCodigo())) {
            return;
        }
        if (!codigos.containsAll(PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR)) {
            throw new ReglaDeNegocio("ADMINISTRADOR_SIN_SEGURIDAD",
                    "El rol Administrador debe conservar los permisos "
                            + String.join(" y ", PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR)
                            + ": sin ellos nadie podría volver a administrar los roles");
        }
    }

    private void auditar(UUID empresaId, UsuarioPrincipal actor, String accion, String detalle) {
        bitacoraRepositorio.save(new BitacoraEvento(
                empresaId,
                actor == null ? null : actor.usuarioId(),
                accion,
                detalle));
    }
}
