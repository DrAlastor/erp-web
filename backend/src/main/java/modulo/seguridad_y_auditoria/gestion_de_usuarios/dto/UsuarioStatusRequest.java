package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotNull;

public record UsuarioStatusRequest(@NotNull Boolean enable) {
    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
