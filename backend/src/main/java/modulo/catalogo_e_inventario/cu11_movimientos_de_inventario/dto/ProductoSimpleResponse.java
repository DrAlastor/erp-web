package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto;

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
