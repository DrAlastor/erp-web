package modulo.ventas_y_facturacion.compartido.controller;

import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * PROVISIONAL hasta que exista la CU17 (Validación Comercial y Aprobación).
 *
 * <p>Sin la CU17 ningún pedido sale de PENDIENTE y el despacho (CU18) no tendría nada que
 * despachar. Esto permite aprobar a mano para probar el flujo completo
 * cotización → pedido → despacho. Cuando la CU17 esté lista, este controlador se borra:
 * nada más depende de él.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoProvisionalController {

    private final PedidoRepository pedidos;

    public PedidoProvisionalController(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public record PedidoPendienteResponse(Integer id, String numeroPedido, LocalDate fecha,
                                          String clienteRazonSocial, String sucursalNombre,
                                          BigDecimal total, String estado, String observaciones) {
    }

    @GetMapping("/pendientes-de-aprobacion")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    @Transactional(readOnly = true)
    public List<PedidoPendienteResponse> pendientes() {
        return pedidos.buscarPorEstados(Set.of(EstadoPedido.PENDIENTE), null).stream()
                .map(p -> new PedidoPendienteResponse(p.getId(), p.getNumeroPedido(), p.getFecha(),
                        p.getCliente().getRazonSocial(), p.getSucursal().getNombre(),
                        p.getTotal(), p.getEstado().name(), p.getObservaciones()))
                .toList();
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    @Transactional
    public PedidoPendienteResponse aprobar(@PathVariable Integer id) {
        Pedido pedido = pedidos.bloquear(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El pedido " + id + " no existe."));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido " + pedido.getNumeroPedido() + " no está PENDIENTE.");
        }
        pedido.setEstado(EstadoPedido.APROBADO);
        return new PedidoPendienteResponse(pedido.getId(), pedido.getNumeroPedido(), pedido.getFecha(),
                pedido.getCliente().getRazonSocial(), pedido.getSucursal().getNombre(),
                pedido.getTotal(), pedido.getEstado().name(), pedido.getObservaciones());
    }
}
