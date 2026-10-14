package modulo.catalogo_e_inventario.cu12_consultar_kardex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Existencias valoradas de un almacén según el método de costeo elegido. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexValoracionResponse {
    private Integer almacenId;
    private String almacenNombre;
    private String metodo;
    private String metodoEtiqueta;
    private BigDecimal totalCantidad;
    private BigDecimal totalValor;
    private List<Item> items;

    /** Existencia y valor de un producto. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long productoId;
        private String productoNombre;
        private String productoSku;
        private String unidadMedida;
        private BigDecimal cantidad;
        private BigDecimal costoUnitario;
        private BigDecimal valor;
    }
}
