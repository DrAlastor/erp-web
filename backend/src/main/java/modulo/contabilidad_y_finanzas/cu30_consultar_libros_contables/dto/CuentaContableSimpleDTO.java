package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

public record CuentaContableSimpleDTO(
        Long id,
        String codigo,
        String nombre,
        Integer nivel,
        String tipo,
        String naturaleza,
        Boolean permiteMovimiento
) {}
