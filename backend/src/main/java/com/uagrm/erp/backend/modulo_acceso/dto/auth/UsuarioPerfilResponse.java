package com.uagrm.erp.backend.modulo_acceso.dto.auth;

public record UsuarioPerfilResponse(
    Long id,
    String username,
    String email,
    String fullname
) {
}
