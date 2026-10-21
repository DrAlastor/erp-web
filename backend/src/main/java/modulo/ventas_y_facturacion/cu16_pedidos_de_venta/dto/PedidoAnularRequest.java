package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedidoAnularRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo) {}
