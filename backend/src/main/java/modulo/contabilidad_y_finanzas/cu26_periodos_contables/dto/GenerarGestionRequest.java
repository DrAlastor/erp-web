package modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerarGestionRequest(
        @NotNull(message = "La gestión es obligatoria")
        @Min(value = 2000, message = "La gestión no es válida")
        @Max(value = 2100, message = "La gestión no es válida")
        Integer gestion) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
