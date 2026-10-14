package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del listado de pedidos. */
public record PedidoResumenResponse(
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
        Integer cantidadItems) {}
