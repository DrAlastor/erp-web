package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Un renglón de la orden: producto, cantidad y costo unitario pactado con el proveedor. */
public record ItemOrdenCompraRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero") BigDecimal cantidad,
        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a cero") BigDecimal costoUnitario) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
