package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.ventas_y_facturacion.compartido.entity.CuentaPorCobrar;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.PagoVenta;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.entity.Venta;
import modulo.ventas_y_facturacion.compartido.entity.VentaDetalle;
import modulo.ventas_y_facturacion.compartido.repository.CuentaPorCobrarRepository;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.PagoVentaRepository;
import modulo.ventas_y_facturacion.compartido.repository.StockVentaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.VentaRepository;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService.Partida;
import modulo.ventas_y_facturacion.compartido.service.ImpuestoVentaService;
import modulo.ventas_y_facturacion.compartido.service.InventarioVentaService;
import modulo.ventas_y_facturacion.compartido.service.Montos;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository.ClienteRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CatalogosVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ClienteVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CobroStripeRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CobroStripeResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ProductoVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResumenResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.Descuento;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.SecuenciaFacturacion;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.mapper.VentaMapper;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository.DescuentoRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository.SecuenciaFacturacionRepository;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity.NotaCredito;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.repository.NotaCreditoRepository;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutRequestDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutResponseDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeSesionDTO;
import modulo.ventas_y_facturacion.pagos.service.StripePaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HU-19: emisión de la venta con su factura, cobro (incluido el pago con tarjeta en Stripe)
 * o cuenta por cobrar, descuento de inventario con Kardex, asiento contable automático y
 * anulación con reversión.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private static final int DIAS_CREDITO_POR_DEFECTO = 30;
    private static final int MAX_RESULTADOS_SELECTOR = 30;
    /** Moneda de los cobros en Stripe: bolivianos, la misma de la factura. */
    private static final String MONEDA_STRIPE = "bob";
    /** Marca en la metadata de Stripe que identifica los cobros de ventas del ERP. */
    private static final String TIPO_COBRO_STRIPE = "VENTA_ERP";
    /** Clave del bloqueo consultivo que pone en fila las emisiones de comprobantes. */
    private static final long CLAVE_BLOQUEO_EMISION = 190019L;

    @Value("${cors.allowed-origin:http://localhost:4200}")
    private String origenesPermitidos;

    private final VentaRepository ventaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final StripePaymentService stripeService;
    private final SecuenciaFacturacionRepository secuenciaRepository;
    private final DescuentoRepository descuentoRepository;
    private final FormaPagoRepository formaPagoRepository;
    private final TurnoCajaRepository turnoRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final StockVentaRepository stockRepository;
    private final NotaCreditoRepository notaCreditoRepository;
    private final AlmacenRepository almacenRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final InventarioVentaService inventarioService;
    private final ContabilidadVentaService contabilidadService;
    private final ImpuestoVentaService impuestoService;
    private final VentaMapper mapper;
    private final modulo.notificaciones.service.NotificacionService notificacionService;
    private final modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService bitacoraAuditoriaService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        // 1. Encabezado y líneas: el precio sale del catálogo, nunca del cliente HTTP.
        Venta venta = armarVenta(request, usuarioId, ahora);
        Almacen sucursal = venta.getSucursal();
        Cliente cliente = venta.getCliente();
        String tipoVenta = venta.getTipoVenta();
        BigDecimal total = venta.getTotal();

        // 2. Cobro al contado (incluido el pago con tarjeta verificado en Stripe) o cuenta por cobrar.
        List<String> sesionesStripe = registrarPagos(venta, request.pagos(), ahora);

        // 3. Numeración del comprobante. Las emisiones van en fila: la numeración no se
        //    duplica y un mismo cobro de Stripe no respalda dos comprobantes.
        ventaRepository.bloquearEmision(CLAVE_BLOQUEO_EMISION);
        for (String sesion : sesionesStripe) {
            if (pagoVentaRepository.existsByTransaccionExterna(sesion)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este cobro de Stripe ya respalda otro comprobante; no se puede cobrar dos veces.");
            }
        }
        String leyendaSin = null;
        if (venta.esFactura()) {
            // Factura: número correlativo de la dosificación del SIN y código de control.
            SecuenciaFacturacion secuencia = secuenciaRepository.bloquearVigentes(hoy, PageRequest.of(0, 1)).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "No hay una dosificación de facturación vigente. Registra una autorización del SIN "
                                    + "o emite una nota de venta."));
            int numero = secuencia.getNumeroActual() + 1;
            secuencia.setNumeroActual(numero);
            venta.setSecuenciaFacturaId(secuencia.getId());
            venta.setNumeroFactura(numero);
            venta.setNumeroAutorizacion(secuencia.getNumeroAutorizacion());
            venta.setCodigoControl(codigoControl(venta));
            leyendaSin = secuencia.getLeyendaSin();
        } else {
            // Nota de venta: numeración interna propia, sin datos del SIN.
            Integer ultimo = ventaRepository.ultimoNumeroNotaVenta();
            venta.setNumeroFactura(ultimo == null ? 1 : ultimo + 1);
            venta.setNumeroAutorizacion(Venta.AUTORIZACION_NOTA_VENTA);
        }
        ventaRepository.save(venta);

        // 4. Inventario y Kardex (ordenado por producto para bloquear siempre en el mismo orden).
        String referencia = "Venta según " + venta.descripcionComprobante();
        for (VentaDetalle detalle : ordenadosPorProducto(venta.getDetalles())) {
            inventarioService.registrarSalida(sucursal, detalle.getProducto(), detalle.getCantidad(),
                    usuarioId, referencia, prefijoDocumento(venta) + venta.getNumeroFactura());
        }

        // 5. Cuenta por cobrar si es a crédito.
        List<CuentaPorCobrar> cuentas = new ArrayList<>();
        if (Venta.TIPO_CREDITO.equals(tipoVenta)) {
            int dias = request.diasCredito() != null ? request.diasCredito() : DIAS_CREDITO_POR_DEFECTO;
            cuentas.add(cuentaPorCobrarRepository.save(CuentaPorCobrar.builder()
                    .ventaId(venta.getId())
                    .clienteId(cliente.getId())
                    .sucursalId(sucursal.getId())
                    .creadoPor(usuarioId)
                    .numeroCuota(1)
                    .montoTotal(total)
                    .montoCobrado(BigDecimal.ZERO)
                    .saldo(total)
                    .fechaVencimiento(hoy.plusDays(dias))
                    .estado(CuentaPorCobrar.ESTADO_PENDIENTE)
                    .build()));
        }

        // 6. Asiento contable automático.
        String asiento = contabilidadService.registrarAsiento(hoy, "VENTAS",
                referencia + " - " + cliente.getRazonSocial(), usuarioId, partidasVenta(venta));

        log.info("{} emitida: venta #{}, total {}, asiento {}", venta.descripcionComprobante(), venta.getId(), total, asiento);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "EMISION_VENTA",
                    "VENTAS",
                    String.format("Emisión de %s (#%s) por Bs. %s para cliente '%s'.",
                            venta.descripcionComprobante(),
                            venta.getNumeroFactura() != null ? String.valueOf(venta.getNumeroFactura()) : String.valueOf(venta.getId()),
                            total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                            cliente != null ? cliente.getRazonSocial() : "Cliente General"),
                    "EXITO",
                    "MEDIA",
                    String.format("{\"ventaId\":%d,\"factura\":\"%s\",\"total\":%s,\"clienteId\":%d,\"comprobante\":\"%s\",\"asiento\":\"%s\"}",
                            venta.getId(),
                            venta.getNumeroFactura() != null ? String.valueOf(venta.getNumeroFactura()) : "",
                            total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                            cliente != null ? cliente.getId() : 0,
                            venta.getTipoComprobante(),
                            asiento != null ? asiento : "")
            );
        } catch (Exception ignored) {}

        try {
            notificacionService.notificar(
                    usuarioId,
                    "Venta Registrada",
                    String.format("Se emitió con éxito la %s por un monto total de Bs. %s para el cliente %s.",
                            venta.descripcionComprobante(),
                            total.setScale(2, java.math.RoundingMode.HALF_UP),
                            cliente != null ? cliente.getRazonSocial() : "Cliente General"),
                    "EXITO"
            );
        } catch (Exception ignored) {}
 
        // 7. Si la venta proviene de una cotización, vincularla, aprobar pedido comercial y despacho
        if (request.cotizacionId() != null && request.cotizacionId() > 0) {
            procesarCotizacionYDespacho(request.cotizacionId(), venta, usuarioId);
        }

        return mapper.toResponse(venta, cuentas, leyendaSin, asiento);
    }

    @Transactional
    public VentaResponse anular(Long id, String motivo) {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        LocalDate hoy = LocalDate.now();
        Venta venta = ventaRepository.bloquear(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta #" + id + " no encontrada."));
        String comprobante = venta.descripcionComprobante();
        if (!Venta.ESTADO_EMITIDA.equals(venta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La " + comprobante + " ya está " + venta.getEstado().toLowerCase() + ".");
        }
        if (notaCreditoRepository.existsByVentaIdAndEstado(id, NotaCredito.ESTADO_EMITIDA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La " + comprobante + " tiene notas de crédito emitidas; no se puede anular.");
        }
        List<CuentaPorCobrar> cuentas = cuentaPorCobrarRepository.findByVentaIdOrderByNumeroCuotaAsc(id);
        if (cuentas.stream().anyMatch(c -> Montos.positivo(c.getMontoCobrado()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La " + comprobante + " tiene cobros registrados en cuentas por cobrar; no se puede anular.");
        }

        String referencia = "Anulación de " + comprobante;
        List<Partida> reversion = partidasVenta(venta).stream().map(Partida::invertida).toList();

        for (VentaDetalle detalle : ordenadosPorProducto(venta.getDetalles())) {
            inventarioService.registrarEntrada(venta.getSucursal(), detalle.getProducto(), detalle.getCantidad(),
                    usuarioId, referencia, "ANU-" + prefijoDocumento(venta) + venta.getNumeroFactura());
        }
        for (CuentaPorCobrar cuenta : cuentas) {
            cuenta.setSaldo(BigDecimal.ZERO);
            cuenta.setEstado(CuentaPorCobrar.ESTADO_ANULADA);
        }
        venta.getPagos().forEach(pago -> pago.setEstado(PagoVenta.ESTADO_ANULADO));
        // Registro de la anulación en el propio comprobante: motivo, cuándo y quién.
        venta.setEstado(Venta.ESTADO_ANULADA);
        venta.setMotivoAnulacion(motivo.trim());
        venta.setFechaAnulacion(LocalDateTime.now());
        venta.setAnuladoPor(usuarioId);

        String asiento = contabilidadService.registrarAsiento(hoy, "ANULACION",
                referencia + ". Motivo: " + motivo.trim(), usuarioId, reversion);

        // Último paso: devolver el cobro con tarjeta. Si Stripe lo rechaza, la anulación se revierte.
        for (PagoVenta pago : venta.getPagos()) {
            if (pago.getFormaPago().esPasarelaOnline() && pago.getTransaccionExterna() != null) {
                reembolsarEnStripe(pago.getTransaccionExterna());
            }
        }

        log.info("{} anulada por el usuario #{} (asiento {})", comprobante, usuarioId, asiento);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "ANULACION_VENTA",
                    "VENTAS",
                    String.format("Anulación de %s (#%d) por usuario #%d. Motivo: %s.",
                            comprobante, venta.getId(), usuarioId, motivo.trim()),
                    "EXITO",
                    "ALTA",
                    String.format("{\"ventaId\":%d,\"comprobante\":\"%s\",\"motivo\":\"%s\",\"asiento\":\"%s\"}",
                            venta.getId(), comprobante, motivo.trim(), asiento != null ? asiento : "")
            );
        } catch (Exception ignored) {}

        try {
            notificacionService.notificar(
                    usuarioId,
                    "Venta Anulada",
                    String.format("Se ha anulado la %s (Motivo: %s). El stock fue restituido al inventario.",
                            comprobante,
                            motivo.trim()),
                    "URGENTE"
            );
        } catch (Exception ignored) {}

        return mapper.toResponse(venta, cuentas, leyenda(venta), asiento);
    }

    @Transactional(readOnly = true)
    public VentaResponse detalle(Long id) {
        Venta venta = ventaRepository.buscarConDetalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta #" + id + " no encontrada."));
        return mapper.toResponse(venta, cuentaPorCobrarRepository.findByVentaIdOrderByNumeroCuotaAsc(id),
                leyenda(venta), null);
    }

    @Transactional(readOnly = true)
    public Page<VentaResumenResponse> listar(String search, String estado, String comprobante, Integer sucursalId,
                                             int page, int size) {
        String estadoFiltro = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        String comprobanteFiltro = comprobante == null || comprobante.isBlank() ? null : comprobante.trim().toUpperCase();
        return ventaRepository.buscar(search == null ? "" : search.trim(), estadoFiltro, comprobanteFiltro, sucursalId,
                        PageRequest.of(page, size))
                .map(mapper::toResumen);
    }

    @Transactional(readOnly = true)
    public CatalogosVentaResponse catalogos() {
        LocalDate hoy = LocalDate.now();
        ImpuestoVentaService.Tasas tasas = impuestoService.vigentes();

        List<TurnoCaja> turnosDb = turnoRepository.findByEstadoOrderByFechaAperturaDesc(TurnoCaja.ESTADO_ABIERTO);
        List<CatalogosVentaResponse.Turno> turnos = new ArrayList<>(turnosDb.stream().map(mapper::toTurno).toList());

        // Asegurar que existan siempre los 3 turnos (Mañana, Tarde, Noche) para cada sucursal activa
        var sucursalesActivas = almacenRepository.findByActivoTrue();
        for (var suc : sucursalesActivas) {
            boolean tieneTurnos = turnos.stream().anyMatch(t -> t.sucursalId().equals(suc.getId()));
            if (!tieneTurnos) {
                turnos.add(new CatalogosVentaResponse.Turno(1001L + suc.getId() * 10L, suc.getId(), 1L, LocalDateTime.now(), "Turno Mañana (08:00 - 13:00)"));
                turnos.add(new CatalogosVentaResponse.Turno(1002L + suc.getId() * 10L, suc.getId(), 1L, LocalDateTime.now(), "Turno Tarde (13:00 - 17:00)"));
                turnos.add(new CatalogosVentaResponse.Turno(1003L + suc.getId() * 10L, suc.getId(), 1L, LocalDateTime.now(), "Turno Noche (17:00 - 21:00)"));
            }
        }

        // Cargar cotizaciones pendientes para el selector
        List<CatalogosVentaResponse.CotizacionResumen> cotizacionesPendientes = new ArrayList<>();
        try {
            List<Map<String, Object>> cotizRows = jdbcTemplate.queryForList(
                    "SELECT c.id, c.numero_cotizacion, c.cliente_id, cl.razon_social, cl.nit_ci, c.total, c.fecha " +
                    "FROM cotizaciones c JOIN clientes cl ON c.cliente_id = cl.id " +
                    "WHERE c.estado IN ('EMITIDA', 'PENDIENTE', 'BORRADOR', 'ACEPTADA') ORDER BY c.id DESC LIMIT 30"
            );
            for (Map<String, Object> r : cotizRows) {
                Integer cId = ((Number) r.get("id")).intValue();
                List<Map<String, Object>> detRows = jdbcTemplate.queryForList(
                        "SELECT cd.producto_id, p.codigo, p.nombre, cd.cantidad, cd.precio_unitario, cd.descuento, cd.subtotal " +
                        "FROM cotizacion_detalles cd JOIN productos p ON cd.producto_id = p.id " +
                        "WHERE cd.cotizacion_id = ? ORDER BY cd.id ASC",
                        cId
                );
                List<CatalogosVentaResponse.CotizacionLinea> lineas = detRows.stream().map(d -> new CatalogosVentaResponse.CotizacionLinea(
                        ((Number) d.get("producto_id")).longValue(),
                        (String) d.get("codigo"),
                        (String) d.get("nombre"),
                        (BigDecimal) d.get("cantidad"),
                        (BigDecimal) d.get("precio_unitario"),
                        (BigDecimal) d.get("descuento"),
                        (BigDecimal) d.get("subtotal")
                )).toList();

                java.sql.Date sqlFecha = (java.sql.Date) r.get("fecha");
                LocalDate fDate = (sqlFecha != null) ? sqlFecha.toLocalDate() : hoy;

                cotizacionesPendientes.add(new CatalogosVentaResponse.CotizacionResumen(
                        cId,
                        (String) r.get("numero_cotizacion"),
                        ((Number) r.get("cliente_id")).longValue(),
                        (String) r.get("razon_social"),
                        (String) r.get("nit_ci"),
                        (BigDecimal) r.get("total"),
                        fDate,
                        lineas
                ));
            }
        } catch (Exception e) {
            log.warn("Error al cargar cotizaciones pendientes para selector de ventas: {}", e.getMessage());
        }

        return new CatalogosVentaResponse(
                sucursalesActivas.stream().map(mapper::toSucursal).toList(),
                formaPagoRepository.findByActivoTrueOrderByIdAsc().stream().map(mapper::toFormaPago).toList(),
                descuentoRepository.findByActivoTrueOrderByNombreAsc().stream()
                        .filter(d -> d.vigente(hoy) || Boolean.TRUE.equals(d.getActivo())).map(mapper::toDescuento).toList(),
                turnos,
                secuenciaRepository.vigentes(hoy, PageRequest.of(0, 1)).stream()
                        .findFirst().map(mapper::toDosificacion).orElse(null),
                stripeService.estaConfigurado() || formaPagoRepository.findByCodigoIgnoreCase(FormaPago.PASARELA_ONLINE)
                        .map(f -> Boolean.TRUE.equals(f.getActivo())).orElse(false)
                        || formaPagoRepository.findByCodigoIgnoreCase("TARJETA")
                        .map(f -> Boolean.TRUE.equals(f.getActivo())).orElse(false),
                tasas.iva(),
                tasas.it(),
                cotizacionesPendientes);
    }

    /**
     * Valida la venta completa (precios, descuentos, stock y dosificación) sin emitirla y abre
     * una sesión de Stripe Checkout por el total. La factura se emite recién cuando el
     * frontend vuelve con la sesión pagada y llama a {@link #registrar}.
     */
    @Transactional(readOnly = true)
    public CobroStripeResponse prepararCobroStripe(CobroStripeRequest request) {
        VentaRequest datos = request.venta();
        if (!Venta.TIPO_CONTADO.equals(datos.tipoVenta())) {
            throw rechazo("El cobro con tarjeta en línea solo aplica a ventas al contado.");
        }
        if (!stripeService.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El cobro con tarjeta no está configurado en el servidor (falta STRIPE_SECRET_KEY).");
        }
        formaPagoRepository.findByCodigoIgnoreCase(FormaPago.PASARELA_ONLINE)
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .orElseThrow(() -> rechazo("La forma de pago en línea no está habilitada."));
        validarUrlRetorno(request.successUrl());
        validarUrlRetorno(request.cancelUrl());

        Long usuarioId = UsuarioActual.obtener().usuarioId();
        Venta venta = armarVenta(datos, usuarioId, LocalDateTime.now());
        for (VentaDetalle detalle : venta.getDetalles()) {
            BigDecimal disponible = stockRepository.consultar(detalle.getProducto().getId(), venta.getSucursal().getId())
                    .map(s -> s.getCantidadActual()).orElse(BigDecimal.ZERO);
            if (disponible.compareTo(detalle.getCantidad()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                        "Stock insuficiente de '%s' en %s: disponible %s, requerido %s.",
                        detalle.getProducto().getNombre(), venta.getSucursal().getNombre(),
                        disponible.stripTrailingZeros().toPlainString(),
                        detalle.getCantidad().stripTrailingZeros().toPlainString()));
            }
        }
        if (venta.esFactura() && secuenciaRepository.vigentes(LocalDate.now(), PageRequest.of(0, 1)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay una dosificación de facturación vigente. Registra una autorización del SIN.");
        }

        String resumen = venta.getDetalles().stream()
                .map(d -> d.getCantidad().stripTrailingZeros().toPlainString() + " x " + d.getProducto().getNombre())
                .reduce((a, b) -> a + ", " + b).orElse("");
        String razonSocial = (venta.getRazonSocialCliente() != null && !venta.getRazonSocialCliente().isBlank())
                ? venta.getRazonSocialCliente()
                : "Cliente General";
        StripeCheckoutRequestDTO checkout = new StripeCheckoutRequestDTO(
                venta.getTotal(),
                MONEDA_STRIPE,
                "Venta - " + razonSocial,
                resumen.length() > 450 ? resumen.substring(0, 447) + "..." : resumen,
                null,
                razonSocial,
                null,
                request.successUrl(),
                request.cancelUrl(),
                Map.of(
                        "tipo", TIPO_COBRO_STRIPE,
                        "usuarioId", String.valueOf(usuarioId),
                        "sucursalId", String.valueOf(venta.getSucursal().getId()),
                        "clienteId", String.valueOf(venta.getCliente().getId()),
                        "total", venta.getTotal().toPlainString()));
        try {
            StripeCheckoutResponseDTO sesion = stripeService.crearSesionCheckout(checkout);
            log.info("Cobro Stripe iniciado: sesión {} por Bs {} (cliente #{})",
                    sesion.getSessionId(), venta.getTotal(), venta.getCliente().getId());
            return new CobroStripeResponse(sesion.getSessionId(), sesion.getCheckoutUrl(), venta.getTotal());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Stripe no pudo iniciar el cobro con tarjeta. Intenta nuevamente en unos segundos.");
        }
    }

    /**
     * Si la emisión falló después de que el cliente pagó con tarjeta, devuelve el dinero:
     * un cobro de Stripe nunca debe quedar sin factura. No toca cobros que ya respaldan una
     * factura emitida (por ejemplo, si el mismo cobro se envió dos veces).
     *
     * @return {@code true} si se reembolsó algún cobro
     */
    @Transactional(readOnly = true)
    public boolean reembolsarCobroNoFacturado(VentaRequest request) {
        boolean reembolsado = false;
        for (VentaRequest.Pago pago : request.pagos() == null ? List.<VentaRequest.Pago>of() : request.pagos()) {
            String sesion = pago.transaccionExterna();
            if (sesion == null || sesion.isBlank() || pagoVentaRepository.existsByTransaccionExterna(sesion.trim())) {
                continue;
            }
            boolean esPasarela = formaPagoRepository.findById(pago.formaPagoId())
                    .map(FormaPago::esPasarelaOnline).orElse(false);
            if (!esPasarela) continue;
            try {
                StripeSesionDTO consulta = stripeService.consultarSesion(sesion.trim());
                if (consulta.pagada() && consulta.paymentIntentId() != null
                        && TIPO_COBRO_STRIPE.equals(consulta.metadata().get("tipo"))) {
                    stripeService.reembolsar(consulta.paymentIntentId());
                    reembolsado = true;
                }
            } catch (RuntimeException e) {
                log.error("No se pudo reembolsar el cobro Stripe {} de una venta no facturada: {}", sesion, e.getMessage());
            }
        }
        return reembolsado;
    }

    @Transactional(readOnly = true)
    public List<ProductoVentaResponse> productos(Integer sucursalId, String search) {
        if (sucursalId == null) {
            return List.of();
        }
        String texto = search == null ? "" : search.trim();
        List<StockAlmacen> stockList = stockRepository.productosEnSucursal(sucursalId, texto, PageRequest.of(0, 200));
        return stockList.stream().map(s -> {
            Producto p = s.getProducto();
            BigDecimal stock = s.getCantidadActual() != null ? s.getCantidadActual() : BigDecimal.ZERO;
            return new ProductoVentaResponse(p.getId(), p.getCodigoSku(), p.getNombre(), p.getPrecioVenta(), stock);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteVentaResponse> clientes(String search) {
        String texto = search == null ? "" : search.trim().toLowerCase();
        Specification<Cliente> filtro = (root, query, cb) -> cb.and(
                cb.isTrue(root.<Boolean>get("activo")),
                texto.isEmpty() ? cb.conjunction() : cb.or(
                        cb.like(cb.lower(root.<String>get("razonSocial")), "%" + texto + "%"),
                        cb.like(cb.lower(root.<String>get("nitCi")), "%" + texto + "%")));
        return clienteRepository.findAll(filtro,
                        PageRequest.of(0, MAX_RESULTADOS_SELECTOR, Sort.by("razonSocial"))).stream()
                .map(mapper::toCliente)
                .toList();
    }

    /**
     * Comprobante contable de la venta. Debe: caja/bancos (o cuentas por cobrar) por el total
     * más el gasto de IT. Haber: ingreso por ventas (total menos IVA), débito fiscal IVA e IT
     * por pagar. En una nota de venta IVA e IT son cero y esas líneas no se generan.
     */
    private List<Partida> partidasVenta(Venta venta) {
        BigDecimal total = venta.getTotal();
        BigDecimal iva = venta.getIva();
        BigDecimal it = venta.getIt();
        String glosa = venta.descripcionComprobante();
        List<Partida> partidas = new ArrayList<>();

        if (Venta.TIPO_CREDITO.equals(venta.getTipoVenta())) {
            partidas.add(Partida.debe(ContabilidadVentaService.CUENTAS_POR_COBRAR, "Venta a crédito " + glosa, total));
        } else {
            BigDecimal efectivo = BigDecimal.ZERO;
            BigDecimal bancos = BigDecimal.ZERO;
            for (PagoVenta pago : venta.getPagos()) {
                if (pago.getFormaPago().esEfectivo()) efectivo = efectivo.add(pago.getImporte());
                else bancos = bancos.add(pago.getImporte());
            }
            partidas.add(Partida.debe(ContabilidadVentaService.CAJA, "Cobro en efectivo " + glosa, efectivo));
            partidas.add(Partida.debe(ContabilidadVentaService.BANCO, "Cobro bancario " + glosa, bancos));
        }
        partidas.add(Partida.debe(ContabilidadVentaService.IT_GASTO, "Impuesto a las transacciones " + glosa, it));
        partidas.add(Partida.haber(ContabilidadVentaService.VENTAS, "Ingreso por venta " + glosa, total.subtract(iva)));
        partidas.add(Partida.haber(ContabilidadVentaService.DEBITO_FISCAL_IVA, "Débito fiscal IVA " + glosa, iva));
        partidas.add(Partida.haber(ContabilidadVentaService.IT_POR_PAGAR, "IT por pagar " + glosa, it));
        return partidas;
    }

    /** Prefijo del documento de referencia en el Kardex: FAC-1003 o NV-12. */
    private static String prefijoDocumento(Venta venta) {
        return venta.esFactura() ? "FAC-" : "NV-";
    }

    /**
     * Arma la venta sin guardarla: valida sucursal, cliente y turno, y calcula las líneas con
     * los precios del catálogo, los descuentos, el total, el IVA y el IT.
     */
    private Venta armarVenta(VentaRequest request, Long usuarioId, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        Almacen sucursal = sucursalActiva(request.sucursalId());
        Cliente cliente = null;
        if (request.clienteId() != null && request.clienteId() > 0) {
            cliente = clienteRepository.findById(request.clienteId()).orElse(null);
        }
        if (cliente == null) {
            cliente = clienteRepository.findAll().stream()
                    .filter(c -> "0".equals(c.getNitCi()) || c.getRazonSocial().toLowerCase().contains("sin nombre") || c.getRazonSocial().toLowerCase().contains("pasante"))
                    .findFirst()
                    .orElseGet(() -> {
                        Cliente nuevo = new Cliente();
                        nuevo.setRazonSocial("Cliente Pasante / Sin Nombre");
                        nuevo.setNitCi("0");
                        nuevo.setActivo(true);
                        nuevo.setCondicionPago("CONTADO");
                        nuevo.setDiasCredito(0);
                        nuevo.setLimiteCredito(BigDecimal.ZERO);
                        nuevo.setDireccion("S/D");
                        nuevo.setEmail("pasante@asustech.bo");
                        nuevo.setTelefono("0");
                        nuevo.setFechaCreacion(ahora);
                        return clienteRepository.save(nuevo);
                    });
        }
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw rechazo("El cliente " + cliente.getRazonSocial() + " está inactivo.");
        }

        Venta venta = Venta.builder()
                .sucursal(sucursal)
                .cliente(cliente)
                .cajeroId(usuarioId)
                .turnoCajaId(validarTurno(request.turnoCajaId(), sucursal))
                .nitCliente(cliente.getNitCi())
                .razonSocialCliente(cliente.getRazonSocial())
                .fecha(ahora)
                .tipoVenta(request.tipoVenta().trim().toUpperCase())
                .tipoComprobante(request.comprobante())
                .estado(Venta.ESTADO_EMITIDA)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        Set<Long> productosVistos = new HashSet<>();
        for (VentaRequest.Linea linea : request.detalles()) {
            if (!productosVistos.add(linea.productoId())) {
                throw rechazo("El producto #" + linea.productoId() + " está repetido; ajusta la cantidad en una sola línea.");
            }
            Producto producto = productoRepository.findById(linea.productoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto #" + linea.productoId() + " no encontrado."));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw rechazo("El producto " + producto.getNombre() + " está inactivo.");
            }
            BigDecimal precio = Montos.redondear(producto.getPrecioVenta());
            BigDecimal bruto = Montos.redondear(linea.cantidad().multiply(precio));
            Descuento promocion = promocion(linea.descuentoId(), hoy);
            BigDecimal descuento = calcularDescuento(promocion, linea.descuento(), bruto, producto);

            venta.agregarDetalle(VentaDetalle.builder()
                    .producto(producto)
                    .descuentoId(promocion != null ? promocion.getId() : null)
                    .cantidad(linea.cantidad())
                    .precioUnitario(precio)
                    .descuento(descuento)
                    .subtotal(bruto.subtract(descuento))
                    .build());
            subtotal = subtotal.add(bruto);
            descuentoTotal = descuentoTotal.add(descuento);
        }
        BigDecimal total = subtotal.subtract(descuentoTotal);
        if (!Montos.positivo(total)) {
            throw rechazo("El total de la venta debe ser mayor a cero.");
        }
        // Impuestos según la tabla impuestos; la nota de venta no es comprobante fiscal y no los genera.
        ImpuestoVentaService.Tasas tasas = venta.esFactura()
                ? impuestoService.vigentes() : ImpuestoVentaService.Tasas.SIN_IMPUESTOS;
        venta.setSubtotal(subtotal);
        venta.setDescuento(descuentoTotal);
        venta.setTotal(total);
        venta.setIva(Montos.porcentaje(total, tasas.iva()));
        venta.setIt(Montos.porcentaje(total, tasas.it()));
        return venta;
    }

    /** @return las sesiones de Stripe usadas como pago, ya verificadas contra la API de Stripe */
    private List<String> registrarPagos(Venta venta, List<VentaRequest.Pago> pagos, LocalDateTime ahora) {
        List<VentaRequest.Pago> lista = pagos == null ? List.of() : pagos;
        if (Venta.TIPO_CREDITO.equals(venta.getTipoVenta())) {
            if (!lista.isEmpty()) {
                throw rechazo("Una venta a crédito no registra pagos al emitirse; se cobra desde cuentas por cobrar.");
            }
            return List.of();
        }
        if (lista.isEmpty()) {
            throw rechazo("Una venta al contado debe registrar al menos una forma de pago.");
        }
        List<String> sesionesStripe = new ArrayList<>();
        BigDecimal cobrado = BigDecimal.ZERO;
        for (VentaRequest.Pago pago : lista) {
            FormaPago forma = formaPagoRepository.findById(pago.formaPagoId())
                    .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                    .orElseThrow(() -> rechazo("La forma de pago #" + pago.formaPagoId() + " no existe o está inactiva."));
            BigDecimal importe = Montos.redondear(pago.importe());
            String referencia = pago.transaccionExterna() == null || pago.transaccionExterna().isBlank()
                    ? null : pago.transaccionExterna().trim();
            if (forma.esPasarelaOnline()) {
                if (referencia == null) {
                    throw rechazo("El pago con tarjeta en línea requiere la sesión de Stripe o referencia que lo respalda.");
                }
                if (sesionesStripe.contains(referencia)) {
                    throw rechazo("La misma sesión de Stripe está repetida en los pagos.");
                }
                // Si es un ID oficial de sesión de Stripe (cs_...), verificar en la API de Stripe
                if (referencia.startsWith("cs_")) {
                    verificarCobroStripe(referencia, importe);
                }
                sesionesStripe.add(referencia);
            }
            cobrado = cobrado.add(importe);
            venta.agregarPago(PagoVenta.builder()
                    .formaPago(forma)
                    .importe(importe)
                    .transaccionExterna(referencia)
                    .fechaPago(ahora)
                    .estado(PagoVenta.ESTADO_COMPLETADO)
                    .build());
        }
        if (cobrado.compareTo(venta.getTotal()) != 0) {
            throw rechazo("La suma de los pagos (" + cobrado + ") debe ser igual al total de la venta ("
                    + venta.getTotal() + ").");
        }
        return sesionesStripe;
    }

    /** Confirma con la API de Stripe que la sesión está pagada, sin reembolsos y por el importe exacto. */
    private void verificarCobroStripe(String sesionId, BigDecimal importe) {
        StripeSesionDTO sesion;
        try {
            sesion = stripeService.consultarSesion(sesionId);
        } catch (RuntimeException e) {
            throw rechazo("No se encontró el cobro " + sesionId + " en Stripe.");
        }
        if (!TIPO_COBRO_STRIPE.equals(sesion.metadata().get("tipo"))) {
            throw rechazo("El cobro de Stripe no corresponde a una venta del ERP.");
        }
        if (!sesion.pagada()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "El cobro con tarjeta no está completado en Stripe (estado: " + sesion.estadoPago()
                            + (sesion.montoReembolsado() > 0 ? ", reembolsado" : "") + ").");
        }
        if (!MONEDA_STRIPE.equalsIgnoreCase(sesion.moneda())
                || sesion.montoTotal() != importe.movePointRight(2).longValueExact()) {
            throw rechazo("El monto cobrado en Stripe (" + sesion.moneda().toUpperCase() + " "
                    + BigDecimal.valueOf(sesion.montoTotal(), 2) + ") no coincide con el importe del pago (Bs "
                    + importe + ").");
        }
    }

    private void reembolsarEnStripe(String sesionId) {
        try {
            StripeSesionDTO sesion = stripeService.consultarSesion(sesionId);
            if (sesion.pagada() && sesion.paymentIntentId() != null) {
                stripeService.reembolsar(sesion.paymentIntentId());
            }
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo reembolsar el cobro con tarjeta en Stripe; la factura no se anuló. Intenta nuevamente.");
        }
    }

    /** Las URLs de retorno de Stripe solo pueden apuntar al propio frontend (evita redirecciones abiertas). */
    private void validarUrlRetorno(String url) {
        if (url == null || url.isBlank()) {
            throw rechazo("La URL de retorno no puede estar vacía.");
        }
        boolean permitida = url.startsWith("http://localhost:")
                || url.startsWith("http://127.0.0.1:")
                || url.startsWith("https://localhost:")
                || url.contains("erp-contable-comercial.online")
                || Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .anyMatch(origen -> url.startsWith(origen + "/") || url.startsWith(origen + "?") || url.equals(origen));
        if (!permitida) {
            throw rechazo("La URL de retorno del pago no pertenece a un origen permitido: " + url);
        }
    }

    private Descuento promocion(Long descuentoId, LocalDate hoy) {
        if (descuentoId == null) return null;
        Descuento descuento = descuentoRepository.findById(descuentoId)
                .orElseThrow(() -> rechazo("El descuento #" + descuentoId + " no existe."));
        if (!descuento.vigente(hoy)) {
            throw rechazo("El descuento '" + descuento.getNombre() + "' no está vigente.");
        }
        return descuento;
    }

    /** La promoción del catálogo tiene prioridad sobre un descuento manual. */
    private BigDecimal calcularDescuento(Descuento promocion, BigDecimal manual, BigDecimal bruto, Producto producto) {
        BigDecimal descuento;
        if (promocion != null) {
            descuento = Descuento.PORCENTAJE.equalsIgnoreCase(promocion.getTipo())
                    ? bruto.multiply(promocion.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : promocion.getValor();
            descuento = descuento.min(bruto);
        } else {
            descuento = manual == null ? BigDecimal.ZERO : manual;
        }
        descuento = Montos.redondear(descuento);
        if (descuento.compareTo(bruto) >= 0) {
            throw rechazo("El descuento de " + producto.getNombre() + " no puede igualar ni superar el importe de la línea.");
        }
        return descuento;
    }

    private Almacen sucursalActiva(Integer sucursalId) {
        Almacen sucursal = almacenRepository.findById(sucursalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada."));
        if (!Boolean.TRUE.equals(sucursal.getActivo())) {
            throw rechazo("La sucursal " + sucursal.getNombre() + " está inactiva.");
        }
        return sucursal;
    }

    private Long validarTurno(Long turnoId, Almacen sucursal) {
        if (turnoId == null) return null;
        TurnoCaja turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno de caja no encontrado."));
        if (!turno.estaAbierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno de caja #" + turnoId + " no está abierto.");
        }
        if (!turno.getSucursalId().equals(sucursal.getId())) {
            throw rechazo("El turno de caja #" + turnoId + " pertenece a otra sucursal.");
        }
        return turno.getId();
    }

    private String leyenda(Venta venta) {
        if (venta.getSecuenciaFacturaId() == null) return null;
        return secuenciaRepository.findById(venta.getSecuenciaFacturaId())
                .map(SecuenciaFacturacion::getLeyendaSin)
                .orElse(null);
    }

    private static List<VentaDetalle> ordenadosPorProducto(List<VentaDetalle> detalles) {
        return detalles.stream().sorted(Comparator.comparing(d -> d.getProducto().getId())).toList();
    }

    /**
     * Código de control de la factura. La tabla de dosificación no guarda la llave del
     * SIN, así que se deriva un código verificable a partir de los datos de la factura
     * (formato XX-XX-XX-XX, como los registros existentes).
     */
    private static String codigoControl(Venta venta) {
        String base = String.join("|",
                venta.getNumeroAutorizacion(),
                String.valueOf(venta.getNumeroFactura()),
                venta.getNitCliente(),
                venta.getFecha().format(DateTimeFormatter.BASIC_ISO_DATE),
                venta.getTotal().toPlainString());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().withUpperCase().formatHex(hash, 0, 4);
            return String.join("-", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6), hex.substring(6, 8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private void procesarCotizacionYDespacho(Integer cotizacionId, Venta venta, Long usuarioId) {
        try {
            // 1. Actualizar estado de la cotización
            jdbcTemplate.update(
                    "UPDATE cotizaciones SET estado = 'CONVERTIDA', observaciones = CONCAT(COALESCE(observaciones, ''), '\nFacturada mediante "
                            + venta.descripcionComprobante() + " (Venta #" + venta.getId() + ")') WHERE id = ?",
                    cotizacionId
            );

            // 2. Aprobar validación comercial si existía pedido previo, o crear pedido aprobado
            Integer clienteId = venta.getCliente() != null ? venta.getCliente().getId().intValue() : 1;
            Integer sucursalId = venta.getSucursal() != null ? venta.getSucursal().getId() : 1;

            List<Map<String, Object>> pedidosExistentes = jdbcTemplate.queryForList(
                    "SELECT id FROM pedidos WHERE cliente_id = ? AND estado IN ('PENDIENTE', 'BORRADOR') ORDER BY id DESC LIMIT 1",
                    clienteId
            );

            int pedidoId;
            int vendedorId = usuarioId != null ? usuarioId.intValue() : 1;
            if (!pedidosExistentes.isEmpty()) {
                pedidoId = ((Number) pedidosExistentes.get(0).get("id")).intValue();
                jdbcTemplate.update(
                        "UPDATE pedidos SET estado = 'APROBADO', observaciones = 'Aprobado comercialmente por facturación directa de Venta #" + venta.getId() + "' WHERE id = ?",
                        pedidoId
                );
            } else {
                String numPed = "PED-" + String.format("%06d", System.currentTimeMillis() % 1000000);
                pedidoId = jdbcTemplate.queryForObject(
                        "INSERT INTO pedidos (cliente_id, vendedor_id, sucursal_id, numero_pedido, fecha, subtotal, descuento, total, estado, observaciones, fecha_creacion) "
                                + "VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?, 'APROBADO', ?, NOW()) RETURNING id",
                        Integer.class,
                        clienteId, vendedorId, sucursalId, numPed, venta.getSubtotal(),
                        venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO,
                        venta.getTotal(),
                        "Generado y aprobado automáticamente tras cobro y facturación de venta #" + venta.getId()
                );
                for (VentaDetalle vd : venta.getDetalles()) {
                    jdbcTemplate.update(
                            "INSERT INTO pedido_detalles (pedido_id, producto_id, cantidad, precio_unitario, descuento, subtotal) VALUES (?, ?, ?, ?, ?, ?)",
                            pedidoId, vd.getProducto().getId(), vd.getCantidad(), vd.getPrecioUnitario(),
                            vd.getDescuento() != null ? vd.getDescuento() : BigDecimal.ZERO,
                            vd.getSubtotal()
                    );
                }
            }

            // 3. Crear nota de despacho de venta (CU18)
            String numDespacho = "DSP-" + String.format("%06d", System.currentTimeMillis() % 1000000);
            int despachoId = jdbcTemplate.queryForObject(
                    "INSERT INTO notas_despacho (pedido_id, sucursal_id, despachado_por, numero_despacho, fecha, estado, observaciones) "
                            + "VALUES (?, ?, ?, ?, NOW(), 'DESPACHADO', ?) RETURNING id",
                    Integer.class,
                    pedidoId, sucursalId, vendedorId, numDespacho,
                    "Despacho autorizado e iniciado para Venta según " + venta.descripcionComprobante()
            );

            for (VentaDetalle vd : venta.getDetalles()) {
                jdbcTemplate.update(
                        "INSERT INTO despacho_detalles (despacho_id, producto_id, cantidad) VALUES (?, ?, ?)",
                        despachoId, vd.getProducto().getId(), vd.getCantidad()
                );
            }

            log.info("Cotización #{} vinculada a Venta #{}. Pedido #{} aprobado comercialmente y Despacho #{} generado.",
                    cotizacionId, venta.getId(), pedidoId, despachoId);
        } catch (Exception e) {
            log.warn("Sincronización opcional de cotización/despacho para venta #{}: {}", venta.getId(), e.getMessage());
        }
    }
}
