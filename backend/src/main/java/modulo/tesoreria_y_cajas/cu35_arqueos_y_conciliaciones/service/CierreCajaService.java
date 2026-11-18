package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.mapper.CajaMapper;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository.FiltroTurnos;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository.TurnoFila;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.BitacoraCaja;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.MovimientoCajaService;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.ReglasCaja;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.TurnoCajaService;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.TurnoCajaService.Actor;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.ArqueoResponse;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.CerrarTurnoRequest;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.DetalleTurnoResponse;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * HU-35 / CU35: arqueo, cierre y consulta de turnos de caja.
 *
 * <p>El arqueo se arma con {@link CalculadoraArqueo} sobre las partidas del turno. El cierre
 * bloquea el turno, exige que siga ABIERTO (409) y que sea del usuario o de un admin (403),
 * calcula la diferencia con {@link ReglasCierreCaja} y guarda monto_cierre, diferencia,
 * fecha_cierre y estado CERRADO. En el historial un admin ve todos los turnos; el resto, solo los suyos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CierreCajaService {

    private static final Set<String> ESTADOS = Set.of(TurnoCaja.ESTADO_ABIERTO, TurnoCaja.ESTADO_CERRADO);

    private final TurnoCajaRepository turnos;
    private final MovimientoCajaRepository movimientos;
    private final CajaConsultaRepository consultas;
    private final TurnoCajaService turnoService;
    private final MovimientoCajaService movimientoService;
    private final CajaMapper mapper;
    private final BitacoraCaja bitacora;

    public ArqueoResponse arqueo(Long turnoId, Authentication authentication) {
        return arqueoDe(turnoService.filaAccesible(turnoId, turnoService.actor(authentication)));
    }

    @Transactional
    public TurnoResponse cerrar(Long turnoId, CerrarTurnoRequest request, Authentication authentication) {
        Actor actor = turnoService.actor(authentication);
        TurnoCaja turno = turnos.bloquear(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno de caja #" + turnoId + " inexistente"));
        ReglasCaja.validarAcceso(turno.getCajeroId(), actor.usuarioId(), actor.admin());
        ReglasCaja.validarAbierto(turnoId, turno.estaAbierto());

        var esperado = turnoService.respuesta(turnoId).efectivoEsperado();
        var cierre = ReglasCierreCaja.evaluar(esperado, request.montoContado(), request.observaciones());
        turno.setMontoCierre(cierre.montoContado());
        turno.setDiferencia(cierre.diferencia());
        turno.setFechaCierre(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        turno.setEstado(TurnoCaja.ESTADO_CERRADO);
        turno.setObservaciones(ReglasCierreCaja.observacionesFinales(turno.getObservaciones(), cierre.observaciones()));
        turnos.saveAndFlush(turno);

        bitacora.registrar("CIERRE_CAJA",
                String.format("Cierre del turno de caja #%d: esperado Bs %s, contado Bs %s (%s).",
                        turnoId, esperado.toPlainString(), cierre.montoContado().toPlainString(),
                        ReglasCierreCaja.descripcion(cierre.diferencia())),
                String.format("{\"turnoId\":%d,\"esperado\":%s,\"contado\":%s,\"diferencia\":%s}",
                        turnoId, esperado.toPlainString(), cierre.montoContado().toPlainString(),
                        cierre.diferencia().toPlainString()));
        return turnoService.respuesta(turnoId);
    }

    public Page<TurnoResponse> historial(Integer sucursalId, String estado, LocalDate desde, LocalDate hasta,
                                         int page, int size, Authentication authentication) {
        String estadoNormalizado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(Locale.ROOT);
        if (estadoNormalizado != null && !ESTADOS.contains(estadoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser ABIERTO o CERRADO");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        Actor actor = turnoService.actor(authentication);
        FiltroTurnos filtro = new FiltroTurnos(
                actor.admin() ? null : actor.usuarioId(), sucursalId, estadoNormalizado, desde, hasta);
        List<TurnoResponse> filas = consultas.listarTurnos(filtro, size, (long) page * size).stream()
                .map(mapper::toResponse)
                .toList();
        return new PageImpl<>(filas, PageRequest.of(page, size, Sort.by("fechaApertura").descending()),
                consultas.contarTurnos(filtro));
    }

    public DetalleTurnoResponse detalle(Long turnoId, Authentication authentication) {
        TurnoFila fila = turnoService.filaAccesible(turnoId, turnoService.actor(authentication));
        return new DetalleTurnoResponse(mapper.toResponse(fila), arqueoDe(fila), movimientoService.listarDeTurno(turnoId));
    }

    private ArqueoResponse arqueoDe(TurnoFila fila) {
        CalculadoraArqueo.Arqueo a = CalculadoraArqueo.calcular(fila.montoApertura(), consultas.partidasArqueo(fila.id()));
        boolean cerrado = TurnoCaja.ESTADO_CERRADO.equalsIgnoreCase(fila.estado());
        return new ArqueoResponse(
                fila.id(),
                a.montoApertura(),
                a.ventasEfectivo(),
                a.ingresosEfectivo(),
                a.egresosEfectivo(),
                a.efectivoEsperado(),
                a.totalIngresos(),
                a.totalEgresos(),
                movimientos.countByTurnoCajaId(fila.id()),
                a.porFormaPago().stream()
                        .map(l -> new ArqueoResponse.FormaPagoArqueo(
                                l.formaPagoId(), l.nombre(), l.ingresos(), l.egresos(), l.ventas()))
                        .toList(),
                cerrado ? fila.montoCierre() : null,
                cerrado ? fila.diferencia() : null);
    }
}
