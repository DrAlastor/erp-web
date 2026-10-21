package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nota de despacho completa.
 *
 * <p>Los indicadores {@code confirmable}, {@code entregable} y {@code anulable} le dicen al
 * frontend qué acciones mostrar; el backend las vuelve a validar al recibirlas.
 */
public record DespachoResponse(
        Integer id,
        String numeroDespacho,
        LocalDateTime fecha,
        String estado,
        Integer pedidoId,
        String numeroPedido,
        String estadoPedido,
        Long clienteId,
        String clienteRazonSocial,
        Integer sucursalId,
        String sucursalNombre,
        Long despachadoPorId,
        String despachadoPor,
        String conductor,
        String placa,
        String observaciones,
        boolean confirmable,
        boolean entregable,
        boolean anulable,
        List<DetalleDespachoResponse> items) {
}
