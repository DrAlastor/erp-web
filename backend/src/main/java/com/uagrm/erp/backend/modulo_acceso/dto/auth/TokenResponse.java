package com.uagrm.erp.backend.modulo_acceso.dto.auth;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UsuarioPerfilResponse usuario
) {
}
