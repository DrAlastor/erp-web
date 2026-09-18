package com.uagrm.erp.backend.auth;

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
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
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
