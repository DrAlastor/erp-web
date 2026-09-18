package modulo.inventario_y_existencia.movimiento_inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoSimpleResponse {
    private Long id;
    private String nombre;
    private String codigoSku;
    private String codigoBarra;
    private String unidadMedida;
    private BigDecimal costoPromedio;
    private Integer stockMinimo;
    private String categoriaNombre;
}
