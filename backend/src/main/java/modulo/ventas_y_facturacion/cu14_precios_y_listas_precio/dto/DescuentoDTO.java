package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DescuentoDTO(
        Integer id,
        String nombre,
        String tipo,
        BigDecimal valor,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Boolean activo) {}
