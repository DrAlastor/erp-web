package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.mapper;

import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResumenResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.PagoProveedorResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.CuentaPorPagarConsultaRepository.CuentaFila;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.EstadoCuentaPorPagar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Traduce las filas de cuotas a los DTOs de la API. {@code vencida} y {@code diasVencido}
 * se calculan respecto de {@code hoy} con {@link EstadoCuentaPorPagar}.
 */
@Component
public class CuentaPorPagarMapper {

    public CuentaPorPagarResumenResponse toResumen(CuentaFila c, LocalDate hoy) {
        return new CuentaPorPagarResumenResponse(
                c.id(),
                c.compraId(),
                c.numeroFactura(),
                c.proveedorId(),
                c.proveedorRazonSocial(),
                c.proveedorNitCi(),
                c.numeroCuota(),
                c.montoTotal(),
                c.montoPagado(),
                c.saldo(),
                c.fechaVencimiento(),
                c.estado(),
                EstadoCuentaPorPagar.vencida(c.saldo(), c.fechaVencimiento(), hoy),
                EstadoCuentaPorPagar.diasVencido(c.saldo(), c.fechaVencimiento(), hoy));
    }

    public CuentaPorPagarResponse toResponse(
            CuentaFila c, List<PagoProveedorResponse> pagos, LocalDate hoy) {
        return new CuentaPorPagarResponse(
                c.id(),
                c.compraId(),
                c.numeroFactura(),
                c.proveedorId(),
                c.proveedorRazonSocial(),
                c.proveedorNitCi(),
                c.numeroCuota(),
                c.montoTotal(),
                c.montoPagado(),
                c.saldo(),
                c.fechaVencimiento(),
                c.estado(),
                EstadoCuentaPorPagar.vencida(c.saldo(), c.fechaVencimiento(), hoy),
                EstadoCuentaPorPagar.diasVencido(c.saldo(), c.fechaVencimiento(), hoy),
                c.fechaCompra(),
                c.totalCompra(),
                c.sucursalNombre(),
                pagos);
    }
}
