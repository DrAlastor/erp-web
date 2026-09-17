package modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UsuarioPerfilResponse usuario
) {
}
