package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Datos de referencia que necesita el formulario del punto de venta. */
public record CatalogosVentaResponse(
        List<Sucursal> sucursales,
        List<FormaPago> formasPago,
        List<Descuento> descuentos,
        List<Turno> turnosAbiertos,
        Dosificacion dosificacion,
        boolean cobroEnLinea,
        /** Tasas vigentes de la tabla impuestos, en porcentaje (13.00 = 13 %). */
        BigDecimal tasaIva,
        BigDecimal tasaIt,
        List<CotizacionResumen> cotizacionesPendientes) {

    public record Sucursal(Integer id, String nombre) {
    }

    public record FormaPago(Long id, String codigo, String nombre) {
    }

    public record Descuento(Long id, String nombre, String tipo, BigDecimal valor) {
    }

    public record Turno(Long id, Integer sucursalId, Long cajeroId, LocalDateTime fechaApertura, String nombre) {
    }

    /** Dosificación vigente; {@code null} si no hay ninguna y no se puede facturar. */
    public record Dosificacion(String numeroAutorizacion, Integer siguienteNumero, Integer numeroFinal,
                               LocalDate fechaLimite) {
    }

    public record CotizacionResumen(
            Integer id,
            String numeroCotizacion,
            Long clienteId,
            String clienteRazonSocial,
            String clienteNitCi,
            BigDecimal total,
            LocalDate fecha,
            List<CotizacionLinea> lineas) {
    }

    public record CotizacionLinea(
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal subtotal) {
    }
}
