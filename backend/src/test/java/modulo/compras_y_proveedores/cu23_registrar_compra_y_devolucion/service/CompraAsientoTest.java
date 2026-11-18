package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.service;

import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService.Partida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU28: los asientos de compras, anulaciones y devoluciones deben cuadrar y usar las cuentas correctas. */
class CompraAsientoTest {

    private static final BigDecimal SUBTOTAL = new BigDecimal("100.00");
    private static final BigDecimal IVA = new BigDecimal("13.00");
    private static final BigDecimal TOTAL = new BigDecimal("113.00");

    private static BigDecimal debe(List<Partida> ps) {
        return ps.stream().map(Partida::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal haber(List<Partida> ps) {
        return ps.stream().map(Partida::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Partida de(List<Partida> ps, String cuenta) {
        return ps.stream().filter(p -> p.codigoCuenta().equals(cuenta)).findFirst().orElseThrow();
    }

    @Test
    void compraAlContadoDebitaInventarioEIvaYAcreditaCaja() {
        List<Partida> ps = CompraService.partidasCompra("CONTADO", "Compra FAC-1", SUBTOTAL, IVA, TOTAL, false);

        assertEquals(0, debe(ps).compareTo(haber(ps)), "el asiento debe cuadrar");
        assertEquals(0, de(ps, ContabilidadVentaService.INVENTARIO).debe().compareTo(SUBTOTAL));
        assertEquals(0, de(ps, ContabilidadVentaService.CREDITO_FISCAL_IVA).debe().compareTo(IVA));
        assertEquals(0, de(ps, ContabilidadVentaService.CAJA).haber().compareTo(TOTAL));
    }

    @Test
    void compraACreditoAcreditaCuentasPorPagar() {
        List<Partida> ps = CompraService.partidasCompra("CREDITO", "Compra FAC-2", SUBTOTAL, IVA, TOTAL, false);

        assertEquals(0, debe(ps).compareTo(haber(ps)));
        assertEquals(0, de(ps, ContabilidadVentaService.CUENTAS_POR_PAGAR).haber().compareTo(TOTAL));
        assertTrue(ps.stream().noneMatch(p -> p.codigoCuenta().equals(ContabilidadVentaService.CAJA)));
    }

    @Test
    void anulacionODevolucionInvierteLosLados() {
        List<Partida> ps = CompraService.partidasCompra("CREDITO", "Anulación", SUBTOTAL, IVA, TOTAL, true);

        assertEquals(0, debe(ps).compareTo(haber(ps)));
        assertEquals(0, de(ps, ContabilidadVentaService.INVENTARIO).haber().compareTo(SUBTOTAL));
        assertEquals(0, de(ps, ContabilidadVentaService.CREDITO_FISCAL_IVA).haber().compareTo(IVA));
        assertEquals(0, de(ps, ContabilidadVentaService.CUENTAS_POR_PAGAR).debe().compareTo(TOTAL));
    }
}
