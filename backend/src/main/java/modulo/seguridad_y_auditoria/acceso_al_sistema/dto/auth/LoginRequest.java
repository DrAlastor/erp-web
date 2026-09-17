package modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El usuario o correo es obligatorio")
    private String usernameOrEmail;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
