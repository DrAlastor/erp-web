package modulo.seguridad_y_auditoria.roles_y_permisos.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 en JSON cuando la petición no trae un token válido, en lugar del formulario
 * de login o del {@code WWW-Authenticate} que Spring Security usa por omisión. El cliente
 * Angular lo usa para mandar al usuario a la pantalla de inicio de sesión.
 */
@Component
public class PuntoDeEntradaNoAutenticado implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        RespuestasDeSeguridad.escribir(respuesta, HttpStatus.UNAUTHORIZED.value(),
                RespuestasDeSeguridad.noAutenticado());
    }
}
