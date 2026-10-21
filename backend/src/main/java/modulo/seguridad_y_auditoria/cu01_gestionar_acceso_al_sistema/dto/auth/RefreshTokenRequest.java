package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "El código de empresa es obligatorio")
    @Size(max = 30, message = "El código de empresa no puede superar 30 caracteres")
    private String codigoEmpresa;

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
