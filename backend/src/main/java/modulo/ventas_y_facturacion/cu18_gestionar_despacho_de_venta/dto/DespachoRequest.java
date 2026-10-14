package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Datos para registrar una nota de despacho de un pedido aprobado. */
public record DespachoRequest(
        @NotNull(message = "El pedido es obligatorio") Integer pedidoId,
        @NotNull(message = "La sucursal que despacha es obligatoria") Integer sucursalId,
        @Size(max = 100, message = "El conductor no puede exceder 100 caracteres") String conductor,
        @Size(max = 20, message = "La placa no puede exceder 20 caracteres") String placa,
        @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres") String observaciones,
        @NotEmpty(message = "El despacho debe incluir al menos un producto")
        @Size(max = 100, message = "Un despacho admite hasta 100 renglones")
        List<@Valid @NotNull ItemDespachoRequest> items) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
