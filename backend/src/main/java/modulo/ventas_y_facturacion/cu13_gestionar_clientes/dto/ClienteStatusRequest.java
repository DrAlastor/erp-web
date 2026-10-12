package modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record ClienteStatusRequest(@NotNull(message = "El estado es obligatorio") Boolean activo) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
