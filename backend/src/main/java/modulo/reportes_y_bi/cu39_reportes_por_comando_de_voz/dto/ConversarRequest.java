package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mensaje del usuario al asistente de reportes, ya transcrito y confirmado. El historial y
 * el último reporte permiten preguntas de seguimiento ("¿y en La Paz?").
 */
public record ConversarRequest(
        @NotBlank(message = "Dicta o escribe la consulta que deseas realizar.")
        @Size(max = 500, message = "La consulta no puede superar los 500 caracteres.")
        String mensaje,

        @Size(max = 20, message = "El historial no puede superar 20 mensajes.")
        List<@Valid Turno> historial,

        ConsultaReporteVoz consultaAnterior) {

    public record Turno(
            @Pattern(regexp = "usuario|asistente", message = "El rol debe ser 'usuario' o 'asistente'.")
            String rol,
            @Size(max = 2000, message = "Un mensaje del historial no puede superar 2000 caracteres.")
            String texto) {}
}
