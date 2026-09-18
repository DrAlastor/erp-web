package com.uagrm.erp.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Toma el token de la cabecera {@code Authorization: Bearer ...} y deja el
 * {@link UsuarioPrincipal} autenticado en el contexto de seguridad.
 *
 * <p>Si el token falta o no es válido, no autentica y sigue: quien decide si la petición
 * necesitaba autenticación es la cadena de seguridad, no este filtro.
 *
 * <p>La autenticación que arma no lleva <em>authorities</em>: los permisos los resuelve
 * {@code ServicioAutorizacion} contra la base en cada verificación.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;

    public FiltroJwt(ServicioJwt servicioJwt) {
        this.servicioJwt = servicioJwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena) throws ServletException, IOException {
        String cabecera = peticion.getHeader(CABECERA);

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            servicioJwt.leer(cabecera.substring(PREFIJO.length()))
                    .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, List.of())));
        }

        cadena.doFilter(peticion, respuesta);
    }
}
