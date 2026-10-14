package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

public record ListaPrecioDTO(
        Integer id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean esPrincipal,
        Boolean activo,
        long articulosTarifadosCount) {}
