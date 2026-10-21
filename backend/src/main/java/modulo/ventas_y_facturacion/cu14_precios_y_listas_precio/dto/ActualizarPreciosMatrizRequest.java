package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ActualizarPreciosMatrizRequest(
        @NotEmpty(message = "Debe enviar al menos un producto para actualizar precios")
        List<@Valid ItemActualizarPrecio> items) {

    public record ItemActualizarPrecio(
            @NotNull(message = "El ID del producto es obligatorio")
            Long productoId,

            @NotNull(message = "El precio es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
            BigDecimal precio) {}
}
