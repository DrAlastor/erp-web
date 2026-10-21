package comun.saas.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record GenerarBackupRequest(
        @JsonProperty("empresaId")
        @NotNull(message = "El id de la empresa es obligatorio")
        Integer empresaId
) {}
