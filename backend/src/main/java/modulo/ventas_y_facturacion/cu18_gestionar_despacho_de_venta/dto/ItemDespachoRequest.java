package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemDespachoRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero") BigDecimal cantidad) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
