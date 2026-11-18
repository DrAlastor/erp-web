package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EstadoCajaResponse(
        TurnoActivo turno,
        List<Sucursal> sucursales,
        List<FormaPago> formasPago,
        ResumenTurno resumen,
        /** Hay una dosificación vigente: el POS puede emitir factura además de nota de venta. */
        boolean facturaDisponible) {

    public record Sucursal(Integer id, String nombre) {}
    public record FormaPago(Long id, String codigo, String nombre) {}
    public record TurnoActivo(Long id, Integer sucursalId, String sucursalNombre, Long cajeroId,
                              BigDecimal montoApertura, LocalDateTime fechaApertura, String estado,
                              String observaciones) {}
    public record ResumenTurno(BigDecimal montoApertura, BigDecimal ventasContado,
                               BigDecimal cobrosEfectivo, BigDecimal otrosCobros,
                               BigDecimal ingresos, BigDecimal egresos,
                               BigDecimal montoEsperado, LocalDateTime fechaCorte) {}
}
