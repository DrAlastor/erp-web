package modulo.seguridad_y_auditoria.roles_y_permisos.web.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que el frontend necesita para armar el menú y decidir qué botones mostrar.
 *
 * <p>Se pide aparte del login, y no viene dentro del token, para que refleje siempre el
 * estado actual de los permisos.
 */
public record MisPermisosDto(
        Long usuarioId,
        UUID empresaId,
        String nombre,
        Set<String> permisos,
        List<String> roles) {
}
