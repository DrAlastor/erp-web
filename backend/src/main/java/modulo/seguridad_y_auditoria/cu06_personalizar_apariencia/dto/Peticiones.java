package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de las peticiones de la pantalla Personalizar Apariencia. */
public final class Peticiones {

    private static final String HEX = "^#[0-9A-Fa-f]{6}$";

    private Peticiones() {
    }

    /**
     * Apariencia completa. Se envía entera y reemplaza a la anterior; el logo se cambia
     * por su propio endpoint porque es un archivo.
     */
    public record Actualizar(
            @NotNull(message = "El tema es obligatorio")
            @Pattern(regexp = "CLARO|OSCURO", message = "El tema debe ser CLARO u OSCURO")
            String tema,

            @NotNull(message = "El color primario es obligatorio")
            @Pattern(regexp = HEX, message = "El color primario debe tener el formato #RRGGBB")
            String colorPrimario,

            @NotNull(message = "El color secundario es obligatorio")
            @Pattern(regexp = HEX, message = "El color secundario debe tener el formato #RRGGBB")
            String colorSecundario,

            @NotNull(message = "El color de acento es obligatorio")
            @Pattern(regexp = HEX, message = "El color de acento debe tener el formato #RRGGBB")
            String colorAcento,

            @NotNull(message = "El tamaño de fuente es obligatorio")
            @Pattern(regexp = "PEQUENO|NORMAL|GRANDE", message = "El tamaño de fuente debe ser PEQUENO, NORMAL o GRANDE")
            String tamanoFuente,

            @NotNull(message = "La tipografía es obligatoria")
            @Pattern(regexp = "INTER|ROBOTO|POPPINS|OPEN_SANS", message = "La tipografía no es válida")
            String tipografia,

            @NotNull(message = "El estilo de bordes es obligatorio")
            @Pattern(regexp = "RECTOS|SUAVES|REDONDEADOS", message = "Los bordes deben ser RECTOS, SUAVES o REDONDEADOS")
            String bordes,

            @Size(max = 500, message = "El encabezado de factura admite hasta 500 caracteres")
            String encabezadoFactura,

            @Size(max = 500, message = "El pie de factura admite hasta 500 caracteres")
            String pieFactura) {
    }
}
