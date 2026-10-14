package modulo.catalogo_e_inventario.cu12_consultar_kardex.mapper;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexLineaResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.KardexCalculador.Paso;
import org.springframework.stereotype.Component;

/** Traduce los pasos calculados del Kardex a las filas que ve el usuario. */
@Component
public class KardexMapper {

    public KardexLineaResponse toLinea(Paso paso) {
        KardexMovimiento m = paso.movimiento();
        KardexLineaResponse.KardexLineaResponseBuilder fila = KardexLineaResponse.builder()
                .id(m.getId())
                .fecha(m.getFecha())
                .tipoMovimiento(m.getTipoMovimiento())
                .referenciaDoc(m.getReferenciaDoc())
                .motivo(m.getMotivo())
                .creadoPor(m.getCreadoPor())
                .saldoCantidad(paso.saldoCantidad())
                .saldoCostoUnitario(paso.saldoCostoUnitario())
                .saldoValor(paso.saldoValor());
        if (paso.entrada()) {
            fila.entradaCantidad(paso.cantidad())
                    .entradaCostoUnitario(paso.costoUnitario())
                    .entradaValor(paso.valor());
        } else {
            fila.salidaCantidad(paso.cantidad())
                    .salidaCostoUnitario(paso.costoUnitario())
                    .salidaValor(paso.valor());
        }
        return fila.build();
    }
}
