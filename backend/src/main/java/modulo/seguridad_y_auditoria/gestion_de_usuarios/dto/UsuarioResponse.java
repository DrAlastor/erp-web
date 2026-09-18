package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cuenta de usuario tal como la devuelve la API.
 *
 * <p>No incluye el hash de la contraseña, los intentos fallidos ni el bloqueo temporal: el
 * listado y el detalle solo muestran lo que la pantalla necesita.
 */
public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String fullname,
        Boolean enable,
        List<RolResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Rol asignado, reducido a lo que muestran el listado y los filtros. */
    public record RolResponse(UUID id, String nombre) {
    }
}
