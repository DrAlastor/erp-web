package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingEmpresaRequest(
        @NotBlank @Size(max = 50) String planCodigo,
        @NotBlank @Size(max = 20) String cicloFacturacion,
        @NotBlank @Size(max = 200) String razonSocial,
        @Size(max = 200) String nombreComercial,
        @NotBlank @Size(max = 30) String nit,
        @NotBlank @Size(min = 2, max = 2) String paisCodigo,
        @Size(max = 100) String ciudad,
        @Size(max = 255) String direccion,
        @Size(max = 30) String telefonoEmpresa,
        @Size(max = 10) String monedaPrincipal,
        @NotBlank @Size(max = 150) String adminNombre,
        @NotBlank @Email @Size(max = 100) String adminEmail,
        @NotBlank @Size(min = 8, max = 72) String adminPassword,
        @Size(max = 30) String adminTelefono) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
