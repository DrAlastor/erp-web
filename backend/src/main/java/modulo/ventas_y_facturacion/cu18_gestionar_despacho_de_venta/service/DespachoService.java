package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.service;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto.MovimientoInventarioRequest;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.service.MovimientoInventarioService;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Numeracion;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.entity.PedidoDetalle;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResumenResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.FaltanteStock;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.ItemDespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.PedidoPorDespacharResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.PendienteDespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.DespachoDetalle;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.NotaDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento.DespachoAnulado;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento.DespachoConfirmado;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.exception.StockInsuficienteException;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.mapper.DespachoMapper;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.repository.NotaDespachoRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU18 - Gestionar Despacho de Venta.
 *
 * <p>Reglas que aplica (las mismas del documento del sprint):
 * <ul>
 *   <li>Sólo se despachan pedidos aprobados por la validación comercial (CU17).</li>
 *   <li>Lo despachado de cada producto no puede superar lo pendiente del pedido.</li>
 *   <li>El stock de la sucursal nunca queda negativo: si falta algo, no sale nada.</li>
 *   <li>Confirmar descuenta el stock y deja la SALIDA en el Kardex (vía CU11) con el
 *       número de despacho como documento de referencia.</li>
 *   <li>Todo es atómico: si algo falla a mitad de camino, no se guarda nada.</li>
 *   <li>Anular un despacho que ya salió devuelve el stock con una ENTRADA en el Kardex.</li>
 * </ul>
 */
@Service
public class DespachoService {

    /** Notas que ocupan cantidad del pedido: todas menos las anuladas. */
    private static final Set<EstadoDespacho> COMPROMETEN =
            EnumSet.of(EstadoDespacho.PREPARACION, EstadoDespacho.DESPACHADO, EstadoDespacho.ENTREGADO);

    /** Notas cuya mercadería ya salió del almacén. */
    private static final Set<EstadoDespacho> SALIERON =
            EnumSet.of(EstadoDespacho.DESPACHADO, EstadoDespacho.ENTREGADO);

    private final NotaDespachoRepository notas;
    private final PedidoRepository pedidos;
    private final AlmacenRepository sucursales;
    private final UsuarioRepository usuarios;
    private final MovimientoInventarioService inventario;
    private final ApplicationEventPublisher eventos;
    private final DespachoMapper mapper;

    public DespachoService(NotaDespachoRepository notas,
                           PedidoRepository pedidos,
                           AlmacenRepository sucursales,
                           UsuarioRepository usuarios,
                           MovimientoInventarioService inventario,
                           ApplicationEventPublisher eventos,
                           DespachoMapper mapper) {
        this.notas = notas;
        this.pedidos = pedidos;
        this.sucursales = sucursales;
        this.usuarios = usuarios;
        this.inventario = inventario;
        this.eventos = eventos;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------- consultas --

    /** Pedidos aprobados que todavía tienen algo por despachar, con su stock. */
    @Transactional(readOnly = true)
    public List<PedidoPorDespacharResponse> pedidosPorDespachar(Integer sucursalId) {
        List<PedidoPorDespacharResponse> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos.buscarPorEstados(EstadoPedido.DESPACHABLES, sucursalId)) {
            Map<Long, BigDecimal> comprometido = sumar(pedido.getId(), COMPROMETEN, null);
            List<PendienteDespachoResponse> items = new ArrayList<>();
            for (Linea linea : lineasDelPedido(pedido).values()) {
                Producto producto = linea.producto();
                BigDecimal ocupado = comprometido.getOrDefault(producto.getId(), BigDecimal.ZERO);
                BigDecimal pendiente = linea.cantidad().subtract(ocupado);
                items.add(new PendienteDespachoResponse(
                        producto.getId(),
                        producto.getCodigoSku(),
                        producto.getNombre(),
                        linea.cantidad(),
                        ocupado,
                        pendiente.max(BigDecimal.ZERO),
                        inventario.obtenerStockDisponible(producto.getId(), pedido.getSucursal().getId())));
            }
            if (items.stream().anyMatch(i -> i.pendiente().signum() > 0)) {
                resultado.add(new PedidoPorDespacharResponse(
                        pedido.getId(),
                        pedido.getNumeroPedido(),
                        pedido.getFecha(),
                        pedido.getEstado().name(),
                        pedido.getCliente().getId(),
                        pedido.getCliente().getRazonSocial(),
                        pedido.getSucursal().getId(),
                        pedido.getSucursal().getNombre(),
                        items));
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Page<DespachoResumenResponse> listar(String texto, EstadoDespacho estado, Integer pedidoId,
                                                Integer sucursalId, int pagina, int tamanio) {
        PageRequest orden = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "id"));
        return notas.buscar(texto == null ? "" : texto.trim(), estado, pedidoId, sucursalId, orden)
                .map(mapper::aResumen);
    }

    @Transactional(readOnly = true)
    public DespachoResponse detalle(Integer id) {
        return responder(cargar(id));
    }

    // ------------------------------------------------------------ operaciones --

    /** Registra la nota en PREPARACION: valida pendiente y stock, pero todavía no mueve nada. */
    @Transactional
    public DespachoResponse registrar(DespachoRequest peticion, Long usuarioId) {
        Pedido pedido = pedidos.bloquear(peticion.pedidoId())
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND,
                        "El pedido " + peticion.pedidoId() + " no existe."));
        exigirDespachable(pedido);

        Almacen sucursal = sucursales.findById(peticion.sucursalId())
                .filter(s -> Boolean.TRUE.equals(s.getActivo()))
                .orElseThrow(() -> rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La sucursal " + peticion.sucursalId() + " no existe o está inactiva."));

        Map<Long, Linea> lineasDelPedido = lineasDelPedido(pedido);

        NotaDespacho nota = new NotaDespacho();
        nota.setPedido(pedido);
        nota.setSucursal(sucursal);
        nota.setDespachadoPor(usuarioId);
        nota.setNumeroDespacho(Numeracion.provisional());
        nota.setFecha(LocalDateTime.now());
        nota.setConductor(textoOpcional(peticion.conductor()));
        nota.setPlaca(textoOpcional(peticion.placa()));
        nota.setObservaciones(textoOpcional(peticion.observaciones()));
        nota.setEstado(EstadoDespacho.PREPARACION);

        Set<Long> vistos = new HashSet<>();
        for (ItemDespachoRequest item : peticion.items()) {
            if (!vistos.add(item.productoId())) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El producto " + item.productoId() + " está repetido en el despacho.");
            }
            Linea linea = lineasDelPedido.get(item.productoId());
            if (linea == null) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "El producto " + item.productoId()
                        + " no forma parte del pedido " + pedido.getNumeroPedido() + ".");
            }
            DespachoDetalle renglon = new DespachoDetalle();
            renglon.setProducto(linea.producto());
            renglon.setCantidad(item.cantidad().setScale(2, RoundingMode.HALF_UP));
            nota.agregarDetalle(renglon);
        }

        exigirDentroDeLoPendiente(pedido, nota, null);
        exigirStock(nota);

        NotaDespacho guardada = notas.saveAndFlush(nota);
        guardada.setNumeroDespacho(Numeracion.formatear("DES", guardada.getId()));
        return responder(guardada);
    }

    /**
     * Confirma la salida: descuenta el stock, registra la SALIDA en el Kardex, actualiza el
     * pedido y avisa a contabilidad con {@link DespachoConfirmado}.
     */
    @Transactional
    public DespachoResponse confirmar(Integer id) {
        NotaDespacho nota = bloquear(id);
        if (nota.getEstado() != EstadoDespacho.PREPARACION) {
            throw rechazo(HttpStatus.CONFLICT, "El despacho " + nota.getNumeroDespacho()
                    + " está " + nota.getEstado() + ": sólo se confirma uno en PREPARACION.");
        }
        Pedido pedido = pedidos.bloquear(nota.getPedido().getId()).orElseThrow();
        exigirDespachable(pedido);
        exigirDentroDeLoPendiente(pedido, nota, nota.getId());
        exigirStock(nota);

        BigDecimal costoTotal = BigDecimal.ZERO;
        for (DespachoDetalle renglon : nota.getDetalles()) {
            Producto producto = renglon.getProducto();
            BigDecimal costo = producto.getCostoPromedio() == null ? BigDecimal.ZERO : producto.getCostoPromedio();
            inventario.registrarMovimiento(MovimientoInventarioRequest.builder()
                    .productoId(producto.getId())
                    .almacenId(nota.getSucursal().getId())
                    .tipoMovimiento("SALIDA")
                    .cantidad(renglon.getCantidad())
                    .costoUnitario(costo)
                    .referenciaDoc(nota.getNumeroDespacho())
                    .motivo("Despacho de venta del pedido " + pedido.getNumeroPedido())
                    .build(), null);
            costoTotal = costoTotal.add(renglon.getCantidad().multiply(costo));
        }

        nota.setEstado(EstadoDespacho.DESPACHADO);
        nota.setFecha(LocalDateTime.now());
        notas.flush();
        recalcularEstadoDelPedido(pedido);

        eventos.publishEvent(new DespachoConfirmado(nota.getId(), nota.getNumeroDespacho(),
                pedido.getId(), nota.getSucursal().getId(), costoTotal.setScale(2, RoundingMode.HALF_UP)));
        return responder(nota);
    }

    @Transactional
    public DespachoResponse marcarEntregado(Integer id) {
        NotaDespacho nota = bloquear(id);
        if (nota.getEstado() != EstadoDespacho.DESPACHADO) {
            throw rechazo(HttpStatus.CONFLICT, "Sólo se marca como entregado un despacho DESPACHADO.");
        }
        nota.setEstado(EstadoDespacho.ENTREGADO);
        nota.anotar("Entregado al cliente el " + LocalDateTime.now().withNano(0));
        return responder(nota);
    }

    /**
     * Anula la nota. Si estaba en preparación no había movido nada; si ya había salido,
     * devuelve el stock con una ENTRADA en el Kardex y avisa a contabilidad.
     */
    @Transactional
    public DespachoResponse anular(Integer id, String motivo) {
        NotaDespacho nota = bloquear(id);
        EstadoDespacho anterior = nota.getEstado();
        if (anterior == EstadoDespacho.ENTREGADO || anterior == EstadoDespacho.ANULADO) {
            throw rechazo(HttpStatus.CONFLICT, "El despacho " + nota.getNumeroDespacho()
                    + " está " + anterior + " y no se puede anular.");
        }
        Pedido pedido = pedidos.bloquear(nota.getPedido().getId()).orElseThrow();
        if (pedido.getEstado() == EstadoPedido.COMPLETADO || pedido.getEstado() == EstadoPedido.ANULADO) {
            throw rechazo(HttpStatus.CONFLICT, "El pedido " + pedido.getNumeroPedido() + " está "
                    + pedido.getEstado() + ": sus despachos ya no se pueden anular.");
        }

        BigDecimal costoTotal = BigDecimal.ZERO;
        if (anterior == EstadoDespacho.DESPACHADO) {
            for (DespachoDetalle renglon : nota.getDetalles()) {
                Producto producto = renglon.getProducto();
                BigDecimal costo = producto.getCostoPromedio() == null ? BigDecimal.ZERO : producto.getCostoPromedio();
                inventario.registrarMovimiento(MovimientoInventarioRequest.builder()
                        .productoId(producto.getId())
                        .almacenId(nota.getSucursal().getId())
                        .tipoMovimiento("ENTRADA")
                        .cantidad(renglon.getCantidad())
                        .costoUnitario(costo)
                        .referenciaDoc(nota.getNumeroDespacho())
                        .motivo("Anulación del despacho " + nota.getNumeroDespacho())
                        .build(), null);
                costoTotal = costoTotal.add(renglon.getCantidad().multiply(costo));
            }
        }

        nota.setEstado(EstadoDespacho.ANULADO);
        nota.anotar("Anulado el " + LocalDateTime.now().withNano(0) + ": " + motivo.trim());
        notas.flush();

        if (anterior == EstadoDespacho.DESPACHADO) {
            recalcularEstadoDelPedido(pedido);
            eventos.publishEvent(new DespachoAnulado(nota.getId(), nota.getNumeroDespacho(),
                    pedido.getId(), costoTotal.setScale(2, RoundingMode.HALF_UP), motivo.trim()));
        }
        return responder(nota);
    }

    // ---------------------------------------------------------------- reglas --

    private void exigirDespachable(Pedido pedido) {
        if (!EstadoPedido.DESPACHABLES.contains(pedido.getEstado())) {
            throw rechazo(HttpStatus.CONFLICT, "El pedido " + pedido.getNumeroPedido() + " está "
                    + pedido.getEstado() + ": sólo se despachan pedidos aprobados por la validación comercial.");
        }
    }

    /** Lo de esta nota más lo ya comprometido en otras no puede superar lo pedido. */
    private void exigirDentroDeLoPendiente(Pedido pedido, NotaDespacho nota, Integer excluirId) {
        Map<Long, Linea> lineas = lineasDelPedido(pedido);
        Map<Long, BigDecimal> comprometido = sumar(pedido.getId(), COMPROMETEN, excluirId);
        for (DespachoDetalle renglon : nota.getDetalles()) {
            Producto producto = renglon.getProducto();
            Linea linea = lineas.get(producto.getId());
            BigDecimal pedidoCant = linea == null ? BigDecimal.ZERO : linea.cantidad();
            BigDecimal pendiente = pedidoCant.subtract(comprometido.getOrDefault(producto.getId(), BigDecimal.ZERO));
            if (renglon.getCantidad().compareTo(pendiente) > 0) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, String.format(
                        "De %s quedan %s por despachar en el pedido %s y se intentan despachar %s.",
                        producto.getNombre(), pendiente.max(BigDecimal.ZERO), pedido.getNumeroPedido(),
                        renglon.getCantidad()));
            }
        }
    }

    /** Revisa todos los renglones y, si falta algo, informa todos los faltantes juntos. */
    private void exigirStock(NotaDespacho nota) {
        List<FaltanteStock> faltantes = new ArrayList<>();
        for (DespachoDetalle renglon : nota.getDetalles()) {
            Producto producto = renglon.getProducto();
            BigDecimal disponible = inventario.obtenerStockDisponible(producto.getId(), nota.getSucursal().getId());
            if (disponible.compareTo(renglon.getCantidad()) < 0) {
                faltantes.add(new FaltanteStock(producto.getId(), producto.getNombre(),
                        renglon.getCantidad(), disponible));
            }
        }
        if (!faltantes.isEmpty()) {
            throw new StockInsuficienteException(faltantes);
        }
    }

    /**
     * DESPACHADO si ya salió todo, DESPACHO_PARCIAL si salió una parte, APROBADO si nada
     * (por ejemplo, después de anular el único despacho).
     */
    private void recalcularEstadoDelPedido(Pedido pedido) {
        Map<Long, BigDecimal> salio = sumar(pedido.getId(), SALIERON, null);
        boolean todo = true;
        boolean algo = false;
        for (Map.Entry<Long, Linea> renglon : lineasDelPedido(pedido).entrySet()) {
            BigDecimal despachado = salio.getOrDefault(renglon.getKey(), BigDecimal.ZERO);
            if (despachado.signum() > 0) algo = true;
            if (despachado.compareTo(renglon.getValue().cantidad()) < 0) todo = false;
        }
        pedido.setEstado(todo ? EstadoPedido.DESPACHADO
                : algo ? EstadoPedido.DESPACHO_PARCIAL
                : EstadoPedido.APROBADO);
    }

    /** Un producto del pedido y la cantidad pedida en total. */
    private record Linea(Producto producto, BigDecimal cantidad) {
    }

    /**
     * Cantidad pedida de cada producto, por id de producto (sumando si un producto aparece
     * en dos renglones).
     */
    private Map<Long, Linea> lineasDelPedido(Pedido pedido) {
        Map<Long, Linea> lineas = new LinkedHashMap<>();
        for (PedidoDetalle renglon : pedido.getDetalles()) {
            lineas.merge(renglon.getProducto().getId(),
                    new Linea(renglon.getProducto(), renglon.getCantidad()),
                    (a, b) -> new Linea(a.producto(), a.cantidad().add(b.cantidad())));
        }
        return lineas;
    }

    private Map<Long, BigDecimal> sumar(Integer pedidoId, Set<EstadoDespacho> estados, Integer excluirId) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        for (Object[] fila : notas.sumarPorProducto(pedidoId, estados, excluirId)) {
            resultado.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return resultado;
    }

    private NotaDespacho cargar(Integer id) {
        return notas.buscarCompleta(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "El despacho " + id + " no existe."));
    }

    private NotaDespacho bloquear(Integer id) {
        notas.bloquear(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "El despacho " + id + " no existe."));
        return cargar(id);
    }

    private DespachoResponse responder(NotaDespacho nota) {
        String despachadoPor = usuarios.findById(nota.getDespachadoPor())
                .map(u -> u.getEmail())
                .orElse("Usuario #" + nota.getDespachadoPor());
        return mapper.aRespuesta(nota, despachadoPor);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String motivo) {
        return new ResponseStatusException(estado, motivo);
    }
}
