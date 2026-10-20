package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.service;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Numeracion;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.entity.PedidoDetalle;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository.ClienteRepository;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResumenResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.ItemCotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.OpcionesCotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.PedidoGeneradoResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.Cotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.CotizacionDetalle;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.EstadoCotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.mapper.CotizacionMapper;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.repository.CotizacionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * CU15 - Gestionar Cotizaciones.
 *
 * <p>Reglas que aplica (las mismas del documento del sprint):
 * <ul>
 *   <li>Sólo se cotiza a clientes activos y con productos activos del catálogo.</li>
 *   <li>El precio lo pone el servidor desde el catálogo, nunca el navegador.</li>
 *   <li>La suma de descuentos no puede superar el porcentaje máximo de la política.</li>
 *   <li>Sólo se modifica en BORRADOR o EMITIDA; se convierte sólo si está vigente.</li>
 *   <li>Una cotización genera como máximo un pedido: al convertirla queda CONVERTIDA.</li>
 *   <li>No toca el inventario: la proforma no reserva ni descuenta stock.</li>
 * </ul>
 */
@Service
public class CotizacionService {

    static final int VALIDEZ_POR_DEFECTO = 15;

    private final CotizacionRepository cotizaciones;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final AlmacenRepository sucursales;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final CotizacionMapper mapper;
    private final BigDecimal descuentoMaximoPorcentaje;
    private final Clock reloj;

    @Autowired
    public CotizacionService(CotizacionRepository cotizaciones,
                             ClienteRepository clientes,
                             ProductoRepository productos,
                             AlmacenRepository sucursales,
                             PedidoRepository pedidos,
                             UsuarioRepository usuarios,
                             CotizacionMapper mapper,
                             @Value("${erp.ventas.descuento-maximo-porcentaje:15}")
                             BigDecimal descuentoMaximoPorcentaje) {
        this(cotizaciones, clientes, productos, sucursales, pedidos, usuarios, mapper,
                descuentoMaximoPorcentaje, Clock.systemDefaultZone());
    }

    CotizacionService(CotizacionRepository cotizaciones,
                      ClienteRepository clientes,
                      ProductoRepository productos,
                      AlmacenRepository sucursales,
                      PedidoRepository pedidos,
                      UsuarioRepository usuarios,
                      CotizacionMapper mapper,
                      BigDecimal descuentoMaximoPorcentaje,
                      Clock reloj) {
        this.cotizaciones = cotizaciones;
        this.clientes = clientes;
        this.productos = productos;
        this.sucursales = sucursales;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.mapper = mapper;
        this.descuentoMaximoPorcentaje = descuentoMaximoPorcentaje;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------- consultas --

    @Transactional
    public Page<CotizacionResumenResponse> listar(String texto, EstadoCotizacion estado,
                                                  Long clienteId, int pagina, int tamanio) {
        cotizaciones.vencerCotizacionesExpiradas();
        PageRequest orden = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "id"));
        return cotizaciones.buscar(texto == null ? "" : texto.trim(), estado, clienteId, orden)
                .map(mapper::aResumen);
    }

    @Transactional(readOnly = true)
    public OpcionesCotizacionResponse opciones() {
        return new OpcionesCotizacionResponse(
                productos.findByActivoTrue().stream()
                        .map(p -> new OpcionesCotizacionResponse.ProductoCotizable(
                                p.getId(), p.getCodigoSku(), p.getNombre(), p.getPrecioVenta()))
                        .toList(),
                sucursales.findByActivoTrue().stream()
                        .map(s -> new OpcionesCotizacionResponse.SucursalOpcion(s.getId(), s.getNombre()))
                        .toList(),
                descuentoMaximoPorcentaje);
    }

    @Transactional
    public CotizacionResponse detalle(Integer id) {
        cotizaciones.vencerCotizacionesExpiradas();
        return responder(cargar(id));
    }

    // ------------------------------------------------------------ operaciones --

    @Transactional
    public CotizacionResponse registrar(CotizacionRequest peticion, Long vendedorId) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setVendedorId(vendedorId);
        cotizacion.setFecha(hoy());
        cotizacion.setNumeroCotizacion(Numeracion.provisional());
        aplicar(cotizacion, peticion);
        cotizacion.setEstado(Boolean.TRUE.equals(peticion.borrador())
                ? EstadoCotizacion.BORRADOR
                : EstadoCotizacion.EMITIDA);

        Cotizacion guardada = cotizaciones.saveAndFlush(cotizacion);
        guardada.setNumeroCotizacion(Numeracion.formatear("COT", guardada.getId()));
        return responder(guardada);
    }

    @Transactional
    public CotizacionResponse modificar(Integer id, CotizacionRequest peticion) {
        cotizaciones.vencerCotizacionesExpiradas();
        Cotizacion cotizacion = cargar(id);
        if (!EstadoCotizacion.MODIFICABLES.contains(cotizacion.getEstado())) {
            throw rechazo(HttpStatus.CONFLICT, "La cotización " + cotizacion.getNumeroCotizacion()
                    + " está " + cotizacion.getEstado() + " y ya no se puede modificar.");
        }
        cotizacion.getDetalles().clear();
        aplicar(cotizacion, peticion);
        if (cotizacion.getEstado() == EstadoCotizacion.BORRADOR
                && Boolean.FALSE.equals(peticion.borrador())) {
            emitirDesdeHoy(cotizacion);
        }
        return responder(cotizaciones.saveAndFlush(cotizacion));
    }

    @Transactional
    public CotizacionResponse emitir(Integer id) {
        Cotizacion cotizacion = cargar(id);
        if (cotizacion.getEstado() != EstadoCotizacion.BORRADOR) {
            throw rechazo(HttpStatus.CONFLICT, "Sólo se emite una cotización en BORRADOR.");
        }
        emitirDesdeHoy(cotizacion);
        return responder(cotizacion);
    }

    @Transactional
    public CotizacionResponse aceptar(Integer id) {
        cotizaciones.vencerCotizacionesExpiradas();
        Cotizacion cotizacion = cargar(id);
        if (cotizacion.getEstado() != EstadoCotizacion.EMITIDA) {
            throw rechazo(HttpStatus.CONFLICT,
                    "Sólo se marca como aceptada una cotización EMITIDA y vigente.");
        }
        cotizacion.setEstado(EstadoCotizacion.ACEPTADA);
        return responder(cotizacion);
    }

    @Transactional
    public CotizacionResponse anular(Integer id, String motivo) {
        Cotizacion cotizacion = cargar(id);
        if (cotizacion.getEstado() == EstadoCotizacion.CONVERTIDA
                || cotizacion.getEstado() == EstadoCotizacion.ANULADA) {
            throw rechazo(HttpStatus.CONFLICT, "La cotización " + cotizacion.getNumeroCotizacion()
                    + " está " + cotizacion.getEstado() + " y no se puede anular.");
        }
        cotizacion.setEstado(EstadoCotizacion.ANULADA);
        cotizacion.anotar("Anulada el " + hoy() + ": " + motivo.trim());
        return responder(cotizacion);
    }

    /**
     * Convierte una cotización vigente en un pedido de venta PENDIENTE de la validación
     * comercial (CU17), copiando cliente, renglones y precios cotizados.
     */
    @Transactional
    public PedidoGeneradoResponse convertirEnPedido(Integer id, Integer sucursalId, Long vendedorId) {
        cotizaciones.vencerCotizacionesExpiradas();
        Cotizacion cotizacion = cargar(id);
        if (cotizacion.getEstado() == EstadoCotizacion.BORRADOR) {
            throw rechazo(HttpStatus.CONFLICT, "Emite la cotización antes de convertirla en pedido.");
        }
        if (!cotizacion.estaVigente(hoy())) {
            throw rechazo(HttpStatus.CONFLICT, "La cotización " + cotizacion.getNumeroCotizacion()
                    + " está " + cotizacion.getEstado() + ": sólo se convierte una cotización vigente.");
        }

        Cliente cliente = cotizacion.getCliente();
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El cliente " + cliente.getRazonSocial() + " está inactivo.");
        }
        BigDecimal limite = cotizaciones.limiteCreditoDe(cliente.getId());
        if (limite != null && limite.signum() > 0 && cotizacion.getTotal().compareTo(limite) > 0) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, String.format(
                    "El total Bs %s supera el límite de crédito del cliente (Bs %s).",
                    cotizacion.getTotal(), limite));
        }

        Almacen sucursal = sucursales.findById(sucursalId)
                .filter(s -> Boolean.TRUE.equals(s.getActivo()))
                .orElseThrow(() -> rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La sucursal " + sucursalId + " no existe o está inactiva."));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setVendedorId(vendedorId);
        pedido.setSucursal(sucursal);
        pedido.setFecha(hoy());
        pedido.setNumeroPedido(Numeracion.provisional());
        pedido.setSubtotal(cotizacion.getSubtotal());
        pedido.setDescuento(cotizacion.getDescuento());
        pedido.setTotal(cotizacion.getTotal());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setObservaciones("Generado desde la cotización " + cotizacion.getNumeroCotizacion());
        for (CotizacionDetalle origen : cotizacion.getDetalles()) {
            PedidoDetalle renglon = new PedidoDetalle();
            renglon.setProducto(origen.getProducto());
            renglon.setCantidad(origen.getCantidad());
            renglon.setPrecioUnitario(origen.getPrecioUnitario());
            renglon.setDescuento(origen.getDescuento());
            renglon.setSubtotal(origen.getSubtotal());
            pedido.agregarDetalle(renglon);
        }
        Pedido guardado = pedidos.saveAndFlush(pedido);
        guardado.setNumeroPedido(Numeracion.formatear("PED", guardado.getId()));

        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacion.anotar("Convertida en el pedido " + guardado.getNumeroPedido() + " el " + hoy());
        return mapper.aPedidoGenerado(guardado, cotizacion);
    }

    // ---------------------------------------------------------------- reglas --

    /** Vuelca cliente, renglones y descuentos de la petición y recalcula los importes. */
    private void aplicar(Cotizacion cotizacion, CotizacionRequest peticion) {
        Cliente cliente = clientes.findById(peticion.clienteId())
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND,
                        "El cliente " + peticion.clienteId() + " no existe."));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El cliente " + cliente.getRazonSocial() + " está inactivo y no se le puede cotizar.");
        }
        cotizacion.setCliente(cliente);
        cotizacion.setValidezDias(peticion.validezDias() == null
                ? VALIDEZ_POR_DEFECTO
                : peticion.validezDias());
        cotizacion.setObservaciones(peticion.observaciones() == null
                ? null
                : peticion.observaciones().trim());

        Set<Long> vistos = new HashSet<>();
        for (ItemCotizacionRequest item : peticion.items()) {
            if (!vistos.add(item.productoId())) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El producto " + item.productoId() + " está repetido: suma las cantidades en un solo renglón.");
            }
            Producto producto = productos.findById(item.productoId())
                    .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND,
                            "El producto " + item.productoId() + " no existe."));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El producto " + producto.getNombre() + " está inactivo.");
            }
            CotizacionDetalle renglon = new CotizacionDetalle();
            renglon.setProducto(producto);
            renglon.setCantidad(item.cantidad().setScale(2, RoundingMode.HALF_UP));
            renglon.setPrecioUnitario(producto.getPrecioVenta());
            renglon.setDescuento(item.descuento() == null
                    ? BigDecimal.ZERO
                    : item.descuento().setScale(2, RoundingMode.HALF_UP));
            if (renglon.getDescuento().compareTo(renglon.importeBruto()) > 0) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El descuento de " + producto.getNombre() + " es mayor que su importe.");
            }
            cotizacion.agregarDetalle(renglon);
        }

        BigDecimal descuentoGlobal = peticion.descuentoGlobal() == null
                ? BigDecimal.ZERO
                : peticion.descuentoGlobal().setScale(2, RoundingMode.HALF_UP);
        cotizacion.calcularTotales(descuentoGlobal);
        validarPoliticaDeDescuentos(cotizacion);
    }

    private void validarPoliticaDeDescuentos(Cotizacion cotizacion) {
        BigDecimal maximo = cotizacion.getSubtotal()
                .multiply(descuentoMaximoPorcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (cotizacion.getDescuento().compareTo(maximo) > 0) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, String.format(
                    "El descuento total (Bs %s) supera el %s%% permitido por la política de descuentos (Bs %s).",
                    cotizacion.getDescuento(), descuentoMaximoPorcentaje.stripTrailingZeros().toPlainString(),
                    maximo));
        }
    }

    /** Al emitir un borrador, el plazo de validez empieza a correr desde hoy. */
    private void emitirDesdeHoy(Cotizacion cotizacion) {
        cotizacion.setFecha(hoy());
        cotizacion.setEstado(EstadoCotizacion.EMITIDA);
    }

    private Cotizacion cargar(Integer id) {
        return cotizaciones.buscarCompleta(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "La cotización " + id + " no existe."));
    }

    private CotizacionResponse responder(Cotizacion cotizacion) {
        String vendedor = usuarios.findById(cotizacion.getVendedorId())
                .map(u -> u.getEmail())
                .orElse("Usuario #" + cotizacion.getVendedorId());
        return mapper.aRespuesta(cotizacion, vendedor, hoy());
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String motivo) {
        return new ResponseStatusException(estado, motivo);
    }
}
