package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.time.LocalDate;
import java.util.List;

/** Pedido aprobado que todavía tiene productos por despachar. */
public record PedidoPorDespacharResponse(
        Integer pedidoId,
        String numeroPedido,
        LocalDate fecha,
        String estado,
        Long clienteId,
        String clienteRazonSocial,
        Integer sucursalId,
        String sucursalNombre,
        List<PendienteDespachoResponse> items) {
}
