package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarStockMinimoRequest {

    @NotNull(message = "El nuevo stock mínimo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El stock mínimo no puede ser negativo")
    private BigDecimal nuevoStockMinimo;

    private Boolean aplicarATodosAlmacenes;
}
