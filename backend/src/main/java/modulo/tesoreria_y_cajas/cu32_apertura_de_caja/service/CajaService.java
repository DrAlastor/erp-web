package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.service;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.AperturaCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.EstadoCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.mapper.CajaMapper;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.repository.CajaConsultaRepository;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.PagoVentaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository.SecuenciaFacturacionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CU32 - Apertura de Caja (tabla TURNOS_CAJA). Un cajero tiene a lo sumo un turno abierto;
 * mientras esté abierto, las ventas del POS (CU33) se registran en él. También expone el
 * resumen del turno y su cierre con arqueo.
 */
@Service
@RequiredArgsConstructor
public class CajaService {

    public static final String ESTADO_CERRADO = "CERRADO";
    private static final String TIPO_INGRESO = "INGRESO";

    private final AlmacenRepository almacenRepository;
    private final TurnoCajaRepository turnoRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final FormaPagoRepository formaPagoRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final SecuenciaFacturacionRepository secuenciaRepository;
    private final CajaConsultaRepository consultaRepository;
    private final CajaMapper mapper;

    @Transactional(readOnly = true)
    public EstadoCajaResponse estado(Integer sucursalId) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        List<EstadoCajaResponse.Sucursal> sucursales = almacenRepository.findByActivoTrue().stream()
                .map(mapper::toSucursal).toList();

        TurnoCaja turno = sucursalId != null
                ? turnoRepository.findFirstBySucursalIdAndCajeroIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalId, usuarioId, TurnoCaja.ESTADO_ABIERTO).orElse(null)
                // Sin sucursal indicada se devuelve el turno abierto del cajero, esté donde esté.
                : turnoRepository.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(
                        usuarioId, TurnoCaja.ESTADO_ABIERTO).orElse(null);

        EstadoCajaResponse.TurnoActivo turnoActivo = null;
        EstadoCajaResponse.ResumenTurno resumen = null;
        if (turno != null) {
            String nombreSucursal = almacenRepository.findById(turno.getSucursalId())
                    .map(Almacen::getNombre).orElse("Sucursal #" + turno.getSucursalId());
            turnoActivo = mapper.toTurno(turno, nombreSucursal);
            resumen = resumir(turno);
        }

        List<EstadoCajaResponse.FormaPago> formas = formaPagoRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(mapper::toFormaPago).toList();
        boolean facturaDisponible = !secuenciaRepository.vigentes(LocalDate.now(), PageRequest.of(0, 1)).isEmpty();
        return new EstadoCajaResponse(turnoActivo, sucursales, formas, resumen, facturaDisponible);
    }

    @Transactional
    public EstadoCajaResponse.TurnoActivo abrir(AperturaCajaRequest request) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        Almacen sucursal = almacenRepository.findById(request.sucursalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada."));
        if (!Boolean.TRUE.equals(sucursal.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sucursal está inactiva.");
        }
        turnoRepository.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(usuarioId, TurnoCaja.ESTADO_ABIERTO)
                .ifPresent(abierto -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya tienes abierto el turno #" + abierto.getId() + "; ciérralo antes de abrir otro.");
                });
        TurnoCaja turno = turnoRepository.save(TurnoCaja.builder()
                .sucursalId(request.sucursalId())
                .cajeroId(usuarioId)
                .montoApertura(request.montoApertura())
                .fechaApertura(LocalDateTime.now())
                .estado(TurnoCaja.ESTADO_ABIERTO)
                .observaciones(limpiar(request.observaciones()))
                .build());
        return mapper.toTurno(turno, sucursal.getNombre());
    }

    @Transactional
    public CierreCajaResponse cerrar(Long turnoId, CierreCajaRequest request) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        TurnoCaja turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno de caja no encontrado."));
        if (!turno.getCajeroId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cajero dueño puede cerrar el turno.");
        }
        if (!turno.estaAbierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya está cerrado.");
        }

        EstadoCajaResponse.ResumenTurno resumen = resumir(turno);
        turno.setMontoCierre(request.montoCierre());
        turno.setDiferencia(CalculadoraArqueo.diferencia(request.montoCierre(), resumen.montoEsperado()));
        turno.setFechaCierre(LocalDateTime.now());
        turno.setEstado(ESTADO_CERRADO);
        String obs = limpiar(request.observaciones());
        if (obs != null) {
            turno.setObservaciones(obs);
        }
        return mapper.toCierre(turnoRepository.save(turno), resumen);
    }

    private EstadoCajaResponse.ResumenTurno resumir(TurnoCaja turno) {
        BigDecimal ventasTotales = safe(pagoVentaRepository.sumarCobrosPorTurno(turno.getId()));
        BigDecimal ventasEfectivo = safe(pagoVentaRepository.sumarCobrosEfectivoPorTurno(turno.getId()));
        BigDecimal cobrosCxc = safe(consultaRepository.cobrosCxcEfectivo(turno.getId()));

        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (MovimientoCaja movimiento : movimientoRepository.findByTurnoCajaIdOrderByFechaHoraAsc(turno.getId())) {
            FormaPago pago = movimiento.getFormaPago();
            if (pago == null || !pago.esEfectivo()) continue;
            if (TIPO_INGRESO.equalsIgnoreCase(movimiento.getTipo())) ingresos = ingresos.add(movimiento.getMonto());
            if (MovimientoCaja.TIPO_EGRESO.equalsIgnoreCase(movimiento.getTipo())) egresos = egresos.add(movimiento.getMonto());
        }
        BigDecimal esperado = CalculadoraArqueo.esperado(turno.getMontoApertura(), ventasEfectivo, cobrosCxc, ingresos, egresos);
        return new EstadoCajaResponse.ResumenTurno(
                turno.getMontoApertura(), ventasTotales, ventasEfectivo.add(cobrosCxc),
                ventasTotales.subtract(ventasEfectivo), ingresos, egresos, esperado, LocalDateTime.now());
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
