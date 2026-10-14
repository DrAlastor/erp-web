package modulo.catalogo_e_inventario.cu12_consultar_kardex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kardex valorado de un producto en un almacén para un rango de fechas: saldo inicial,
 * movimientos del periodo, totales y saldo final.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexConsultaResponse {
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private String unidadMedida;
    private Integer almacenId;
    private String almacenNombre;

    private String metodo;
    private String metodoEtiqueta;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    private BigDecimal saldoInicialCantidad;
    private BigDecimal saldoInicialValor;

    private BigDecimal totalEntradasCantidad;
    private BigDecimal totalEntradasValor;
    private BigDecimal totalSalidasCantidad;
    private BigDecimal totalSalidasValor;

    private BigDecimal saldoFinalCantidad;
    private BigDecimal saldoFinalCostoUnitario;
    private BigDecimal saldoFinalValor;

    private List<KardexLineaResponse> movimientos;
}
