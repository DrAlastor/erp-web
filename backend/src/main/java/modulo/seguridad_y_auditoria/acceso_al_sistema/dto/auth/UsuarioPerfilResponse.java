package modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth;

public record UsuarioPerfilResponse(
    Long id,
    String username,
    String email,
    String fullname
) {
}
