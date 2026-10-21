package modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service;

import lombok.RequiredArgsConstructor;

import comun.exception.ResourceNotFoundException;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.dto.UsuarioUpdateRequest;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.mapper.UsuarioMapper;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.UsuarioGestionRepository;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.UsuarioSpecifications;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.RolAsignadoDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAsignaciones;

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
import comun.saas.TenantEntitlementsService;

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
    private final ServicioAsignaciones servicioAsignaciones;
    private final UsuarioAuditoriaService auditoria;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final TenantEntitlementsService entitlements;

    private final modulo.seguridad_y_auditoria.compartido.repository.RolRepository rolRepository;

    /** Listado paginado con búsqueda y filtros, sin datos sensibles. */
    public Page<UsuarioResponse> list(String search, String role, Boolean enable, int page, int size) {
        UUID empresaId = UsuarioActual.obtener().empresaId();
        Integer targetRolId = resolverRolId(role);
        Page<Usuario> cuentas = usuarioGestionRepository.findAll(
                UsuarioSpecifications.filtrar(search, targetRolId, enable, empresaId),
                PageRequest.of(page, size, Sort.by("id")));

        Map<Long, List<RolAsignadoDto>> rolesPorUsuario = rolesDeLaPagina(cuentas, empresaId);
        return cuentas.map(cuenta -> usuarioMapper.toResponse(cuenta,
                rolesPorUsuario.getOrDefault(cuenta.getId(), List.of())));
    }

    public Page<UsuarioResponse> list(String search, UUID role, Boolean enable, int page, int size) {
        return list(search, role != null ? role.toString() : null, enable, page, size);
    }

    private Integer resolverRolId(String roleParam) {
        if (roleParam == null || roleParam.trim().isBlank()) {
            return null;
        }
        String clean = roleParam.trim();
        try {
            int parsed = Integer.parseInt(clean);
            if (rolRepository.existsById(parsed)) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {}

        for (modulo.seguridad_y_auditoria.compartido.entity.Rol r : rolRepository.findAll()) {
            UUID uuid = UUID.nameUUIDFromBytes(r.getNombre().getBytes());
            if (uuid.toString().equalsIgnoreCase(clean) || r.getNombre().equalsIgnoreCase(clean)) {
                return r.getId();
            }
        }
        return -1;
    }

    /** Roles de la empresa, para los filtros del listado. */
    public List<UsuarioResponse.RolResponse> roles() {
        return rolRepository.findAll().stream()
                .map(r -> new UsuarioResponse.RolResponse(UUID.nameUUIDFromBytes(r.getNombre().getBytes()), r.getNombre()))
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
        entitlements.requireCapacity("USUARIOS", usuarioGestionRepository.count());

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

        modulo.seguridad_y_auditoria.compartido.entity.Rol rolAsignado = null;
        if (request.rolesIds() != null && !request.rolesIds().isEmpty()) {
            List<modulo.seguridad_y_auditoria.compartido.entity.Rol> todosLosRoles = rolRepository.findAll();
            for (UUID rolId : request.rolesIds()) {
                for (modulo.seguridad_y_auditoria.compartido.entity.Rol r : todosLosRoles) {
                    if (UUID.nameUUIDFromBytes(r.getNombre().getBytes()).equals(rolId)) {
                        if (Boolean.FALSE.equals(r.getActivo())) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un rol inactivo: " + r.getNombre());
                        }
                        rolAsignado = r;
                        break;
                    }
                }
                if (rolAsignado != null) break;
            }
        }
        if (rolAsignado == null) {
            rolAsignado = rolRepository.findAll().stream().findFirst().orElse(null);
        }

        if (request.empleadoId() != null) {
            if (usuarioGestionRepository.existsByEmpleadoId(request.empleadoId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El colaborador seleccionado ya cuenta con un usuario asignado");
            }
        }

        Usuario usuario = usuarioMapper.toEntity(request, empresaId, actor);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRbacInicializado(true);
        usuario.setRol(rolAsignado);
        usuario.setActivo(true);
        usuario.setNivelUsuario("GERENCIA");
        usuario.setFechaCreacion(java.time.LocalDateTime.now());

        usuarioGestionRepository.saveAndFlush(usuario);

        try {
            if (rolAsignado != null) {
                servicioAsignaciones.asignar(empresaId, usuario.getId(), String.valueOf(rolAsignado.getId()), UsuarioActual.obtener());
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
        List<RolAsignadoDto> roles = usuario.getRol() != null
                ? List.of(new RolAsignadoDto(usuario.getId(), UUID.nameUUIDFromBytes(usuario.getRol().getNombre().getBytes()), usuario.getRol().getNombre()))
                : List.of();
        return usuarioMapper.toResponse(usuario, roles);
    }

    /**
     * Roles de todos los usuarios de la página mapeados desde su rol directo en la tabla usuario.
     */
    private Map<Long, List<RolAsignadoDto>> rolesDeLaPagina(Page<Usuario> cuentas, UUID empresaId) {
        Map<Long, List<RolAsignadoDto>> map = new java.util.HashMap<>();
        for (Usuario u : cuentas) {
            if (u.getRol() != null) {
                map.put(u.getId(), List.of(new RolAsignadoDto(u.getId(), UUID.nameUUIDFromBytes(u.getRol().getNombre().getBytes()), u.getRol().getNombre())));
            } else {
                map.put(u.getId(), List.of());
            }
        }
        return map;
    }
}
