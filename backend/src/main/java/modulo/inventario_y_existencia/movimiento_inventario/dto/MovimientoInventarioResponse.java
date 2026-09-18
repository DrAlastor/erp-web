package modulo.inventario_y_existencia.movimiento_inventario.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private String unidadMedida;
    private Integer almacenId;
    private String almacenNombre;
    private String tipoMovimiento;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal saldoCantidad;
    private BigDecimal saldoValorado;
    private String referenciaDoc;
    private String motivo;
    private LocalDateTime fecha;
    private String creadoPor;
}
