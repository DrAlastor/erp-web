package modulo.inventario_y_almacenes.movimientos_de_inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlmacenResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private String codigoBarra;
    private String unidadMedida;
    private BigDecimal precioVenta;
    private BigDecimal costoPromedio;
    private Integer stockMinimo;
    private Integer almacenId;
    private String almacenNombre;
    private BigDecimal cantidadActual;
    private Boolean bajoStockMinimo;
}
