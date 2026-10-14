package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import jakarta.validation.constraints.NotNull;

/** Sucursal que atenderá el pedido generado a partir de la cotización. */
public record ConvertirCotizacionRequest(
        @NotNull(message = "La sucursal es obligatoria") Integer sucursalId) {
}
