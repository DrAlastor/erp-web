package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.contabilidad_y_finanzas.compartido.PermisoFuncionContable;
import modulo.contabilidad_y_finanzas.compartido.UsuarioContableResolver;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CobroResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CuentaPorCobrarResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CuentaPorCobrarResumenResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.EstadoCuentaClienteResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.OpcionResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.RegistrarCobroRequest;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.RegistrarCobroResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.ResumenCxcResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.entity.Cobro;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository.CobroRepository;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository.CuentaCobroRepository;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository.CuentaPorCobrarConsultaRepository;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository.CuentaPorCobrarConsultaRepository.CuentaFila;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.CuentaPorCobrar;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService.Partida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HU-29 / CU29: gestionar cuentas por cobrar y cobros.
 *
 * <p>Las cuotas nacen en la CU19 al emitir una venta a crédito; aquí se consultan y se
 * cobran. Cada cobro bloquea la cuota, no puede superar el saldo, recibe un recibo
 * correlativo RC-AAAA-NNNN, recalcula cobrado, saldo y estado (PENDIENTE, PARCIAL,
 * PAGADO) y genera el asiento Caja/Bancos contra Cuentas por Cobrar Clientes. Si quien
 * cobra tiene un turno de caja abierto en la sucursal de la venta, el cobro se liga al
 * turno y queda como INGRESO de caja para el arqueo. No hay borrado ni anulación de cobros.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuentaPorCobrarService {

    private static final int MAX_OPCIONES = 20;
    private static final int TOP_CLIENTES = 5;

    private final CuentaCobroRepository cuentas;
    private final CobroRepository cobros;
    private final CuentaPorCobrarConsultaRepository consultas;
    private final FormaPagoRepository formasPago;
    private final TurnoCajaRepository turnos;
    private final MovimientoCajaRepository movimientosCaja;
    private final ContabilidadVentaService contabilidad;
    private final UsuarioContableResolver usuarioResolver;
    private final PermisoFuncionContable permisos;
    private final BitacoraAuditoriaService bitacora;

    public Page<CuentaPorCobrarResumenResponse> list(String search, String estado, Long clienteId, int page, int size) {
        if (estado != null && !estado.isBlank()
                && !EstadoCuentaPorCobrar.FILTROS.contains(estado.trim().toUpperCase(Locale.ROOT))) {
            throw rechazo("Estado no válido: " + estado);
        }
        LocalDate hoy = LocalDate.now();
        long total = consultas.contar(search, estado, clienteId, hoy);
        List<CuentaPorCobrarResumenResponse> filas = consultas
                .listar(search, estado, clienteId, hoy, size, (long) page * size).stream()
                .map(f -> resumen(f, hoy)).toList();
        return new PageImpl<>(filas, PageRequest.of(page, size), total);
    }

    public ResumenCxcResponse resumen() {
        LocalDate hoy = LocalDate.now();
        var t = consultas.totales(hoy, hoy.plusDays(7));
        BigDecimal[] tramos = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (var s : consultas.saldosVencidos(hoy)) {
            int i = EstadoCuentaPorCobrar.tramo(java.time.temporal.ChronoUnit.DAYS.between(s.fechaVencimiento(), hoy));
            tramos[i] = tramos[i].add(s.saldo());
        }
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        return new ResumenCxcResponse(
                EstadoCuentaPorCobrar.escala(t.saldoTotal()),
                EstadoCuentaPorCobrar.escala(t.vencido()),
                EstadoCuentaPorCobrar.escala(t.porVencer()),
                EstadoCuentaPorCobrar.escala(consultas.cobrado(inicioMes, inicioMes.plusMonths(1))),
                t.cantidad(),
                t.vencidas(),
                List.of(tramos).stream().map(EstadoCuentaPorCobrar::escala).toList(),
                consultas.topClientes(TOP_CLIENTES).stream()
                        .map(o -> new ResumenCxcResponse.ClienteSaldo(o.id(), o.nombre(),
                                EstadoCuentaPorCobrar.escala(new BigDecimal(o.detalle()))))
                        .toList());
    }

    public CuentaPorCobrarResponse detail(Long id) {
        return respuesta(id);
    }

    @Transactional
    public RegistrarCobroResponse registrarCobro(Long id, RegistrarCobroRequest request, Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        permisos.exigirEdicion(usuarioId, PermisoFuncionContable.CUENTAS_POR_COBRAR);
        CuentaPorCobrar cuenta = cuentas.findParaCobro(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar inexistente"));
        if (!EstadoCuentaPorCobrar.tieneDeuda(cuenta.getEstado(), cuenta.getSaldo())) {
            throw conflicto("La cuota no tiene saldo por cobrar (estado " + cuenta.getEstado() + ").");
        }
        BigDecimal monto = EstadoCuentaPorCobrar.escala(request.monto());
        BigDecimal saldo = EstadoCuentaPorCobrar.escala(cuenta.getSaldo());
        if (monto.compareTo(saldo) > 0) {
            throw conflicto("El monto (" + monto.toPlainString() + ") supera el saldo pendiente ("
                    + saldo.toPlainString() + ").");
        }
        FormaPago forma = formasPago.findById(request.formaPagoId())
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .orElseThrow(() -> rechazo("La forma de pago no existe o está inactiva."));
        if (forma.esPasarelaOnline()) {
            throw rechazo("La pasarela en línea solo se usa al vender; registre el cobro con otra forma de pago.");
        }
        LocalDate fecha = request.fechaCobro() == null ? LocalDate.now() : request.fechaCobro();
        TurnoCaja turno = turnos.findFirstBySucursalIdAndCajeroIdAndEstadoOrderByFechaAperturaDesc(
                cuenta.getSucursalId(), usuarioId, TurnoCaja.ESTADO_ABIERTO).orElse(null);

        consultas.bloquearNumeracionRecibos();
        Cobro cobro = new Cobro();
        cobro.setCuentaPorCobrarId(cuenta.getId());
        cobro.setFormaPagoId(forma.getId());
        cobro.setTurnoCajaId(turno == null ? null : turno.getId());
        cobro.setUsuarioId(usuarioId);
        cobro.setNumeroRecibo(siguienteRecibo(fecha.getYear()));
        cobro.setTransaccionExterna(limpiar(request.transaccionExterna()));
        cobro.setMonto(monto);
        cobro.setFechaCobro(fecha);
        cobro.setObservaciones(limpiar(request.observaciones()));
        cobro = cobros.saveAndFlush(cobro);

        cuenta.setMontoCobrado(EstadoCuentaPorCobrar.escala(cuenta.getMontoCobrado()).add(monto));
        cuenta.setSaldo(saldo.subtract(monto));
        cuenta.setEstado(EstadoCuentaPorCobrar.estadoSegun(cuenta.getMontoCobrado(), cuenta.getSaldo()));
        cuentas.saveAndFlush(cuenta);

        CuentaFila fila = consultas.cuenta(id).orElseThrow();
        String cuentaDebe = EstadoCuentaPorCobrar.vaACaja(forma.getCodigo())
                ? ContabilidadVentaService.CAJA : ContabilidadVentaService.BANCO;
        String glosa = "Cobro " + cobro.getNumeroRecibo() + " de " + fila.documento() + " cuota "
                + fila.numeroCuota() + " - " + fila.razonSocial();
        String asiento = contabilidad.registrarAsiento(fecha, "COBRO", glosa, usuarioId, List.of(
                Partida.debe(cuentaDebe, glosa, monto),
                Partida.haber(ContabilidadVentaService.CUENTAS_POR_COBRAR, glosa, monto)));

        if (turno != null) {
            movimientosCaja.save(MovimientoCaja.builder()
                    .turnoCajaId(turno.getId())
                    .formaPago(forma)
                    .tipo("INGRESO")
                    .concepto(glosa.length() > 255 ? glosa.substring(0, 255) : glosa)
                    .monto(monto)
                    .numeroComprobante(cobro.getNumeroRecibo())
                    .fechaHora(LocalDateTime.now())
                    .build());
        }

        auditar(cobro, fila, asiento);
        return new RegistrarCobroResponse(consultas.cobro(cobro.getId()).orElseThrow(), asiento, respuesta(id));
    }

    public EstadoCuentaClienteResponse estadoCuenta(Long clienteId) {
        var cliente = consultas.cliente(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente inexistente"));
        LocalDate hoy = LocalDate.now();
        List<CuentaPorCobrarResumenResponse> cuotas = new ArrayList<>();
        BigDecimal facturado = BigDecimal.ZERO;
        BigDecimal cobrado = BigDecimal.ZERO;
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        for (CuentaFila f : consultas.cuotasDeCliente(clienteId)) {
            CuentaPorCobrarResumenResponse r = resumen(f, hoy);
            cuotas.add(r);
            if (r.estado() != null && r.estado().toUpperCase(Locale.ROOT).startsWith("ANULAD")) continue;
            facturado = facturado.add(EstadoCuentaPorCobrar.escala(f.montoTotal()));
            cobrado = cobrado.add(EstadoCuentaPorCobrar.escala(f.montoCobrado()));
            saldo = saldo.add(EstadoCuentaPorCobrar.escala(f.saldo()));
            if (r.vencida()) vencido = vencido.add(EstadoCuentaPorCobrar.escala(f.saldo()));
        }
        return new EstadoCuentaClienteResponse(cliente.id(), cliente.razonSocial(), cliente.nitCi(),
                cliente.limiteCredito(), facturado, cobrado, saldo, vencido, cuotas);
    }

    public List<OpcionResponse> clientes(String search) {
        return consultas.clientesConCuentas(search, MAX_OPCIONES);
    }

    /** Si el usuario conectado puede registrar cobros (Edición en la función de la CU29). */
    public Map<String, Boolean> permisos(Authentication authentication) {
        return Map.of("cobrar", permisos.puedeEditar(usuarioResolver.resolver(authentication),
                PermisoFuncionContable.CUENTAS_POR_COBRAR));
    }

    public List<OpcionResponse> formasPago() {
        return formasPago.findByActivoTrueOrderByIdAsc().stream()
                .filter(f -> !f.esPasarelaOnline())
                .map(f -> new OpcionResponse(f.getId(), f.getNombre(), f.getCodigo()))
                .toList();
    }

    // ------------------------------------------------------------------ internos

    private CuentaPorCobrarResponse respuesta(Long id) {
        CuentaFila f = consultas.cuenta(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar inexistente"));
        List<CobroResponse> historial = consultas.cobros(id);
        return new CuentaPorCobrarResponse(resumen(f, LocalDate.now()), f.fechaVenta(), f.totalVenta(),
                f.telefono(), f.email(), EstadoCuentaPorCobrar.tieneDeuda(f.estado(), f.saldo()), historial);
    }

    private static CuentaPorCobrarResumenResponse resumen(CuentaFila f, LocalDate hoy) {
        return new CuentaPorCobrarResumenResponse(f.id(), f.ventaId(), f.documento(), f.clienteId(),
                f.razonSocial(), f.nitCi(), f.numeroCuota(), f.montoTotal(), f.montoCobrado(), f.saldo(),
                f.fechaVencimiento(), f.estado(),
                EstadoCuentaPorCobrar.vencida(f.estado(), f.saldo(), f.fechaVencimiento(), hoy),
                EstadoCuentaPorCobrar.diasVencida(f.estado(), f.saldo(), f.fechaVencimiento(), hoy),
                f.sucursal());
    }

    private String siguienteRecibo(int gestion) {
        String prefijo = "RC-" + gestion + "-";
        String ultimo = cobros.ultimoRecibo(prefijo + "%");
        int siguiente = 1;
        if (ultimo != null) {
            try {
                siguiente = Integer.parseInt(ultimo.substring(prefijo.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("Número de recibo con formato inesperado: {}", ultimo);
            }
        }
        return prefijo + String.format("%04d", siguiente);
    }

    private void auditar(Cobro cobro, CuentaFila fila, String asiento) {
        try {
            bitacora.registrarEventoActual("REGISTRO_COBRO", "CONTABILIDAD",
                    String.format("Cobro %s por Bs. %s de %s (cuota %d) del cliente '%s'.",
                            cobro.getNumeroRecibo(), cobro.getMonto().toPlainString(), fila.documento(),
                            fila.numeroCuota(), fila.razonSocial()),
                    "EXITO", "MEDIA",
                    String.format("{\"cobroId\":%d,\"recibo\":\"%s\",\"cuentaPorCobrarId\":%d,\"monto\":%s,\"asiento\":\"%s\"}",
                            cobro.getId(), cobro.getNumeroRecibo(), fila.id(), cobro.getMonto().toPlainString(), asiento));
        } catch (Exception e) {
            log.warn("No se pudo registrar en la bitácora: {}", e.getMessage());
        }
    }

    private static String limpiar(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
