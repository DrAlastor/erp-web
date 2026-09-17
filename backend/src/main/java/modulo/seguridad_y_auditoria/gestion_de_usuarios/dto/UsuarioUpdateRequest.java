package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.*;

public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 150) String fullname,
        @NotBlank @Email @Size(max = 100) String email) {
    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
