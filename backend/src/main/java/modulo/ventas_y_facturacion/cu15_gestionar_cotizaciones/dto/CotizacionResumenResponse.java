package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del listado de cotizaciones. */
public record CotizacionResumenResponse(
        Integer id,
        String numeroCotizacion,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Long clienteId,
        String clienteRazonSocial,
        BigDecimal total,
        String estado) {
}
