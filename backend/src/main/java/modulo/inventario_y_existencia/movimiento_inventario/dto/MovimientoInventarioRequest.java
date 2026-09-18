package modulo.inventario_y_existencia.movimiento_inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El ID del almacén es obligatorio")
    private Integer almacenId;

    @NotBlank(message = "El tipo de movimiento es obligatorio (ENTRADA, SALIDA, AJUSTE_POSITIVO, AJUSTE_NEGATIVO, AJUSTE)")
    private String tipoMovimiento;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser estrictamente mayor a cero")
    private BigDecimal cantidad;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
    private BigDecimal costoUnitario;

    @Size(max = 100, message = "La referencia del documento no puede exceder 100 caracteres")
    private String referenciaDoc;

    @Size(max = 255, message = "El motivo o justificación no puede exceder 255 caracteres")
    private String motivo;

    private LocalDateTime fecha;
}
