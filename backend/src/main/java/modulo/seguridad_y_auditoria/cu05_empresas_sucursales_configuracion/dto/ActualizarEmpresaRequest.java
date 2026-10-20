package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarEmpresaRequest(
        @NotBlank(message = "El nombre comercial es obligatorio")
        @Size(max = 160, message = "El nombre no puede exceder los 160 caracteres")
        String nombre,

        @Size(max = 200, message = "La razón social no puede exceder los 200 caracteres")
        String razonSocial,

        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 20, message = "El NIT no puede exceder los 20 caracteres")
        String nit,

        @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
        String direccion,

        @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres")
        String telefono,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 120, message = "El correo no puede exceder los 120 caracteres")
        String email,

        @NotBlank(message = "La moneda principal es obligatoria")
        @Size(max = 10, message = "La moneda no puede exceder los 10 caracteres")
        String monedaPrincipal,

        @Size(max = 10, message = "La moneda no puede exceder los 10 caracteres")
        String monedaSecundaria,

        String logoUrl) {
}
