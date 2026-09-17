package modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
