package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.compras_y_proveedores.compartido.entity.Compra;
import modulo.compras_y_proveedores.compartido.repository.CompraRepository;
import modulo.compras_y_proveedores.compartido.repository.ProveedorRepository;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CompraSinCuentaResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResumenResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.FormaPagoOpcionResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.GenerarCuotasRequest;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ProveedorOpcionResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.RegistrarPagoRequest;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ResumenCxpResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.CuentaPorPagar;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.PagoProveedor;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.mapper.CuentaPorPagarMapper;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.CuentaPorPagarConsultaRepository;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.CuentaPorPagarConsultaRepository.Totales;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.CuentaPorPagarRepository;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.PagoProveedorRepository;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * HU-24 / CU34: cuentas por pagar a proveedores.
 *
 * <p>Las cuotas se generan desde una compra a credito no anulada (una sola vez por compra)
 * y bajan con pagos parciales o totales. Cada pago bloquea la cuota (pesimista), no puede
 * superar el saldo y recalcula monto pagado, saldo y estado (PENDIENTE, PARCIAL, PAGADA).
 * "Vencida" se deriva, no se guarda. No hay borrado ni anulacion de pagos en esta CU.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentaPorPagarService {

    private static final int MAX_OPCIONES = 20;
    private static final int TOP_PROVEEDORES = 5;
    private static final int DIAS_ENTRE_CUOTAS = 30;

    private final CuentaPorPagarRepository cuentas;
    private final PagoProveedorRepository pagos;
    private final CompraRepository compras;
    private final ProveedorRepository proveedores;
    private final FormaPagoRepository formasPago;
    private final CuentaPorPagarConsultaRepository consultas;
    private final CuentaPorPagarMapper mapper;
    private final UsuarioActualResolver usuarioResolver;

    public Page<CuentaPorPagarResumenResponse> list(
            String search, String estado, Long proveedorId, int page, int size) {
        LocalDate hoy = LocalDate.now();
        long total = consultas.contar(search, estado, proveedorId, hoy);
        List<CuentaPorPagarResumenResponse> filas = consultas
                .listar(search, estado, proveedorId, hoy, size, (long) page * size)
                .stream()
                .map(c -> mapper.toResumen(c, hoy))
                .toList();
        Sort orden = Sort.by("fechaVencimiento").ascending().and(Sort.by("id").ascending());
        return new PageImpl<>(filas, PageRequest.of(page, size, orden), total);
    }

    public ResumenCxpResponse resumen() {
        LocalDate hoy = LocalDate.now();
        Totales t = consultas.totales(hoy, hoy.plusDays(7));
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        return new ResumenCxpResponse(
                EstadoCuentaPorPagar.escala(t.saldoTotal()),
                EstadoCuentaPorPagar.escala(t.vencido()),
                EstadoCuentaPorPagar.escala(t.porVencer()),
                EstadoCuentaPorPagar.escala(consultas.pagado(inicioMes, inicioMes.plusMonths(1))),
                t.cantidadPendientes(),
                t.cantidadVencidas(),
                EstadoCuentaPorPagar.antiguedad(consultas.saldosVencidos(hoy), hoy),
                consultas.topProveedores(TOP_PROVEEDORES).stream()
                        .map(p -> new ResumenCxpResponse.ProveedorSaldo(
                                p.proveedorId(), p.razonSocial(), EstadoCuentaPorPagar.escala(p.saldo())))
                        .toList());
    }

    public CuentaPorPagarResponse detail(Long id) {
        return respuesta(id);
    }

    @Transactional
    public CuentaPorPagarResponse registrarPago(
            Long id, RegistrarPagoRequest request, Authentication authentication) {
        CuentaPorPagar cuenta = cuentas.findParaPago(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar inexistente"));
        if (CuentaPorPagar.PAGADA.equalsIgnoreCase(cuenta.getEstado()) || cuenta.getSaldo().signum() <= 0) {
            throw conflicto("La cuenta por pagar ya está pagada");
        }
        BigDecimal monto = EstadoCuentaPorPagar.escala(request.monto());
        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            throw conflicto("El monto (" + monto + ") supera el saldo pendiente (" + cuenta.getSaldo() + ")");
        }
        FormaPago forma = formasPago.findById(request.formaPagoId())
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .orElseThrow(() -> rechazo("La forma de pago no existe o está inactiva"));

        PagoProveedor pago = new PagoProveedor();
        pago.setCuentaPorPagarId(cuenta.getId());
        pago.setFormaPagoId(forma.getId());
        pago.setUsuarioId(usuarioResolver.resolver(authentication));
        pago.setMonto(monto);
        pago.setFechaPago(request.fechaPago() == null ? LocalDate.now() : request.fechaPago());
        pago.setNumeroRecibo(limpiar(request.numeroRecibo()));
        pago.setTransaccionExterna(limpiar(request.transaccionExterna()));
        pago.setObservaciones(limpiar(request.observaciones()));
        pagos.save(pago);

        cuenta.setMontoPagado(EstadoCuentaPorPagar.escala(cuenta.getMontoPagado()).add(monto));
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuenta.setEstado(EstadoCuentaPorPagar.estadoSegun(cuenta.getMontoPagado(), cuenta.getSaldo()));
        cuentas.saveAndFlush(cuenta);
        return respuesta(cuenta.getId());
    }

    @Transactional
    public List<CuentaPorPagarResumenResponse> generar(
            GenerarCuotasRequest request, Authentication authentication) {
        return generarDesdeCompra(
                request.compraId(),
                request.numeroCuotas(),
                request.primerVencimiento(),
                request.diasEntreCuotas(),
                usuarioResolver.resolver(authentication));
    }

    /**
     * Genera las cuotas por pagar de una compra a credito. Publico para que la CU23/CU24 lo
     * invoque al registrar la compra. Nulls: primer vencimiento = fecha de compra + dias de
     * credito del proveedor (30 si no tiene); dias entre cuotas = 30.
     */
    @Transactional
    public List<CuentaPorPagarResumenResponse> generarDesdeCompra(
            Long compraId, int numeroCuotas, LocalDate primerVencimiento,
            Integer diasEntreCuotas, Long usuarioId) {
        Compra compra = compras.findParaActualizar(compraId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra inexistente"));
        if (compra.esAnulada()) throw conflicto("La compra está anulada");
        if (!compra.esCredito()) throw conflicto("La compra no es a crédito (tipo de pago " + compra.getTipoPago() + ")");
        if (cuentas.existsByCompraId(compraId)) throw conflicto("La compra ya tiene cuotas por pagar");

        LocalDate primera = primerVencimiento != null ? primerVencimiento
                : CalculadoraCuotas.primerVencimientoPorDefecto(compra.getFecha(), diasCredito(compra));
        List<CalculadoraCuotas.Cuota> plan = CalculadoraCuotas.calcular(
                compra.getTotal(), numeroCuotas, primera,
                diasEntreCuotas == null ? DIAS_ENTRE_CUOTAS : diasEntreCuotas);

        List<CuentaPorPagar> nuevas = new ArrayList<>();
        for (CalculadoraCuotas.Cuota c : plan) {
            CuentaPorPagar cuenta = new CuentaPorPagar();
            cuenta.setCompraId(compra.getId());
            cuenta.setProveedorId(compra.getProveedorId());
            cuenta.setCreadoPor(usuarioId);
            cuenta.setNumeroCuota(c.numero());
            cuenta.setMontoTotal(c.monto());
            cuenta.setMontoPagado(EstadoCuentaPorPagar.escala(BigDecimal.ZERO));
            cuenta.setSaldo(c.monto());
            cuenta.setFechaVencimiento(c.fechaVencimiento());
            cuenta.setEstado(CuentaPorPagar.PENDIENTE);
            nuevas.add(cuenta);
        }
        cuentas.saveAllAndFlush(nuevas);
        LocalDate hoy = LocalDate.now();
        return consultas.cuotasDeCompra(compraId).stream().map(f -> mapper.toResumen(f, hoy)).toList();
    }

    public List<CompraSinCuentaResponse> comprasSinCuenta(String search) {
        return consultas.comprasSinCuenta(search, MAX_OPCIONES);
    }

    public List<ProveedorOpcionResponse> proveedores(String search) {
        return consultas.proveedoresActivos(search, MAX_OPCIONES);
    }

    public List<FormaPagoOpcionResponse> formasPago() {
        return formasPago.findByActivoTrueOrderByIdAsc().stream()
                .map(f -> new FormaPagoOpcionResponse(f.getId(), f.getNombre()))
                .toList();
    }

    // ------------------------------------------------------------------ internos

    private CuentaPorPagarResponse respuesta(Long id) {
        var fila = consultas.cuenta(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar inexistente"));
        return mapper.toResponse(fila, consultas.pagos(id), LocalDate.now());
    }

    private Integer diasCredito(Compra compra) {
        return proveedores.findById(compra.getProveedorId()).map(p -> p.getDiasCredito()).orElse(null);
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
