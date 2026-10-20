package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** clave -> nuevo valor. Solo se aceptan las claves del catalogo de parametros. */
public record GuardarConfiguracionRequest(
        @NotEmpty(message = "Debe enviar al menos un parámetro")
        Map<String, String> parametros) {
}
