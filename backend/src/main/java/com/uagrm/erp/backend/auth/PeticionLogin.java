package com.uagrm.erp.backend.auth;

import jakarta.validation.constraints.NotBlank;

/** Credenciales que envía el cliente Angular al iniciar sesión. */
public record PeticionLogin(
        @NotBlank(message = "El email es obligatorio") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
