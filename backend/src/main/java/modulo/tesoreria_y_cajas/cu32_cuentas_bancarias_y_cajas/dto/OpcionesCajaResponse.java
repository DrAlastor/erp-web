package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import java.util.List;

/** Sucursales y formas de pago activas para los formularios de caja. */
public record OpcionesCajaResponse(List<SucursalOpcion> sucursales, List<FormaPagoOpcion> formasPago) {

    public record SucursalOpcion(Integer id, String nombre) {}

    public record FormaPagoOpcion(Long id, String codigo, String nombre) {}
}
