package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarListaPrecioRequest(
        @NotBlank(message = "El nombre de la lista es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        Boolean esPrincipal,
        Boolean activo) {}
