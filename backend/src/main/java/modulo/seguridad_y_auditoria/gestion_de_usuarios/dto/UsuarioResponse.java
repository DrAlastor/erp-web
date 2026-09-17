package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String fullname,
        Boolean enable,
        List<RolResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public record RolResponse(Integer id, String nombre) {}
}
