package modulo.comercial_y_preventa.gestion_de_clientes.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar 150 caracteres")
        String razonSocial,
        @NotBlank(message = "El NIT/CI es obligatorio")
        @Size(max = 30, message = "El NIT/CI no puede superar 30 caracteres")
        String nitCi,
        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono,
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
