package modulo.catalogo_e_inventario.cu12_consultar_kardex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del Kardex valorado: el movimiento repartido en columnas de entrada o salida y
 * el saldo que deja, con el costo que le asigna el método de valuación elegido.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexLineaResponse {
    private Long id;
    private LocalDateTime fecha;
    private String tipoMovimiento;
    private String referenciaDoc;
    private String motivo;
    private String creadoPor;

    private BigDecimal entradaCantidad;
    private BigDecimal entradaCostoUnitario;
    private BigDecimal entradaValor;

    private BigDecimal salidaCantidad;
    private BigDecimal salidaCostoUnitario;
    private BigDecimal salidaValor;

    private BigDecimal saldoCantidad;
    private BigDecimal saldoCostoUnitario;
    private BigDecimal saldoValor;
}
