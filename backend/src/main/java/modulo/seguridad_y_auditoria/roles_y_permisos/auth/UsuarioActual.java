package modulo.seguridad_y_auditoria.roles_y_permisos.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Acceso al usuario de la petición en curso.
 *
 * <p>Los controladores lo usan para saber de qué empresa es quien pregunta, que es lo que
 * aísla a los inquilinos entre sí.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** El usuario de la petición, si hay alguien autenticado. */
    public static Optional<UsuarioPrincipal> buscar() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return desde(autenticacion);
    }

    public static Optional<UsuarioPrincipal> desde(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()) return Optional.empty();
        if (autenticacion.getPrincipal() instanceof UsuarioPrincipal principal) return Optional.of(principal);
        if (autenticacion.getPrincipal() instanceof
                modulo.seguridad_y_auditoria.acceso_al_sistema.security.ApplicationUserPrincipal principal)
            return Optional.ofNullable(principal.identidad());
        return Optional.empty();
    }

    /**
     * El usuario de la petición.
     *
     * @throws AccessDeniedException si no hay nadie autenticado
     */
    public static UsuarioPrincipal obtener() {
        return buscar().orElseThrow(() -> new AccessDeniedException("No hay un usuario autenticado en la petición"));
    }
}
