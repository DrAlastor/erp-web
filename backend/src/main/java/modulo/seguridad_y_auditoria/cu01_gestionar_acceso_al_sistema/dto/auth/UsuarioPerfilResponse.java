package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth;

public record UsuarioPerfilResponse(
    Long id,
    String username,
    String email,
    String fullname
) {
}
