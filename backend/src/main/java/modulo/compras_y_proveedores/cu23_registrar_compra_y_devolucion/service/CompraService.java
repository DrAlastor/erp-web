package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto.MovimientoInventarioRequest;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.service.MovimientoInventarioService;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.dto.ComprasDtos.*;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.entity.*;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.repository.CompraRepository;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.repository.DevolucionProveedorRepository;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Sucursal;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.SucursalRepository;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.service.EmpresaConfiguracionService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CU23: registro de compras a proveedores y devoluciones. Cada compra suma stock al almacen y cada
 * devolucion lo resta, siempre a traves del servicio de movimientos de inventario (CU11) para que
 * el Kardex quede consistente. Todo ocurre en una sola transaccion: si el inventario rechaza una
 * linea, no queda registrada ni la compra ni la devolucion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompraService {

    private static final Set<String> TIPOS_PAGO = Set.of("CONTADO", "CREDITO");

    private final CompraRepository compraRepository;
    private final DevolucionProveedorRepository devolucionRepository;
    private final MovimientoInventarioService inventarioService;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaConfiguracionService configuracionService;
    private final BitacoraLogRepository bitacoraLogRepository;
    private final JdbcTemplate jdbc;

    // -------------------------------------------------------------- Catalogos

    @Transactional(readOnly = true)
    public CatalogosDTO catalogos() {
        List<ProveedorItemDTO> proveedores = jdbc.query(
                "SELECT id, razon_social, nit_ci, dias_credito FROM proveedores WHERE activo = TRUE ORDER BY razon_social",
                (rs, n) -> new ProveedorItemDTO(rs.getInt("id"), rs.getString("razon_social"),
                        rs.getString("nit_ci"), (Integer) rs.getObject("dias_credito")));
        return new CatalogosDTO(proveedores, inventarioService.listarAlmacenesActivos(),
                inventarioService.listarProductosActivos());
    }

    // ---------------------------------------------------------------- Compras

    @Transactional(readOnly = true)
    public List<CompraDTO> listarCompras(Integer proveedorId, String estado, LocalDate desde, LocalDate hasta, String search) {
        Map<Integer, String> proveedores = nombresProveedores();
        Map<Integer, String> sucursales = nombresSucursales();
        String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        return compraRepository.findAllByOrderByFechaDescIdDesc().stream()
                .filter(c -> proveedorId == null || c.getProveedorId().equals(proveedorId))
                .filter(c -> estado == null || estado.isBlank() || c.getEstado().equalsIgnoreCase(estado))
                .filter(c -> desde == null || !c.getFecha().isBefore(desde))
                .filter(c -> hasta == null || !c.getFecha().isAfter(hasta))
                .filter(c -> q == null || c.getNumeroFactura().toLowerCase().contains(q)
                        || proveedores.getOrDefault(c.getProveedorId(), "").toLowerCase().contains(q))
                .map(c -> aDTO(c, proveedores, sucursales, null, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompraDTO obtenerCompra(Integer id) {
        return aDTO(buscarCompra(id), nombresProveedores(), nombresSucursales(), null, true);
    }

    @Transactional
    public CompraDTO registrarCompra(RegistrarCompraRequest req, UUID empresaId, Long usuarioId, String actor, String ip) {
        String tipoPago = req.tipoPago().trim().toUpperCase();
        if (!TIPOS_PAGO.contains(tipoPago)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de pago debe ser CONTADO o CREDITO");
        }
        validarProveedorActivo(req.proveedorId());
        Sucursal almacen = sucursalRepository.findById(req.sucursalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Almacén no encontrado"));
        if (!Boolean.TRUE.equals(almacen.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El almacén seleccionado está inactivo");
        }
        String factura = req.numeroFactura().trim();
        if (compraRepository.existsByProveedorIdAndNumeroFacturaIgnoreCaseAndEstadoNot(
                req.proveedorId(), factura, Compra.ANULADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una compra del proveedor con la factura " + factura);
        }
        if (req.fecha().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de la compra no puede ser futura");
        }
        long distintos = req.lineas().stream().map(LineaCompraRequest::productoId).distinct().count();
        if (distintos != req.lineas().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hay productos repetidos; agrupe la cantidad en una sola línea");
        }

        Compra compra = new Compra();
        compra.setProveedorId(req.proveedorId());
        compra.setSucursalId(req.sucursalId());
        compra.setUsuarioId(usuarioId.intValue());
        compra.setOrdenCompraId(req.ordenCompraId());
        compra.setNumeroFactura(factura);
        compra.setNumeroAutorizacion(vacioANulo(req.numeroAutorizacion()));
        compra.setFecha(req.fecha());
        compra.setTipoPago(tipoPago);
        compra.setObservaciones(vacioANulo(req.observaciones()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaCompraRequest l : req.lineas()) {
            CompraDetalle d = new CompraDetalle();
            d.setProductoId(l.productoId());
            d.setCantidad(l.cantidad());
            d.setCostoUnitario(l.costoUnitario());
            d.setSubtotal(dinero(l.cantidad().multiply(l.costoUnitario())));
            subtotal = subtotal.add(d.getSubtotal());
            compra.agregarDetalle(d);
        }
        BigDecimal tasaIva = tasaIva(empresaId);
        compra.setSubtotal(subtotal);
        compra.setIva(dinero(subtotal.multiply(tasaIva)));
        compra.setTotal(compra.getSubtotal().add(compra.getIva()));

        Compra guardada = compraRepository.save(compra);

        // Ingreso al inventario: una ENTRADA por linea, valorada al costo de la compra.
        String referencia = truncar("COMPRA-" + guardada.getId() + " FAC-" + factura, 50);
        for (CompraDetalle d : guardada.getDetalles()) {
            inventarioService.registrarMovimiento(MovimientoInventarioRequest.builder()
                    .productoId(d.getProductoId())
                    .almacenId(guardada.getSucursalId())
                    .tipoMovimiento("ENTRADA")
                    .cantidad(d.getCantidad())
                    .costoUnitario(d.getCostoUnitario())
                    .referenciaDoc(referencia)
                    .motivo("Compra a proveedor")
                    .fecha(guardada.getFecha().atStartOfDay().plusHours(12))
                    .build(), actor);
        }

        bitacora("REGISTRAR_COMPRA", actor, "compras", guardada.getId().longValue(), ip);
        log.info("CU23: compra {} registrada, total {}", guardada.getId(), guardada.getTotal());
        return obtenerCompra(guardada.getId());
    }

    @Transactional
    public CompraDTO anularCompra(Integer id, AnularCompraRequest req, String actor, String ip) {
        Compra compra = buscarCompra(id);
        if (!Compra.REGISTRADA.equals(compra.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede anular una compra en estado REGISTRADA (estado actual: " + compra.getEstado() + ")");
        }
        if (devolucionRepository.existsByCompraId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra tiene devoluciones y no puede anularse");
        }
        String referencia = truncar("ANULA-COMPRA-" + id, 50);
        for (CompraDetalle d : compra.getDetalles()) {
            inventarioService.registrarMovimiento(MovimientoInventarioRequest.builder()
                    .productoId(d.getProductoId())
                    .almacenId(compra.getSucursalId())
                    .tipoMovimiento("SALIDA")
                    .cantidad(d.getCantidad())
                    .costoUnitario(d.getCostoUnitario())
                    .referenciaDoc(referencia)
                    .motivo("Anulación de compra: " + req.motivo().trim())
                    .build(), actor);
        }
        compra.setEstado(Compra.ANULADA);
        String nota = "Anulada: " + req.motivo().trim();
        compra.setObservaciones(compra.getObservaciones() == null ? nota : compra.getObservaciones() + " | " + nota);
        compraRepository.save(compra);
        bitacora("ANULAR_COMPRA", actor, "compras", id.longValue(), ip);
        return obtenerCompra(id);
    }

    // ----------------------------------------------------------- Devoluciones

    @Transactional(readOnly = true)
    public List<DevolucionDTO> listarDevoluciones(Integer compraId) {
        List<DevolucionProveedor> lista = compraId == null
                ? devolucionRepository.findAllByOrderByIdDesc()
                : devolucionRepository.findByCompraIdOrderByIdDesc(compraId);
        Map<Integer, String> proveedores = nombresProveedores();
        Map<Long, Producto> productos = productosPorId(lista.stream()
                .flatMap(d -> d.getDetalles().stream()).map(DevolucionDetalle::getProductoId).collect(Collectors.toSet()));
        Map<Integer, Compra> compras = compraRepository.findAllById(
                lista.stream().map(DevolucionProveedor::getCompraId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Compra::getId, c -> c));
        return lista.stream().map(d -> aDTO(d, compras.get(d.getCompraId()), proveedores, productos))
                .collect(Collectors.toList());
    }

    @Transactional
    public DevolucionDTO registrarDevolucion(Integer compraId, RegistrarDevolucionRequest req, Long usuarioId,
                                             String actor, String ip) {
        Compra compra = buscarCompra(compraId);
        if (!Compra.REGISTRADA.equals(compra.getEstado()) && !Compra.DEVUELTA_PARCIAL.equals(compra.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede devolver una compra en estado " + compra.getEstado());
        }
        LocalDate fecha = req.fecha() != null ? req.fecha() : LocalDate.now();
        if (fecha.isBefore(compra.getFecha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de la devolución no puede ser anterior a la de la compra");
        }
        Map<Integer, CompraDetalle> lineas = compra.getDetalles().stream()
                .collect(Collectors.toMap(CompraDetalle::getId, d -> d));
        if (req.lineas().stream().map(LineaDevolucionRequest::compraDetalleId).distinct().count() != req.lineas().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay líneas repetidas en la devolución");
        }

        DevolucionProveedor dev = new DevolucionProveedor();
        dev.setCompraId(compraId);
        dev.setUsuarioId(usuarioId.intValue());
        dev.setNumeroDevolucion("TMP-" + UUID.randomUUID());
        dev.setFecha(fecha);
        dev.setMotivo(req.motivo().trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaDevolucionRequest l : req.lineas()) {
            CompraDetalle original = lineas.get(l.compraDetalleId());
            if (original == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La línea " + l.compraDetalleId() + " no pertenece a esta compra");
            }
            BigDecimal pendiente = original.getCantidad().subtract(devolucionRepository.cantidadDevuelta(original.getId()));
            if (l.cantidad().compareTo(pendiente) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Solo se pueden devolver " + pendiente.stripTrailingZeros().toPlainString()
                                + " unidades de la línea " + original.getId());
            }
            DevolucionDetalle d = new DevolucionDetalle();
            d.setCompraDetalleId(original.getId());
            d.setProductoId(original.getProductoId());
            d.setCantidad(l.cantidad());
            d.setCostoUnitario(original.getCostoUnitario());
            d.setSubtotal(dinero(l.cantidad().multiply(original.getCostoUnitario())));
            subtotal = subtotal.add(d.getSubtotal());
            dev.agregarDetalle(d);
        }
        // El IVA devuelto conserva la proporcion con que se registro la compra.
        BigDecimal proporcion = compra.getSubtotal().signum() == 0 ? BigDecimal.ZERO
                : compra.getIva().divide(compra.getSubtotal(), 8, RoundingMode.HALF_UP);
        dev.setSubtotal(subtotal);
        dev.setIva(dinero(subtotal.multiply(proporcion)));
        dev.setTotal(dev.getSubtotal().add(dev.getIva()));

        DevolucionProveedor guardada = devolucionRepository.save(dev);
        guardada.setNumeroDevolucion(String.format("DP-%06d", guardada.getId()));

        // Salida del inventario: si no hay stock suficiente se rechaza y se revierte todo.
        String referencia = truncar(guardada.getNumeroDevolucion() + " COMPRA-" + compraId, 50);
        for (DevolucionDetalle d : guardada.getDetalles()) {
            inventarioService.registrarMovimiento(MovimientoInventarioRequest.builder()
                    .productoId(d.getProductoId())
                    .almacenId(compra.getSucursalId())
                    .tipoMovimiento("SALIDA")
                    .cantidad(d.getCantidad())
                    .costoUnitario(d.getCostoUnitario())
                    .referenciaDoc(referencia)
                    .motivo("Devolución a proveedor: " + guardada.getMotivo())
                    .fecha(fecha.atStartOfDay().plusHours(12))
                    .build(), actor);
        }

        boolean todoDevuelto = compra.getDetalles().stream().allMatch(d ->
                devolucionRepository.cantidadDevuelta(d.getId()).compareTo(d.getCantidad()) >= 0);
        compra.setEstado(todoDevuelto ? Compra.DEVUELTA : Compra.DEVUELTA_PARCIAL);
        compraRepository.save(compra);

        bitacora("REGISTRAR_DEVOLUCION_PROVEEDOR", actor, "devoluciones_proveedor", guardada.getId().longValue(), ip);
        log.info("CU23: devolucion {} de la compra {} por {}", guardada.getNumeroDevolucion(), compraId, guardada.getTotal());
        return listarDevoluciones(compraId).stream().filter(x -> x.id().equals(guardada.getId())).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- Privado

    private Compra buscarCompra(Integer id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
    }

    private void validarProveedorActivo(Integer proveedorId) {
        List<Boolean> r = jdbc.query("SELECT activo FROM proveedores WHERE id = ?",
                (rs, n) -> rs.getBoolean("activo"), proveedorId);
        if (r.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado");
        if (!r.get(0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor está inactivo");
    }

    private BigDecimal tasaIva(UUID empresaId) {
        return configuracionService.obtenerConfiguracion(empresaId).stream()
                .filter(p -> p.clave().equals("iva_porcentaje")).findFirst()
                .map(p -> new BigDecimal(p.valor()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .orElse(new BigDecimal("0.13"));
    }

    private Map<Integer, String> nombresProveedores() {
        Map<Integer, String> m = new HashMap<>();
        jdbc.query("SELECT id, razon_social FROM proveedores", rs -> {
            m.put(rs.getInt("id"), rs.getString("razon_social"));
        });
        return m;
    }

    private Map<Integer, String> nombresSucursales() {
        return sucursalRepository.findAll().stream()
                .collect(Collectors.toMap(Sucursal::getId, Sucursal::getNombre, (a, b) -> a));
    }

    private Map<Long, Producto> productosPorId(Collection<Long> ids) {
        return productoRepository.findAllById(ids).stream().collect(Collectors.toMap(Producto::getId, p -> p));
    }

    private CompraDTO aDTO(Compra c, Map<Integer, String> proveedores, Map<Integer, String> sucursales,
                           Map<Long, Producto> productos, boolean conDetalle) {
        List<CompraDetalleDTO> detalles = List.of();
        if (conDetalle) {
            Map<Long, Producto> prods = productos != null ? productos
                    : productosPorId(c.getDetalles().stream().map(CompraDetalle::getProductoId).collect(Collectors.toSet()));
            detalles = c.getDetalles().stream().map(d -> {
                Producto p = prods.get(d.getProductoId());
                return new CompraDetalleDTO(d.getId(), d.getProductoId(), p != null ? p.getCodigoSku() : null,
                        p != null ? p.getNombre() : "Producto #" + d.getProductoId(), d.getCantidad(),
                        d.getCostoUnitario(), d.getSubtotal(), devolucionRepository.cantidadDevuelta(d.getId()));
            }).collect(Collectors.toList());
        }
        return new CompraDTO(c.getId(), c.getProveedorId(), proveedores.get(c.getProveedorId()), c.getSucursalId(),
                sucursales.get(c.getSucursalId()), c.getOrdenCompraId(), c.getNumeroFactura(), c.getNumeroAutorizacion(),
                c.getFecha(), c.getSubtotal(), c.getIva(), c.getTotal(), c.getTipoPago(), c.getEstado(),
                c.getObservaciones(), c.getFechaCreacion(), detalles);
    }

    private DevolucionDTO aDTO(DevolucionProveedor d, Compra compra, Map<Integer, String> proveedores,
                               Map<Long, Producto> productos) {
        List<DevolucionDetalleDTO> detalles = d.getDetalles().stream().map(x -> {
            Producto p = productos.get(x.getProductoId());
            return new DevolucionDetalleDTO(x.getId(), x.getCompraDetalleId(), x.getProductoId(),
                    p != null ? p.getNombre() : "Producto #" + x.getProductoId(), x.getCantidad(),
                    x.getCostoUnitario(), x.getSubtotal());
        }).collect(Collectors.toList());
        return new DevolucionDTO(d.getId(), d.getCompraId(), d.getNumeroDevolucion(),
                compra != null ? compra.getNumeroFactura() : null,
                compra != null ? proveedores.get(compra.getProveedorId()) : null,
                d.getFecha(), d.getMotivo(), d.getSubtotal(), d.getIva(), d.getTotal(), detalles);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String truncar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private void bitacora(String accion, String actor, String entidad, Long entidadId, String ip) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion,
                    (actor != null && !actor.isBlank()) ? actor : "sistema", entidad, entidadId, ip));
        } catch (Exception e) {
            log.warn("No se pudo persistir registro en bitacora_logs: {}", e.getMessage());
        }
    }
}
