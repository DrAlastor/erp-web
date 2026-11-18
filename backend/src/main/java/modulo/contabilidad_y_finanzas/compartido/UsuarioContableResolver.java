package modulo.contabilidad_y_finanzas.compartido;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Id de la tabla {@code usuario} del usuario autenticado, que queda como autor de los
 * asientos (CU27) y de los cobros (CU29). Mismo criterio que la CU34: identidad del token
 * verificada contra la tabla y, si no, búsqueda por correo. Nunca se asume un usuario.
 */
@Component
@RequiredArgsConstructor
public class UsuarioContableResolver {

    private final NamedParameterJdbcTemplate jdbc;

    public Long resolver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw sinUsuario();

        Long usuarioId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof ApplicationUserPrincipal app && app.identidad() != null) {
            usuarioId = app.identidad().usuarioId();
        } else if (principal instanceof UsuarioPrincipal identidad) {
            usuarioId = identidad.usuarioId();
        }
        if (usuarioId != null && !jdbc.queryForList(
                "SELECT id FROM usuario WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", usuarioId), Long.class).isEmpty()) {
            return usuarioId;
        }
        List<Long> porEmail = jdbc.queryForList(
                "SELECT id FROM usuario WHERE LOWER(email) = LOWER(:email) AND activo = TRUE ORDER BY id LIMIT 1",
                new MapSqlParameterSource("email", authentication.getName()), Long.class);
        if (porEmail.isEmpty()) throw sinUsuario();
        return porEmail.get(0);
    }

    private ResponseStatusException sinUsuario() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No se pudo identificar al usuario que registra la operación");
    }
}
