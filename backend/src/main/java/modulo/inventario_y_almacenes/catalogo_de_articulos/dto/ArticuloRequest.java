package modulo.inventario_y_almacenes.catalogo_de_articulos.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de un artículo del catálogo.
 *
 * <p>La categoría se envía por identificador, no como objeto, para que el cliente no pueda
 * crear categorías por accidente al guardar un artículo. Cualquier campo que no esté acá se
 * rechaza con 400.
 */
public record ArticuloRequest(
        @NotBlank @Size(max = 255) String sku,
        @NotBlank @Size(max = 255) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull @Positive(message = "El precio debe ser mayor a 0") java.math.BigDecimal precio,
        @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,
        @Size(max = 512) String imagenUrl,
        Integer categoriaId,
        Boolean activo) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
