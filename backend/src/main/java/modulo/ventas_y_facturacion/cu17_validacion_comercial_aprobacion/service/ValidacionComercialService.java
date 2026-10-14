package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.StockAlmacenRepository;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity.PedidoAValidar;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity.PedidoAValidarDetalle;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository.PedidoAValidarRepository;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.LineaPedidoResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.ValidacionPedidoResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.mapper.ValidacionMapper;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository.CreditoClienteRepository;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository.CreditoClienteRepository.DatosCliente;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository.CreditoClienteRepository.DeudaCliente;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.repository.ExistenciaBloqueoRepository;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Cliente;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Deuda;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Linea;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Regla;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CU17 - Validación Comercial y Aprobación. Evalúa un pedido pendiente contra las reglas
 * comerciales (cliente, mora, crédito, existencias y descuento) y registra la decisión.
 *
 * <p>La aprobación se vuelve a evaluar en el servidor al momento de decidir: lo que se vio
 * en pantalla pudo cambiar. Quedan trazados el usuario, la fecha y la justificación en las
 * observaciones del propio pedido.
 */
@Service
@RequiredArgsConstructor
public class ValidacionComercialService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PedidoAValidarRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final StockAlmacenRepository stockRepository;
    private final CreditoClienteRepository creditoRepository;
    private final ExistenciaBloqueoRepository existenciaBloqueo;
    private final ValidacionMapper mapper;

    /** Estados que se pueden filtrar en la bandeja; vacío o TODOS devuelve todos los pedidos. */
    private static final Set<String> ESTADOS = Set.of(
            PedidoAValidar.PENDIENTE, PedidoAValidar.APROBADO, PedidoAValidar.RECHAZADO,
            "DESPACHO_PARCIAL", "DESPACHADO", "COMPLETADO", "ANULADO");

    @Transactional(readOnly = true)
    public Page<PedidoResumenResponse> listar(String estado, int page, int size) {
        String filtro = null;
        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("TODOS")) {
            filtro = estado.trim().toUpperCase();
            if (!ESTADOS.contains(filtro)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Estado no válido: '" + estado
                                + "'. Use PENDIENTE, APROBADO, RECHAZADO, DESPACHO_PARCIAL, DESPACHADO, COMPLETADO, ANULADO o TODOS.");
            }
        }
        Page<PedidoAValidar> pedidos = pedidoRepository.buscarPorEstado(filtro, PageRequest.of(page, size));
        Map<Integer, String> nombres = creditoRepository.razonesSociales(
                pedidos.getContent().stream().map(PedidoAValidar::getClienteId).collect(Collectors.toSet()));
        return pedidos.map(p -> mapper.toResumen(p, nombres.get(p.getClienteId())));
    }

    @Transactional(readOnly = true)
    public ValidacionPedidoResponse evaluar(Integer pedidoId) {
        PedidoAValidar pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> pedidoNoEncontrado(pedidoId));
        return construir(pedido, evaluacion(pedido));
    }

    @Transactional
    public ValidacionPedidoResponse aprobar(Integer pedidoId, String observacion, String actor) {
        PedidoAValidar pedido = pedidoPendienteBloqueado(pedidoId);
        bloquearExistencias(pedido);
        Evaluacion evaluacion = evaluacion(pedido);

        List<Regla> bloqueantes = evaluacion.reglas().stream().filter(Regla::impideAprobar).toList();
        if (!bloqueantes.isEmpty()) {
            String motivos = bloqueantes.stream().map(Regla::detalle).collect(Collectors.joining(" "));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido no se puede aprobar. " + motivos);
        }
        boolean hayAdvertencias = evaluacion.reglas().stream().anyMatch(r -> !r.cumple());
        if (hayAdvertencias && (observacion == null || observacion.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pedido tiene reglas no cumplidas: indique la justificación para aprobarlo.");
        }

        registrarDecision(pedido, PedidoAValidar.APROBADO, observacion, actor);
        return construir(pedido, evaluacion);
    }

    @Transactional
    public ValidacionPedidoResponse rechazar(Integer pedidoId, String motivo, String actor) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo es obligatorio para rechazar un pedido.");
        }
        PedidoAValidar pedido = pedidoPendienteBloqueado(pedidoId);
        Evaluacion evaluacion = evaluacion(pedido);

        registrarDecision(pedido, PedidoAValidar.RECHAZADO, motivo, actor);
        return construir(pedido, evaluacion);
    }

    private PedidoAValidar pedidoPendienteBloqueado(Integer pedidoId) {
        PedidoAValidar pedido = pedidoRepository.buscarParaDecidir(pedidoId)
                .orElseThrow(() -> pedidoNoEncontrado(pedidoId));
        if (!PedidoAValidar.PENDIENTE.equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido " + pedido.getNumeroPedido() + " ya está en estado " + pedido.getEstado()
                            + " y no admite una nueva decisión.");
        }
        return pedido;
    }

    /** Bloquea las existencias del pedido para que la reserva se calcule sin carreras. */
    private void bloquearExistencias(PedidoAValidar pedido) {
        List<Long> productoIds = pedidoRepository.detallesDe(pedido.getId()).stream()
                .map(d -> d.getProductoId().longValue())
                .distinct()
                .toList();
        if (!productoIds.isEmpty()) {
            existenciaBloqueo.bloquear(pedido.getSucursalId(), productoIds);
        }
    }

    private void registrarDecision(PedidoAValidar pedido, String estado, String texto, String actor) {
        String marca = "[VALIDACION COMERCIAL " + LocalDateTime.now().format(FORMATO_FECHA)
                + " | " + (actor != null ? actor : "sistema") + " | " + estado + "]"
                + (texto != null && !texto.isBlank() ? " " + texto.trim() : "");
        String previas = pedido.getObservaciones();
        pedido.setObservaciones(previas == null || previas.isBlank() ? marca : previas + "\n" + marca);
        pedido.setEstado(estado);
        pedidoRepository.save(pedido);
    }

    /** Lo que se leyó para evaluar un pedido, para no volver a consultarlo al armar la respuesta. */
    private record Evaluacion(
            List<PedidoAValidarDetalle> detalles,
            DatosCliente cliente,
            DeudaCliente deuda,
            List<LineaPedidoResponse> lineas,
            List<Regla> reglas) {
    }

    private Evaluacion evaluacion(PedidoAValidar pedido) {
        List<PedidoAValidarDetalle> detalles = pedidoRepository.detallesDe(pedido.getId());

        Set<Long> productoIds = detalles.stream()
                .map(d -> d.getProductoId().longValue())
                .collect(Collectors.toSet());
        Map<Long, Producto> productos = new HashMap<>();
        productoRepository.findAllById(productoIds).forEach(p -> productos.put(p.getId(), p));

        List<Linea> lineasRegla = new ArrayList<>();
        List<LineaPedidoResponse> lineasRespuesta = new ArrayList<>();
        for (PedidoAValidarDetalle d : detalles) {
            Producto producto = productos.get(d.getProductoId().longValue());
            BigDecimal fisico = stockRepository
                    .findByProductoIdAndAlmacenId(d.getProductoId().longValue(), pedido.getSucursalId())
                    .map(StockAlmacen::getCantidadActual)
                    .orElse(BigDecimal.ZERO);
            BigDecimal reservado = pedidoRepository.cantidadReservada(
                    d.getProductoId(), pedido.getSucursalId(), pedido.getId());
            String nombre = producto != null ? producto.getNombre() : "Producto #" + d.getProductoId();

            Linea linea = new Linea(d.getProductoId(), nombre, d.getCantidad(), fisico, reservado);
            lineasRegla.add(linea);
            lineasRespuesta.add(mapper.toLinea(
                    d.getProductoId(), nombre, producto != null ? producto.getCodigoSku() : null,
                    d.getCantidad(), d.getPrecioUnitario(), d.getDescuento(), d.getSubtotal(),
                    fisico, reservado, linea.disponible(), linea.cubierta()));
        }

        DatosCliente datos = creditoRepository.buscarCliente(pedido.getClienteId()).orElse(null);
        DeudaCliente deuda = creditoRepository.deudaDe(pedido.getClienteId(), LocalDate.now());

        List<Regla> reglas = ReglasValidacionComercial.evaluar(
                datos == null ? null : new Cliente(datos.razonSocial(), datos.condicionPago(),
                        datos.limiteCredito(), datos.activo()),
                new Deuda(deuda.saldoPendiente(), deuda.saldoVencido(), deuda.cuotasVencidas()),
                pedido.getSubtotal(), pedido.getDescuento(), pedido.getTotal(), lineasRegla);
        return new Evaluacion(detalles, datos, deuda, lineasRespuesta, reglas);
    }

    private ValidacionPedidoResponse construir(PedidoAValidar pedido, Evaluacion e) {
        boolean bloqueado = e.reglas().stream().anyMatch(Regla::impideAprobar);
        boolean advertencias = e.reglas().stream().anyMatch(r -> !r.cumple() && !r.bloqueante());
        BigDecimal limite = e.cliente() != null && e.cliente().limiteCredito() != null
                ? e.cliente().limiteCredito() : BigDecimal.ZERO;

        return ValidacionPedidoResponse.builder()
                .pedido(mapper.toResumen(pedido, e.cliente() != null ? e.cliente().razonSocial() : null))
                .observaciones(pedido.getObservaciones())
                .condicionPago(e.cliente() != null ? e.cliente().condicionPago() : null)
                .limiteCredito(limite)
                .saldoPendiente(e.deuda().saldoPendiente())
                .saldoVencido(e.deuda().saldoVencido())
                .creditoDisponible(limite.subtract(e.deuda().saldoPendiente()).max(BigDecimal.ZERO))
                .lineas(e.lineas())
                .reglas(e.reglas().stream().map(mapper::toRegla).toList())
                .puedeAprobar(PedidoAValidar.PENDIENTE.equals(pedido.getEstado()) && !bloqueado)
                .requiereJustificacion(advertencias)
                .build();
    }

    private ResponseStatusException pedidoNoEncontrado(Integer pedidoId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "PedidoAValidar con ID " + pedidoId + " no encontrado.");
    }
}
