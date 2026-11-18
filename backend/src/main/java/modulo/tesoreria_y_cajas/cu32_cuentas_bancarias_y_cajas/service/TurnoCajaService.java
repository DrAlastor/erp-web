package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.AbrirTurnoRequest;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.OpcionesCajaResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.mapper.CajaMapper;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository.CajaConsultaRepository.TurnoFila;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.UsuarioActualResolver;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * HU-34 / CU32-CU34: apertura mínima del turno de caja y acceso a turnos.
 *
 * <p>Un usuario abre un turno en una sucursal activa con un fondo ≥ 0 y no puede tener dos
 * turnos ABIERTO (409). La apertura no crea movimiento: el fondo es {@code monto_apertura}.
 * Solo el cajero dueño o un administrador accede a un turno (403). La HU-35 reutiliza
 * {@link #actor} y {@link #filaAccesible} para el arqueo, el cierre y la consulta.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnoCajaService {

    private final TurnoCajaRepository turnos;
    private final FormaPagoRepository formasPago;
    private final CajaConsultaRepository consultas;
    private final CajaMapper mapper;
    private final UsuarioActualResolver usuarioResolver;
    private final BitacoraCaja bitacora;

    /** Usuario autenticado y si tiene rol de administrador. */
    public record Actor(Long usuarioId, boolean admin) {}

    public Actor actor(Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        return new Actor(usuarioId, consultas.esAdministrador(usuarioId));
    }

    public OpcionesCajaResponse opciones() {
        return new OpcionesCajaResponse(
                consultas.sucursalesActivas(),
                formasPago.findByActivoTrueOrderByIdAsc().stream()
                        .map(f -> new OpcionesCajaResponse.FormaPagoOpcion(f.getId(), f.getCodigo(), f.getNombre()))
                        .toList());
    }

    public Optional<TurnoResponse> turnoActual(Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        return turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(usuarioId, TurnoCaja.ESTADO_ABIERTO)
                .map(t -> respuesta(t.getId()));
    }

    @Transactional
    public TurnoResponse abrir(AbrirTurnoRequest request, Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        consultas.bloquearAperturaDe(usuarioId);
        turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(usuarioId, TurnoCaja.ESTADO_ABIERTO)
                .ifPresent(t -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya tienes abierto el turno de caja #" + t.getId() + "; ciérralo antes de abrir otro");
                });
        if (!consultas.sucursalActiva(request.sucursalId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "La sucursal no existe o está inactiva");
        }
        TurnoCaja turno = turnos.saveAndFlush(TurnoCaja.builder()
                .sucursalId(request.sucursalId())
                .cajeroId(usuarioId)
                .montoApertura(CalculadoraArqueo.escala(request.montoApertura()))
                .fechaApertura(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .estado(TurnoCaja.ESTADO_ABIERTO)
                .observaciones(limpiar(request.observaciones()))
                .build());
        bitacora.registrar("APERTURA_CAJA",
                "Apertura del turno de caja #" + turno.getId() + " con fondo de Bs "
                        + turno.getMontoApertura().toPlainString() + ".",
                String.format("{\"turnoId\":%d,\"sucursalId\":%d,\"montoApertura\":%s}",
                        turno.getId(), turno.getSucursalId(), turno.getMontoApertura().toPlainString()));
        return respuesta(turno.getId());
    }

    /** Turno visible para el actor: 404 si no existe, 403 si no es suyo ni es administrador. */
    public TurnoFila filaAccesible(Long turnoId, Actor actor) {
        TurnoFila fila = fila(turnoId);
        ReglasCaja.validarAcceso(fila.cajeroId(), actor.usuarioId(), actor.admin());
        return fila;
    }

    public TurnoFila fila(Long turnoId) {
        return consultas.turno(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno de caja #" + turnoId + " inexistente"));
    }

    public TurnoResponse respuesta(Long turnoId) {
        return mapper.toResponse(fila(turnoId));
    }

    public static String limpiar(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
