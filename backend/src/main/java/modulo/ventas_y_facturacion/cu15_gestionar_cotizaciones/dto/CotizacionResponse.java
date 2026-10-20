package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cotización completa, con sus renglones.
 *
 * <p>{@code modificable} y {@code convertible} le dicen al frontend qué acciones mostrar
 * según el estado; el backend vuelve a validarlas igual al recibir la petición.
 */
public record CotizacionResponse(
        Integer id,
        String numeroCotizacion,
        LocalDate fecha,
        Integer validezDias,
        LocalDate fechaVencimiento,
        Long clienteId,
        String clienteRazonSocial,
        String clienteNitCi,
        Long vendedorId,
        String vendedor,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal descuentoGlobal,
        BigDecimal total,
        String estado,
        String observaciones,
        boolean modificable,
        boolean convertible,
        List<DetalleCotizacionResponse> items) {
}
