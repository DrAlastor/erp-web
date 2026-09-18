package modulo.seguridad_y_auditoria.gestion_de_usuarios.service;

import lombok.RequiredArgsConstructor;

import comun.exception.ResourceNotFoundException;

import modulo.seguridad_y_auditoria.acceso_al_sistema.service.SesionService;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioUpdateRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.mapper.UsuarioMapper;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.repository.UsuarioGestionRepository;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.repository.UsuarioSpecifications;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.RolAsignadoDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Rol;
import modulo.seguridad_y_auditoria.roles_y_permisos.exception.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.roles_y_permisos.exception.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.RolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.UsuarioRolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.roles_y_permisos.service.ServicioAsignaciones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CU-02: reglas de negocio de la administración de cuentas de usuario.
 *
 * <p>Toda operación queda acotada a la empresa del actor, que se resuelve desde la identidad
 * autenticada: un usuario de otra empresa "no existe" y no se puede consultar ni tocar.
 *
 * <p>Los roles pertenecen a la CU-03 y se administran con su servicio de asignaciones, no con
 * SQL propio, para que las reglas de asignación —rol activo, empresa correcta, auditoría y
 * recálculo de permisos efectivos— se apliquen en un único lugar.
 *
 * <p>Cada cambio efectivo deja su registro en la bitácora dentro de la misma transacción.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    /** El límite de bcrypt: más bytes que esto se truncan en silencio al cifrar. */
    private static final int MAX_BYTES_CONTRASENA = 72;

    private final UsuarioGestionRepository usuarioGestionRepository;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final RolRepositorio rolRepositorio;
    private final ServicioAsignaciones servicioAsignaciones;
    private final UsuarioAuditoriaService auditoria;
    private final SesionService sesionService;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    /** Listado paginado con búsqueda y filtros, sin datos sensibles. */
    public Page<UsuarioResponse> list(String search, UUID role, Boolean enable, int page, int size) {
        UUID empresaId = UsuarioActual.obtener().empresaId();
        Page<Usuario> cuentas = usuarioGestionRepository.findAll(
                UsuarioSpecifications.filtrar(search, role, enable, empresaId),
                PageRequest.of(page, size, Sort.by("id")));

        Map<Long, List<RolAsignadoDto>> rolesPorUsuario = rolesDeLaPagina(cuentas, empresaId);
        return cuentas.map(cuenta -> usuarioMapper.toResponse(cuenta,
                rolesPorUsuario.getOrDefault(cuenta.getId(), List.of())));
    }

    /** Roles de la empresa, para los filtros del listado. */
    public List<UsuarioResponse.RolResponse> roles() {
        return rolRepositorio.findByEmpresaIdOrderByNombre(UsuarioActual.obtener().empresaId()).stream()
                .map(usuarioMapper::toRolResponse)
                .toList();
    }

    /** Detalle de una cuenta de la empresa del actor. */
    public UsuarioResponse detail(Long id) {
        return response(usuarioGestionRepository.findByIdAndEmpresaId(id, UsuarioActual.obtener().empresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario inexistente")));
    }

    /** Crea una cuenta con su contraseña cifrada y sus roles. */
    @Transactional
    public UsuarioResponse create(UsuarioRequest request, String actor, String ip) {
        UUID empresaId = UsuarioActual.obtener().empresaId();

        if (request.fullname().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena excede el limite de 72 bytes");
        }
        if (usuarioGestionRepository.existsByUsernameIgnoreCase(request.username().trim())
                || usuarioGestionRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario o correo ya pertenece a otra cuenta");
        }

        for (UUID rolId : request.rolesIds()) {
            Rol rol = rolRepositorio.findByIdAndEmpresaId(rolId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Rol inexistente en la empresa"));
            if (!rol.isActivo()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un rol inactivo");
            }
        }

        Usuario usuario = usuarioMapper.toEntity(request, empresaId, actor);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRbacInicializado(true);
        usuarioGestionRepository.saveAndFlush(usuario);

        try {
            for (UUID rolId : request.rolesIds()) {
                servicioAsignaciones.asignar(empresaId, usuario.getId(), rolId, UsuarioActual.obtener());
            }
        } catch (ReglaDeNegocio | RecursoNoEncontrado ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        auditoria.registrar("USUARIO_CREADO", actor, usuario.getId(), ip);
        usuarioGestionRepository.flush();
        return response(usuario);
    }

    /**
     * Edita nombre completo y correo. Nada más: la contraseña, los roles y el estado tienen
     * su propia operación para que cada cambio quede auditado por separado.
     */
    @Transactional
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request, String actor, String ip) {
        Usuario usuario = locked(id);
        String nombre = request.fullname().trim();
        String correo = request.email().trim();

        if (nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (usuarioGestionRepository.existsByEmailIgnoreCaseAndIdNot(correo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya pertenece a otra cuenta");
        }

        if (!nombre.equals(usuario.getFullname()) || !correo.equals(usuario.getEmail())) {
            usuario.setFullname(nombre);
            usuario.setEmail(correo);
            usuario.setUpdatedBy(actor);
            usuarioGestionRepository.saveAndFlush(usuario);
            auditoria.registrar("USUARIO_ACTUALIZADO", actor, id, ip);
        }

        return response(usuario);
    }

    /**
     * Activa o desactiva una cuenta. Al desactivarla se cierran sus sesiones de renovación y
     * el filtro JWT deja de aceptarla en la siguiente petición. Nadie puede desactivarse a
     * sí mismo: dejaría el sistema sin quien lo administre.
     */
    @Transactional
    public UsuarioResponse status(Long id, boolean enable, String actor, String ip) {
        Usuario usuario = locked(id);

        if (!enable && usuario.getUsername().equals(actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta");
        }

        if (!Boolean.valueOf(enable).equals(usuario.getEnable())) {
            usuario.setEnable(enable);
            usuario.setUpdatedBy(actor);
            usuarioGestionRepository.saveAndFlush(usuario);
            if (!enable) {
                sesionService.cerrarActivas(id);
            }
            auditoria.registrar(enable ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", actor, id, ip);
        }

        return response(usuario);
    }

    /** Cuenta de la empresa del actor, bloqueada para editarla sin carreras. */
    private Usuario locked(Long id) {
        return usuarioGestionRepository.findForAdministration(id, UsuarioActual.obtener().empresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario inexistente"));
    }

    private UsuarioResponse response(Usuario usuario) {
        return usuarioMapper.toResponse(usuario, usuarioRolRepositorio.findRolesDe(
                List.of(usuario.getId()), usuario.getEmpresaId()));
    }

    /**
     * Roles de todos los usuarios de la página en una sola consulta, agrupados por usuario.
     * Si la página viene vacía no se consulta nada: una lista vacía no es un {@code IN}
     * válido.
     */
    private Map<Long, List<RolAsignadoDto>> rolesDeLaPagina(Page<Usuario> cuentas, UUID empresaId) {
        List<Long> ids = cuentas.stream().map(Usuario::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarioRolRepositorio.findRolesDe(ids, empresaId).stream()
                .collect(Collectors.groupingBy(RolAsignadoDto::usuarioId));
    }
}
