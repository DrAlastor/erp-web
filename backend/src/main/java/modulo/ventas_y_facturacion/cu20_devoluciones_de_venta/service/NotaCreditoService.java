package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.ventas_y_facturacion.compartido.entity.CuentaPorCobrar;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.entity.Venta;
import modulo.ventas_y_facturacion.compartido.entity.VentaDetalle;
import modulo.ventas_y_facturacion.compartido.repository.CuentaPorCobrarRepository;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.VentaRepository;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService.Partida;
import modulo.ventas_y_facturacion.compartido.service.InventarioVentaService;
import modulo.ventas_y_facturacion.compartido.service.Montos;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoRequest;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResumenResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.VentaDevolucionResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCredito;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCreditoDetalle;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.mapper.NotaCreditoMapper;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.repository.NotaCreditoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU20 (HU-20): devoluciones de mercadería mediante nota de crédito. Reingresa los artículos
 * al inventario, reduce la cuenta por cobrar o reembolsa por caja y revierte en forma
 * proporcional el asiento contable de la venta original.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoService {

    private static final int MAX_VENTAS_BUSQUEDA = 20;

    private final NotaCreditoRepository notaRepository;
    private final VentaRepository ventaRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final TurnoCajaRepository turnoRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final FormaPagoRepository formaPagoRepository;
    private final InventarioVentaService inventarioService;
    private final ContabilidadVentaService contabilidadService;
    private final NotaCreditoMapper mapper;

    @Transactional(readOnly = true)
    public List<VentaDevolucionResponse> buscarVentas(Integer numeroFactura, String nitCliente) {
        String nit = nitCliente == null ? "" : nitCliente.trim();
        List<Venta> ventas = ventaRepository.buscarVigentes(numeroFactura, nit).stream()
                .limit(MAX_VENTAS_BUSQUEDA)
                .toList();
        if (ventas.isEmpty()) return List.of();
        Map<Long, BigDecimal> devueltas = cantidadesDevueltas(ventas.stream().map(Venta::getId).toList());
        return ventas.stream().map(venta -> mapper.toVentaDevolucion(venta, devueltas)).toList();
    }

    @Transactional
    public NotaCreditoResponse emitir(NotaCreditoRequest request) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        // Bloquear la venta serializa las devoluciones concurrentes sobre la misma factura.
        Venta venta = ventaRepository.bloquear(request.ventaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Venta #" + request.ventaId() + " no encontrada."));
        if (!Venta.ESTADO_EMITIDA.equals(venta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se emiten notas de crédito sobre comprobantes vigentes; la "
                            + venta.descripcionComprobante() + " está " + venta.getEstado().toLowerCase() + ".");
        }

        Map<Long, VentaDetalle> lineas = new HashMap<>();
        venta.getDetalles().forEach(detalle -> lineas.put(detalle.getId(), detalle));
        Map<Long, BigDecimal> devueltas = cantidadesDevueltas(List.of(venta.getId()));

        NotaCredito nota = NotaCredito.builder()
                .venta(venta)
                .clienteId(venta.getCliente().getId())
                .sucursalId(venta.getSucursal().getId())
                .emitidoPor(usuarioId)
                .numeroNota(siguienteNumero(hoy.getYear()))
                .fecha(hoy)
                .motivo(request.motivo())
                .estado(NotaCredito.ESTADO_EMITIDA)
                .observaciones(request.observaciones() == null || request.observaciones().isBlank()
                        ? null : request.observaciones().trim())
                .build();

        // 1. Validar cantidades: a devolver <= vendida - devuelta antes.
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> vistas = new HashSet<>();
        for (NotaCreditoRequest.Item item : request.items()) {
            VentaDetalle detalle = lineas.get(item.ventaDetalleId());
            if (detalle == null) {
                throw rechazo("La línea #" + item.ventaDetalleId() + " no pertenece a la "
                        + venta.descripcionComprobante() + ".");
            }
            if (!vistas.add(detalle.getId())) {
                throw rechazo("El producto " + detalle.getProducto().getNombre() + " está repetido en la devolución.");
            }
            BigDecimal disponible = detalle.getCantidad()
                    .subtract(devueltas.getOrDefault(detalle.getId(), BigDecimal.ZERO));
            if (item.cantidad().compareTo(disponible) > 0) {
                throw rechazo(String.format("De '%s' solo quedan %s unidades por devolver.",
                        detalle.getProducto().getNombre(), disponible.stripTrailingZeros().toPlainString()));
            }
            // Se devuelve al precio neto efectivamente cobrado (con el descuento de la línea prorrateado).
            BigDecimal subtotal = Montos.redondear(detalle.getSubtotal().multiply(item.cantidad())
                    .divide(detalle.getCantidad(), 6, RoundingMode.HALF_UP));
            nota.agregarDetalle(NotaCreditoDetalle.builder()
                    .producto(detalle.getProducto())
                    .ventaDetalleId(detalle.getId())
                    .cantidad(item.cantidad())
                    .precioUnitario(detalle.getSubtotal().divide(detalle.getCantidad(), 2, RoundingMode.HALF_UP))
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        if (!Montos.positivo(total)) {
            throw rechazo("El importe de la devolución debe ser mayor a cero.");
        }
        nota.setTotal(total);
        notaRepository.save(nota);

        // 2. Reingreso a inventario y Kardex.
        String referencia = "Devolución de cliente segun Nota de Crédito " + nota.getNumeroNota();
        for (NotaCreditoDetalle detalle : nota.getDetalles().stream()
                .sorted(Comparator.comparing(d -> d.getProducto().getId())).toList()) {
            inventarioService.registrarEntrada(venta.getSucursal(), detalle.getProducto(), detalle.getCantidad(),
                    usuarioId, referencia, nota.getNumeroNota());
        }

        // 3. Ajuste financiero: primero baja la deuda del cliente, lo que sobre se reembolsa por caja.
        BigDecimal pendiente = total;
        if (Venta.TIPO_CREDITO.equals(venta.getTipoVenta())) {
            for (CuentaPorCobrar cuenta : cuentaPorCobrarRepository.findByVentaIdOrderByNumeroCuotaAsc(venta.getId())) {
                if (!Montos.positivo(pendiente)) break;
                if (CuentaPorCobrar.ESTADO_ANULADA.equals(cuenta.getEstado()) || !Montos.positivo(cuenta.getSaldo())) {
                    continue;
                }
                BigDecimal aplicado = cuenta.getSaldo().min(pendiente);
                cuenta.setSaldo(cuenta.getSaldo().subtract(aplicado));
                if (!Montos.positivo(cuenta.getSaldo())) cuenta.setEstado(CuentaPorCobrar.ESTADO_PAGADO);
                pendiente = pendiente.subtract(aplicado);
            }
        }
        BigDecimal aplicadoCxc = total.subtract(pendiente);
        BigDecimal reembolso = pendiente;
        if (Montos.positivo(reembolso)) {
            registrarReembolso(venta, nota, reembolso, usuarioId, ahora);
        }

        // 4. Reversión proporcional del asiento de la venta: las mismas cuentas en sentido
        //    contrario, por la fracción devuelta (total de la nota / total de la venta). El IVA
        //    y el IT salen de los que se registraron en la venta, no de las tasas actuales.
        BigDecimal iva = Montos.proporcion(venta.getIva(), total, venta.getTotal());
        BigDecimal it = Montos.proporcion(venta.getIt(), total, venta.getTotal());
        String glosa = "Nota de Crédito " + nota.getNumeroNota() + " s/" + venta.descripcionComprobante();
        String asiento = contabilidadService.registrarAsiento(hoy, "DEVOLUCION",
                glosa + " - " + venta.getRazonSocialCliente(), usuarioId, List.of(
                        Partida.debe(ContabilidadVentaService.VENTAS, "Reversión de ingreso " + glosa,
                                total.subtract(iva)),
                        Partida.debe(ContabilidadVentaService.DEBITO_FISCAL_IVA, "Reversión débito fiscal IVA " + glosa, iva),
                        Partida.debe(ContabilidadVentaService.IT_POR_PAGAR, "Reversión IT por pagar " + glosa, it),
                        Partida.haber(ContabilidadVentaService.IT_GASTO, "Reversión impuesto a las transacciones " + glosa, it),
                        Partida.haber(ContabilidadVentaService.CUENTAS_POR_COBRAR, "Reducción de deuda " + glosa,
                                aplicadoCxc),
                        Partida.haber(ContabilidadVentaService.CAJA, "Reembolso " + glosa, reembolso)));

        log.info("Nota de crédito {} emitida sobre la {}: total {}, CxC {}, reembolso {}",
                nota.getNumeroNota(), venta.descripcionComprobante(), total, aplicadoCxc, reembolso);
        return mapper.toResponse(nota, aplicadoCxc, reembolso, asiento);
    }

    @Transactional(readOnly = true)
    public Page<NotaCreditoResumenResponse> listar(String search, int page, int size) {
        return notaRepository.buscar(search == null ? "" : search.trim(), PageRequest.of(page, size))
                .map(mapper::toResumen);
    }

    @Transactional(readOnly = true)
    public NotaCreditoResponse detalle(Long id) {
        NotaCredito nota = notaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nota de crédito #" + id + " no encontrada."));
        return mapper.toResponse(nota, null, null, null);
    }

    /** Egreso en el turno abierto de la sucursal: el del usuario si tiene uno, si no cualquiera. */
    private void registrarReembolso(Venta venta, NotaCredito nota, BigDecimal monto, Long usuarioId,
                                    LocalDateTime ahora) {
        Integer sucursalId = venta.getSucursal().getId();
        TurnoCaja turno = turnoRepository
                .findFirstBySucursalIdAndCajeroIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalId, usuarioId, TurnoCaja.ESTADO_ABIERTO)
                .or(() -> turnoRepository.findFirstBySucursalIdAndEstadoOrderByFechaAperturaDesc(
                        sucursalId, TurnoCaja.ESTADO_ABIERTO))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay un turno de caja abierto en " + venta.getSucursal().getNombre()
                                + " para reembolsar Bs " + monto + ". Abre un turno de caja e intenta de nuevo."));
        FormaPago efectivo = formaPagoRepository.findByCodigoIgnoreCase(FormaPago.EFECTIVO)
                .orElseThrow(() -> new IllegalStateException("No existe la forma de pago EFECTIVO"));
        movimientoCajaRepository.save(MovimientoCaja.builder()
                .turnoCajaId(turno.getId())
                .formaPago(efectivo)
                .tipo(MovimientoCaja.TIPO_EGRESO)
                .concepto("Reembolso por Nota de Crédito " + nota.getNumeroNota()
                        + " s/" + venta.descripcionComprobante())
                .monto(monto)
                .numeroComprobante(nota.getNumeroNota())
                .fechaHora(ahora)
                .build());
    }

    private Map<Long, BigDecimal> cantidadesDevueltas(List<Long> ventaIds) {
        Map<Long, BigDecimal> devueltas = new HashMap<>();
        for (Object[] fila : notaRepository.cantidadesDevueltas(ventaIds)) {
            devueltas.put(((Number) fila[0]).longValue(), (BigDecimal) fila[1]);
        }
        return devueltas;
    }

    private String siguienteNumero(int gestion) {
        String prefijo = "NC-" + gestion + "-";
        String ultimo = notaRepository.ultimoNumero(prefijo + "%");
        int siguiente = 1;
        if (ultimo != null) {
            try {
                siguiente = Integer.parseInt(ultimo.substring(prefijo.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("Número de nota de crédito con formato inesperado: {}", ultimo);
            }
        }
        return prefijo + String.format("%04d", siguiente);
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
