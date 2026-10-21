package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.ClienteOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoDetalleRequest;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoRequest;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.ProductoOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.SucursalOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoVenta;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity.PedidoVentaDetalle;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.mapper.PedidoVentaMapper;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.ClienteRef;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.PedidoCabecera;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.ProductoRef;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository.SucursalRef;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HU-16 / CU16: pedidos de venta.
 *
 * <p>Un pedido nace PENDIENTE, solo se modifica mientras sigue PENDIENTE y se anula
 * (PENDIENTE o APROBADO a ANULADO, estado final). Aprobarlo o rechazarlo es tarea de la
 * validacion comercial (CU17) y despacharlo de la CU18, con los estados de
 * {@code compartido.entity.EstadoPedido} de esas ramas: aqui no se inventan estados nuevos.
 * No hay borrado fisico. El stock solo se valida: descontarlo y registrar kardex
 * corresponde a despacho y facturacion.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoVentaService {

    private static final int MAX_OPCIONES = 20;

    private final PedidoVentaRepository pedidos;
    private final PedidoVentaConsultaRepository consultas;
    private final PedidoVentaMapper mapper;
    private final VendedorActualResolver vendedorResolver;

    public Page<PedidoResumenResponse> list(String search, String estado, int page, int size) {
        long total = consultas.contar(search, estado);
        List<PedidoResumenResponse> filas = consultas
                .listar(search, estado, size, (long) page * size)
                .stream()
                .map(mapper::toResumen)
                .toList();
        return new PageImpl<>(filas, PageRequest.of(page, size, Sort.by("id").descending()), total);
    }

    public PedidoResponse detail(Long id) {
        return respuesta(find(id));
    }

    @Transactional
    public PedidoResponse create(PedidoRequest request, Authentication authentication) {
        Long vendedorId = vendedorResolver.resolver(authentication);
        LocalDate fecha = request.fecha() == null ? LocalDate.now() : request.fecha();

        PedidoVenta pedido = new PedidoVenta();
        pedido.setVendedorId(vendedorId);
        pedido.setNumeroPedido(siguienteNumero(fecha.getYear()));
        pedido.setEstado(PedidoVenta.PENDIENTE);
        aplicar(pedido, request, fecha);
        return respuesta(pedidos.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse update(Long id, PedidoRequest request) {
        PedidoVenta pedido = find(id);
        if (!PedidoVenta.PENDIENTE.equals(pedido.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Solo se pueden modificar pedidos pendientes");
        }
        LocalDate fecha = request.fecha() == null ? pedido.getFecha() : request.fecha();
        aplicar(pedido, request, fecha);
        return respuesta(pedidos.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse anular(Long id, String motivo) {
        PedidoVenta pedido = find(id);
        if (PedidoVenta.ANULADO.equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya está anulado");
        }
        if (!PedidoVenta.PENDIENTE.equals(pedido.getEstado())
                && !PedidoVenta.APROBADO.equals(pedido.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden anular pedidos pendientes o aprobados sin despachar");
        }
        String nota = "[ANULADO] " + motivo.trim();
        String previas = pedido.getObservaciones();
        pedido.setObservaciones(previas == null || previas.isBlank() ? nota : previas + "\n" + nota);
        pedido.setEstado(PedidoVenta.ANULADO);
        return respuesta(pedidos.saveAndFlush(pedido));
    }

    public List<ClienteOpcionResponse> clientes(String search) {
        return consultas.clientesActivos(search, MAX_OPCIONES).stream()
                .map(c -> new ClienteOpcionResponse(c.id(), c.razonSocial(), c.nitCi()))
                .toList();
    }

    public List<SucursalOpcionResponse> sucursales() {
        return consultas.sucursalesActivas().stream()
                .map(s -> new SucursalOpcionResponse(s.id(), s.nombre()))
                .toList();
    }

    public List<ProductoOpcionResponse> productos(String search, Long sucursalId) {
        return consultas.productosActivos(search, sucursalId, MAX_OPCIONES).stream()
                .map(p -> new ProductoOpcionResponse(
                        p.id(), p.codigo(), p.nombre(), p.precioVenta(), p.stockDisponible()))
                .toList();
    }

    // ------------------------------------------------------------------ internos

    private PedidoVenta find(Long id) {
        return pedidos.findConDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido inexistente"));
    }

    /** Valida el cuerpo, recalcula importes y deja cabecera y lineas listas para guardar. */
    private void aplicar(PedidoVenta pedido, PedidoRequest request, LocalDate fecha) {
        List<PedidoDetalleRequest> detalles = request.detalles();
        if (detalles == null || detalles.isEmpty()) {
            throw rechazo("El pedido debe tener al menos una línea de detalle");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (PedidoDetalleRequest d : detalles) {
            if (!ids.add(d.productoId())) {
                throw rechazo("El producto " + d.productoId() + " está repetido en el detalle");
            }
        }

        ClienteRef cliente = consultas.cliente(request.clienteId())
                .orElseThrow(() -> rechazo("El cliente seleccionado no existe"));
        if (!cliente.activo()) throw rechazo("El cliente " + cliente.razonSocial() + " está inactivo");

        SucursalRef sucursal = consultas.sucursal(request.sucursalId())
                .orElseThrow(() -> rechazo("La sucursal seleccionada no existe"));
        if (!sucursal.activo()) throw rechazo("La sucursal " + sucursal.nombre() + " está inactiva");

        Map<Long, ProductoRef> productos = consultas.productos(ids);
        List<String> inexistentes = ids.stream()
                .filter(id -> !productos.containsKey(id))
                .map(String::valueOf)
                .toList();
        if (!inexistentes.isEmpty()) {
            throw rechazo("Productos inexistentes: " + String.join(", ", inexistentes));
        }
        List<String> inactivos = productos.values().stream()
                .filter(p -> !p.activo())
                .map(ProductoRef::nombre)
                .toList();
        if (!inactivos.isEmpty()) {
            throw rechazo("Productos inactivos: " + String.join(", ", inactivos));
        }

        List<CalculadoraPedido.LineaEntrada> entradas = detalles.stream()
                .map(d -> {
                    ProductoRef p = productos.get(d.productoId());
                    return new CalculadoraPedido.LineaEntrada(
                            d.productoId(),
                            p.nombre(),
                            d.cantidad(),
                            d.precioUnitario() == null ? p.precioVenta() : d.precioUnitario(),
                            d.descuento() == null ? BigDecimal.ZERO : d.descuento());
                })
                .toList();
        CalculadoraPedido.Resultado calculo = CalculadoraPedido.calcular(
                entradas, request.descuento() == null ? BigDecimal.ZERO : request.descuento());

        validarStock(sucursal.id(), entradas);

        pedido.setClienteId(cliente.id());
        pedido.setSucursalId(sucursal.id());
        pedido.setFecha(fecha);
        pedido.setObservaciones(limpiar(request.observaciones()));
        pedido.setSubtotal(calculo.subtotal());
        pedido.setDescuento(calculo.descuento());
        pedido.setTotal(calculo.total());

        List<PedidoVentaDetalle> lineas = new ArrayList<>();
        for (CalculadoraPedido.LineaCalculada l : calculo.lineas()) {
            PedidoVentaDetalle linea = new PedidoVentaDetalle();
            linea.setProductoId(l.productoId());
            linea.setCantidad(l.cantidad());
            linea.setPrecioUnitario(l.precioUnitario());
            linea.setDescuento(l.descuento());
            linea.setSubtotal(l.subtotal());
            lineas.add(linea);
        }
        pedido.reemplazarDetalles(lineas);
    }

    /** Cada cantidad debe caber en el stock disponible del producto en la sucursal. */
    private void validarStock(Long sucursalId, List<CalculadoraPedido.LineaEntrada> lineas) {
        Map<Long, BigDecimal> stock = consultas.stock(
                sucursalId,
                lineas.stream().map(CalculadoraPedido.LineaEntrada::productoId).toList());
        List<String> faltantes = new ArrayList<>();
        for (CalculadoraPedido.LineaEntrada l : lineas) {
            BigDecimal disponible = stock.getOrDefault(l.productoId(), BigDecimal.ZERO);
            if (l.cantidad().compareTo(disponible) > 0) {
                faltantes.add(l.productoNombre()
                        + " (disponible " + numero(disponible)
                        + ", solicitado " + numero(l.cantidad()) + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Stock insuficiente: " + String.join("; ", faltantes));
        }
    }

    /** Siguiente numero {@code PED-<yyyy>-<0001>}: mayor correlativo del año + 1. */
    private String siguienteNumero(int anio) {
        String prefijo = "PED-" + anio + "-";
        int siguiente = pedidos.maxCorrelativo(prefijo.length() + 1, "^" + prefijo + "[0-9]+$") + 1;
        return prefijo + String.format("%04d", siguiente);
    }

    private PedidoResponse respuesta(PedidoVenta pedido) {
        PedidoCabecera cabecera = consultas.cabecera(pedido.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido inexistente"));
        return mapper.toResponse(cabecera, pedido, consultas.productos(productoIds(pedido)));
    }

    private Set<Long> productoIds(PedidoVenta pedido) {
        return pedido.getDetalles().stream()
                .map(PedidoVentaDetalle::getProductoId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String nombre(ProductoRef producto, Long id) {
        return producto == null ? "Producto " + id : producto.nombre();
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String limpiar(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
