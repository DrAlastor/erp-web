package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import java.math.BigDecimal;

public record ItemPrecioProductoDTO(
        Long productoId,
        String codigoSku,
        String nombre,
        String categoriaNombre,
        BigDecimal costoCompra,
        BigDecimal precioCatalogo,
        BigDecimal precioAsignado,
        BigDecimal margenCalculadoPorc,
        String fechaActualizacion) {}
