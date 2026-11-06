package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service;

import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.entity.PedidoDetalle;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.compartido.service.Montos;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.repository.NotaDespachoRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CatalogosVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facturación de un pedido ya despachado. El despacho (CU18) es la salida física: la venta
 * solo emite el comprobante de lo que salió y todavía no se facturó, sin volver a mover stock.
 * El precio y el descuento salen del pedido, porque es lo que aprobó la validación comercial.
 */
@Service
@RequiredArgsConstructor
public class FacturacionPedidoService {

    private static final Set<EstadoDespacho> SALIERON =
            EnumSet.of(EstadoDespacho.DESPACHADO, EstadoDespacho.ENTREGADO);
    private static final Set<EstadoPedido> FACTURABLES =
            EnumSet.of(EstadoPedido.DESPACHO_PARCIAL, EstadoPedido.DESPACHADO);

    private final PedidoRepository pedidos;
    private final NotaDespachoRepository despachos;

    /** Comprueba que la venta corresponde al pedido y que factura solo lo despachado pendiente. */
    public Pedido validar(VentaRequest request) {
        if (request.cotizacionId() != null && request.cotizacionId() > 0) {
            throw rechazo("Una venta se emite desde un pedido o desde una cotización, no desde ambos.");
        }
        Pedido pedido = pedidos.findById(request.pedidoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El pedido " + request.pedidoId() + " no existe."));
        if (!FACTURABLES.contains(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido " + pedido.getNumeroPedido()
                    + " está " + pedido.getEstado() + ": solo se facturan pedidos con mercadería despachada.");
        }
        if (!pedido.getCliente().getId().equals(request.clienteId())) {
            throw rechazo("El cliente de la venta no es el del pedido " + pedido.getNumeroPedido() + ".");
        }
        if (!pedido.getSucursal().getId().equals(request.sucursalId())) {
            throw rechazo("La sucursal de la venta no es la del pedido " + pedido.getNumeroPedido() + ".");
        }
        Map<Long, BigDecimal> pendiente = porFacturar(pedido);
        for (VentaRequest.Linea linea : request.detalles()) {
            BigDecimal disponible = pendiente.getOrDefault(linea.productoId(), BigDecimal.ZERO);
            if (linea.cantidad().compareTo(disponible) > 0) {
                throw rechazo(String.format("Del producto #%d quedan %s unidades despachadas sin facturar en el pedido %s.",
                        linea.productoId(), disponible.stripTrailingZeros().toPlainString(), pedido.getNumeroPedido()));
            }
        }
        return pedido;
    }

    /** Precio de la línea del pedido. */
    public BigDecimal precio(Pedido pedido, Long productoId) {
        return Montos.redondear(linea(pedido, productoId).getPrecioUnitario());
    }

    /** Parte del descuento del pedido (de línea y global) que corresponde a la cantidad facturada. */
    public BigDecimal descuento(Pedido pedido, Long productoId, BigDecimal cantidad) {
        PedidoDetalle linea = linea(pedido, productoId);
        BigDecimal deLinea = linea.getDescuento() == null ? BigDecimal.ZERO : linea.getDescuento();
        BigDecimal global = BigDecimal.ZERO;
        if (pedido.getDescuento() != null && pedido.getSubtotal().signum() > 0) {
            global = pedido.getDescuento().multiply(linea.getSubtotal())
                    .divide(pedido.getSubtotal(), 6, RoundingMode.HALF_UP);
        }
        return Montos.redondear(deLinea.add(global).multiply(cantidad)
                .divide(linea.getCantidad(), 6, RoundingMode.HALF_UP));
    }

    /** COMPLETADO cuando todo lo pedido salió y está facturado; si deja de estarlo, vuelve a DESPACHADO. */
    public void actualizarEstado(Long pedidoId) {
        Pedido pedido = pedidos.bloquear(pedidoId.intValue()).orElseThrow();
        Map<Long, BigDecimal> facturado = sumar(despachos.facturadoPorProducto(pedidoId));
        boolean todo = true;
        for (Map.Entry<Long, BigDecimal> renglon : cantidades(pedido).entrySet()) {
            if (facturado.getOrDefault(renglon.getKey(), BigDecimal.ZERO).compareTo(renglon.getValue()) < 0) {
                todo = false;
            }
        }
        if (todo && pedido.getEstado() == EstadoPedido.DESPACHADO) {
            pedido.setEstado(EstadoPedido.COMPLETADO);
        } else if (!todo && pedido.getEstado() == EstadoPedido.COMPLETADO) {
            pedido.setEstado(EstadoPedido.DESPACHADO);
        }
    }

    /** Pedidos que el punto de venta puede facturar, con lo que queda pendiente de cada uno. */
    public List<CatalogosVentaResponse.PedidoPorFacturar> porFacturar() {
        List<CatalogosVentaResponse.PedidoPorFacturar> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos.buscarPorEstados(FACTURABLES, null)) {
            List<CatalogosVentaResponse.PedidoLinea> lineas = new ArrayList<>();
            Map<Long, BigDecimal> pendiente = porFacturar(pedido);
            for (PedidoDetalle detalle : pedido.getDetalles()) {
                Long productoId = detalle.getProducto().getId();
                BigDecimal cantidad = pendiente.getOrDefault(productoId, BigDecimal.ZERO);
                if (cantidad.signum() > 0) {
                    lineas.add(new CatalogosVentaResponse.PedidoLinea(productoId, detalle.getProducto().getCodigoSku(),
                            detalle.getProducto().getNombre(), cantidad, precio(pedido, productoId),
                            descuento(pedido, productoId, cantidad)));
                }
            }
            if (!lineas.isEmpty()) {
                resultado.add(new CatalogosVentaResponse.PedidoPorFacturar(pedido.getId(), pedido.getNumeroPedido(),
                        pedido.getCliente().getId(), pedido.getCliente().getRazonSocial(), pedido.getCliente().getNitCi(),
                        pedido.getSucursal().getId(), lineas));
            }
        }
        return resultado;
    }

    private Map<Long, BigDecimal> porFacturar(Pedido pedido) {
        Map<Long, BigDecimal> resultado = sumar(despachos.sumarPorProducto(pedido.getId(), SALIERON, null));
        Map<Long, BigDecimal> facturado = sumar(despachos.facturadoPorProducto(pedido.getId().longValue()));
        facturado.forEach((producto, cantidad) -> resultado.computeIfPresent(producto, (k, v) -> v.subtract(cantidad)));
        return resultado;
    }

    private Map<Long, BigDecimal> cantidades(Pedido pedido) {
        Map<Long, BigDecimal> resultado = new LinkedHashMap<>();
        pedido.getDetalles().forEach(d -> resultado.merge(d.getProducto().getId(), d.getCantidad(), BigDecimal::add));
        return resultado;
    }

    private PedidoDetalle linea(Pedido pedido, Long productoId) {
        return pedido.getDetalles().stream()
                .filter(d -> d.getProducto().getId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> rechazo("El producto #" + productoId + " no forma parte del pedido "
                        + pedido.getNumeroPedido() + "."));
    }

    private static Map<Long, BigDecimal> sumar(List<Object[]> filas) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        for (Object[] fila : filas) {
            resultado.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return resultado;
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
