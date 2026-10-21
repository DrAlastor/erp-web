package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.time.LocalDateTime;

/** Fila del listado de notas de despacho. */
public record DespachoResumenResponse(
        Integer id,
        String numeroDespacho,
        LocalDateTime fecha,
        Integer pedidoId,
        String numeroPedido,
        String clienteRazonSocial,
        String sucursalNombre,
        String estado) {
}
