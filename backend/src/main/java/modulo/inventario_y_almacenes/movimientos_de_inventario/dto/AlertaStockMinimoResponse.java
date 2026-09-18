package modulo.inventario_y_almacenes.movimientos_de_inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaStockMinimoResponse {
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private String unidadMedida;
    private Integer almacenId;
    private String almacenNombre;
    private BigDecimal cantidadActual;
    private Integer stockMinimo;
    private BigDecimal deficit;
    private String nivelCriticidad; // AGOTADO, CRITICO, ALERTA
}
