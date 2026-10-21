package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para guardar o actualizar una plantilla QBE. */
public record PlantillaQbeRequest(
        @NotBlank(message = "Ponle un nombre a la plantilla.")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
        String descripcion,

        @NotNull(message = "La plantilla necesita la definición del reporte.")
        @Valid
        DefinicionQbe definicion) {
}
