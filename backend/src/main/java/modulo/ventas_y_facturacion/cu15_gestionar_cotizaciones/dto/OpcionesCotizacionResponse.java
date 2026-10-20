package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que necesita el formulario de cotización para armarse: productos activos con su
 * precio de lista, sucursales para convertir en pedido y el tope de descuento.
 *
 * <p>El precio es sólo para mostrar el total mientras se arma: al guardar, el servidor lo
 * vuelve a tomar del catálogo.
 */
public record OpcionesCotizacionResponse(
        List<ProductoCotizable> productos,
        List<SucursalOpcion> sucursales,
        BigDecimal descuentoMaximoPorcentaje) {

    public record ProductoCotizable(Long id, String codigo, String nombre, BigDecimal precio) {
    }

    public record SucursalOpcion(Integer id, String nombre) {
    }
}
