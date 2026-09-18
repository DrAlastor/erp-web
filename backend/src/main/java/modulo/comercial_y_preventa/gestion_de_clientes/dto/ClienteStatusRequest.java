package modulo.comercial_y_preventa.gestion_de_clientes.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record ClienteStatusRequest(@NotNull(message = "El estado es obligatorio") Boolean activo) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
