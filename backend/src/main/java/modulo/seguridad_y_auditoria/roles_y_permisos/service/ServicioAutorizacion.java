package modulo.seguridad_y_auditoria.roles_y_permisos.service;

import modulo.seguridad_y_auditoria.roles_y_permisos.repository.UsuarioRolRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de autorización del ERP: la clase {@code ServicioAutorizacion} del diagrama de
 * clases de la HU-03 y la única fuente de verdad para decidir si un usuario puede hacer
 * algo.
 *
 * <p>Los <em>permisos efectivos</em> de un usuario son la unión de los permisos de todos
 * sus roles <strong>activos</strong>. Se leen de la base de datos, no del token: así,
 * cuando el administrador le cambia la matriz a un rol o le quita una asignación a alguien,
 * el cambio tiene efecto en la siguiente petición y no cuando expire el JWT. Eso es lo que
 * el diagrama de actividad llama "recalcular los permisos efectivos".
 *
 * <p>Para no pegarle a la base en cada petición hay una caché por usuario. Toda operación
 * que cambie roles, matrices o asignaciones tiene que invalidarla con
 * {@link #invalidarCache(UUID)} o {@link #invalidarCacheDeRol(UUID)}.
 */
@Service
public class ServicioAutorizacion {

    private final UsuarioRolRepositorio usuarioRolRepositorio;

    /** Permisos efectivos por usuario. La invalidación es explícita, no por tiempo. */
    private final Map<Long, Set<String>> cachePorUsuario = new ConcurrentHashMap<>();

    public ServicioAutorizacion(UsuarioRolRepositorio usuarioRolRepositorio) {
        this.usuarioRolRepositorio = usuarioRolRepositorio;
    }

    /**
     * Permisos efectivos del usuario: la unión de los permisos de sus roles activos.
     *
     * @return conjunto inmutable de códigos de permiso; vacío si no tiene ninguno
     */
    @Transactional(readOnly = true)
    public Set<String> permisosEfectivos(Long usuarioId) {
        if (usuarioId == null) {
            return Set.of();
        }
        return cachePorUsuario.computeIfAbsent(usuarioId, this::leerDeLaBase);
    }

    /** Indica si el usuario tiene un permiso concreto, por ejemplo {@code COMERCIAL_ANULAR}. */
    @Transactional(readOnly = true)
    public boolean tienePermiso(Long usuarioId, String codigoPermiso) {
        if (usuarioId == null || codigoPermiso == null || codigoPermiso.isBlank()) {
            return false;
        }
        return permisosEfectivos(usuarioId).contains(codigoPermiso);
    }

    /** Descarta los permisos cacheados de un usuario. */
    public void invalidarCache(Long usuarioId) {
        if (usuarioId != null) {
            cachePorUsuario.remove(usuarioId);
        }
    }

    /**
     * Descarta los permisos cacheados de todos los usuarios que tengan un rol. Se llama al
     * editar la matriz del rol o al activarlo o desactivarlo.
     */
    @Transactional(readOnly = true)
    public void invalidarCacheDeRol(UUID rolId) {
        if (rolId == null) {
            return;
        }
        for (Long usuarioId : usuarioRolRepositorio.findUsuarioIdsPorRol(rolId)) {
            cachePorUsuario.remove(usuarioId);
        }
    }

    /** Descarta toda la caché. Pensado para pruebas y para cambios masivos. */
    public void invalidarTodo() {
        cachePorUsuario.clear();
    }

    private Set<String> leerDeLaBase(Long usuarioId) {
        return Set.copyOf(new LinkedHashSet<>(usuarioRolRepositorio.findCodigosDePermisosEfectivos(usuarioId)));
    }
}
