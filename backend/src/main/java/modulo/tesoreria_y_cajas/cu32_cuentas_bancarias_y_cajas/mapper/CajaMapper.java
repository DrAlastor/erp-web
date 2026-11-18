package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.mapper;

import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.MovimientoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository.TurnoFila;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import org.springframework.stereotype.Component;

/** Traduce turnos y movimientos de caja a los DTOs de la API; el esperado sale de {@link CalculadoraArqueo}. */
@Component("movimientosCajaMapper")
public class CajaMapper {

    public TurnoResponse toResponse(TurnoFila t) {
        return new TurnoResponse(
                t.id(),
                t.sucursalId(),
                t.sucursalNombre(),
                t.cajeroId(),
                t.cajeroNombre(),
                CalculadoraArqueo.escala(t.montoApertura()),
                t.montoCierre(),
                t.diferencia(),
                t.fechaApertura(),
                t.fechaCierre(),
                t.estado(),
                t.observaciones(),
                CalculadoraArqueo.efectivoEsperado(
                        t.montoApertura(), t.ventasEfectivo(), t.ingresosEfectivo(), t.egresosEfectivo()));
    }

    public MovimientoResponse toResponse(MovimientoCaja m) {
        return new MovimientoResponse(
                m.getId(),
                m.getTurnoCajaId(),
                m.getTipo(),
                m.getFormaPago().getId(),
                m.getFormaPago().getNombre(),
                m.getConcepto(),
                m.getMonto(),
                m.getNumeroComprobante(),
                m.getFechaHora());
    }
}
