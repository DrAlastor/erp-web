package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto;

public record CuentaResponse(
        Long id,
        String codigo,
        String nombre,
        Integer nivel,
        String tipo,
        String naturaleza,
        boolean permiteMovimiento,
        Long padreId,
        String codigoPadre,
        boolean activo,
        boolean tieneHijos,
        boolean tieneMovimientos,
        boolean cuentaIntegracion) {
}
