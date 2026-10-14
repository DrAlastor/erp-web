package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearListaPrecioRequest(
        @NotBlank(message = "El código de la lista es obligatorio")
        @Size(max = 30, message = "El código no puede superar 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El código solo puede contener letras, números, guiones y guiones bajos")
        String codigo,

        @NotBlank(message = "El nombre de la lista es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        Boolean esPrincipal) {}
