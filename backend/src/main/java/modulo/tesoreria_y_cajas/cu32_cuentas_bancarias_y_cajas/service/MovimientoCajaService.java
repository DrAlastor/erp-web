package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.MovimientoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.RegistrarMovimientoRequest;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.mapper.CajaMapper;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.TurnoCajaService.Actor;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * HU-34 / CU34: registrar movimientos de caja (ingresos y egresos) en un turno ABIERTO.
 *
 * <p>Bloquea el turno (pesimista) para que movimientos y cierre no se pisen. Forma de pago
 * inexistente o inactiva → 422. Un EGRESO en EFECTIVO no puede superar el efectivo
 * disponible (409). Los movimientos no se editan ni eliminan: se corrigen con uno contrario.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientoCajaService {

    private final TurnoCajaRepository turnos;
    private final MovimientoCajaRepository movimientos;
    private final FormaPagoRepository formasPago;
    private final TurnoCajaService turnoService;
    private final CajaMapper mapper;
    private final CajaConsultaRepository consultas;
    private final BitacoraCaja bitacora;

    public List<MovimientoResponse> listar(Long turnoId, Authentication authentication) {
        turnoService.filaAccesible(turnoId, turnoService.actor(authentication));
        return listarDeTurno(turnoId);
    }

    /**
     * Movimientos manuales y cobros de ventas del turno, del más reciente al más antiguo, sin
     * control de acceso. Las ventas no se copian a movimientos_caja (el arqueo ya las suma desde
     * pago_venta); aquí solo se listan para que el cajero vea lo que vendió.
     */
    public List<MovimientoResponse> listarDeTurno(Long turnoId) {
        return Stream.concat(
                        movimientos.findByTurnoCajaIdOrderByFechaHoraDescIdDesc(turnoId).stream().map(mapper::toResponse),
                        consultas.ventasDelTurno(turnoId).stream())
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    private static final Comparator<MovimientoResponse> MAS_RECIENTE_PRIMERO = Comparator
            .comparing(MovimientoResponse::fechaHora, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MovimientoResponse::id, Comparator.reverseOrder());

    @Transactional
    public MovimientoResponse registrar(Long turnoId, RegistrarMovimientoRequest request, Authentication authentication) {
        Actor actor = turnoService.actor(authentication);
        TurnoCaja turno = turnos.bloquear(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno de caja #" + turnoId + " inexistente"));
        ReglasCaja.validarAcceso(turno.getCajeroId(), actor.usuarioId(), actor.admin());
        ReglasCaja.validarAbierto(turnoId, turno.estaAbierto());
        FormaPago forma = formasPago.findById(request.formaPagoId())
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "La forma de pago no existe o está inactiva"));
        String concepto = request.concepto().trim();
        if (concepto.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El concepto debe tener entre 3 y 255 caracteres");
        }
        String tipo = request.tipo().trim().toUpperCase(Locale.ROOT);
        BigDecimal monto = CalculadoraArqueo.escala(request.monto());
        if (MovimientoCaja.TIPO_EGRESO.equals(tipo) && forma.esEfectivo()) {
            ReglasCaja.validarEgresoEfectivo(monto, turnoService.respuesta(turnoId).efectivoEsperado());
        }
        String comprobante = TurnoCajaService.limpiar(request.numeroComprobante());
        if (comprobante == null) {
            comprobante = ReglasCaja.numeroComprobante(turnoId, movimientos.countByTurnoCajaId(turnoId) + 1);
        }
        MovimientoCaja movimiento = movimientos.saveAndFlush(MovimientoCaja.builder()
                .turnoCajaId(turnoId)
                .formaPago(forma)
                .tipo(tipo)
                .concepto(concepto)
                .monto(monto)
                .numeroComprobante(comprobante)
                .fechaHora(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build());
        bitacora.registrar("MOVIMIENTO_CAJA",
                String.format("%s de Bs %s (%s) en el turno de caja #%d: %s.",
                        tipo, monto.toPlainString(), forma.getNombre(), turnoId, concepto),
                String.format("{\"turnoId\":%d,\"movimientoId\":%d,\"tipo\":\"%s\",\"formaPagoId\":%d,\"monto\":%s,\"comprobante\":\"%s\"}",
                        turnoId, movimiento.getId(), tipo, forma.getId(), monto.toPlainString(),
                        comprobante.replace("\"", "'")));
        return mapper.toResponse(movimiento);
    }
}
