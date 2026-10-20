package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Pedido completo: los datos del listado mas observaciones, auditoria y lineas. */
public record PedidoResponse(
        Long id,
        String numeroPedido,
        LocalDate fecha,
        Long clienteId,
        String clienteRazonSocial,
        String clienteNitCi,
        Long sucursalId,
        String sucursalNombre,
        Long vendedorId,
        String vendedorNombre,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String estado,
        Integer cantidadItems,
        String observaciones,
        LocalDateTime fechaCreacion,
        List<PedidoDetalleResponse> detalles) {}
