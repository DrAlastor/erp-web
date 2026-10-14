package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion;

import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Cliente;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Deuda;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Linea;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ReglasValidacionComercial.Regla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasValidacionComercialTest {

    private static final Deuda SIN_DEUDA = new Deuda(BigDecimal.ZERO, BigDecimal.ZERO, 0);

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static Cliente credito(String limite) {
        return new Cliente("INNOVATECH S.R.L.", "CREDITO", bd(limite), true);
    }

    private static Cliente contado() {
        return new Cliente("JUAN TORREZ", "CONTADO", BigDecimal.ZERO, true);
    }

    private static Linea linea(String cantidad, String fisico, String reservado) {
        return new Linea(1, "Laptop ROG", bd(cantidad), bd(fisico), bd(reservado));
    }

    private static Regla regla(List<Regla> reglas, String codigo) {
        return reglas.stream().filter(r -> r.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static List<Regla> evaluar(Cliente cliente, Deuda deuda, String subtotal, String descuento,
                                       String total, List<Linea> lineas) {
        return ReglasValidacionComercial.evaluar(cliente, deuda, bd(subtotal), bd(descuento), bd(total), lineas);
    }

    @Test
    @DisplayName("Un pedido sano de un cliente al día cumple todas las reglas")
    void pedidoSano() {
        List<Regla> reglas = evaluar(credito("50000"), new Deuda(bd("10000"), BigDecimal.ZERO, 0),
                "20000", "500", "19500", List.of(linea("2", "5", "1")));

        assertTrue(reglas.stream().allMatch(Regla::cumple));
        assertTrue(reglas.stream().noneMatch(Regla::impideAprobar));
    }

    @Test
    @DisplayName("Un saldo vencido bloquea el pedido aunque el crédito alcance")
    void saldoVencidoBloquea() {
        List<Regla> reglas = evaluar(credito("50000"), new Deuda(bd("5000"), bd("2000"), 1),
                "1000", "0", "1000", List.of(linea("1", "5", "0")));

        Regla mora = regla(reglas, ReglasValidacionComercial.SALDO_VENCIDO);
        assertFalse(mora.cumple());
        assertTrue(mora.impideAprobar());
        assertTrue(regla(reglas, ReglasValidacionComercial.LIMITE_CREDITO).cumple());
    }

    @Test
    @DisplayName("La deuda pendiente más el pedido no puede pasar el límite de crédito")
    void limiteDeCredito() {
        Deuda deuda = new Deuda(bd("45000"), BigDecimal.ZERO, 0);

        Regla dentro = regla(evaluar(credito("50000"), deuda, "5000", "0", "5000",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.LIMITE_CREDITO);
        Regla fuera = regla(evaluar(credito("50000"), deuda, "5000.01", "0", "5000.01",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.LIMITE_CREDITO);

        assertTrue(dentro.cumple());
        assertFalse(fuera.cumple());
        assertTrue(fuera.impideAprobar());
    }

    @Test
    @DisplayName("Un cliente de crédito sin límite asignado no puede comprar a crédito")
    void creditoSinLimite() {
        Regla credito = regla(evaluar(credito("0"), SIN_DEUDA, "100", "0", "100",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.LIMITE_CREDITO);

        assertFalse(credito.cumple());
    }

    @Test
    @DisplayName("Un cliente de contado no consume línea de crédito")
    void clienteDeContado() {
        Regla credito = regla(evaluar(contado(), SIN_DEUDA, "9000", "0", "9000",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.LIMITE_CREDITO);

        assertTrue(credito.cumple());
    }

    @Test
    @DisplayName("La existencia reservada por otros pedidos aprobados no se vuelve a prometer")
    void stockDescuentaReservas() {
        // 5 en almacén, 4 ya reservados: solo queda 1 libre y el pedido pide 2.
        Regla stock = regla(evaluar(contado(), SIN_DEUDA, "100", "0", "100",
                List.of(linea("2", "5", "4"))), ReglasValidacionComercial.STOCK_DISPONIBLE);

        assertFalse(stock.cumple());
        assertTrue(stock.impideAprobar());
        assertTrue(stock.detalle().contains("Laptop ROG"));
    }

    @Test
    @DisplayName("Un descuento sobre el tope exige justificación pero no bloquea")
    void descuentoSobreElTope() {
        List<Regla> reglas = evaluar(contado(), SIN_DEUDA, "1000", "150", "850",
                List.of(linea("1", "5", "0")));

        Regla descuento = regla(reglas, ReglasValidacionComercial.DESCUENTO_AUTORIZADO);
        assertFalse(descuento.cumple());
        assertFalse(descuento.bloqueante());
        assertFalse(descuento.impideAprobar());
    }

    @Test
    @DisplayName("Un descuento justo en el tope se acepta")
    void descuentoEnElTope() {
        Regla descuento = regla(evaluar(contado(), SIN_DEUDA, "1000", "100", "900",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.DESCUENTO_AUTORIZADO);

        assertTrue(descuento.cumple());
    }

    @Test
    @DisplayName("Un cliente inactivo bloquea el pedido")
    void clienteInactivo() {
        Cliente inactivo = new Cliente("EMPRESA CERRADA", "CONTADO", BigDecimal.ZERO, false);

        Regla activo = regla(evaluar(inactivo, SIN_DEUDA, "100", "0", "100",
                List.of(linea("1", "5", "0"))), ReglasValidacionComercial.CLIENTE_ACTIVO);

        assertTrue(activo.impideAprobar());
    }

    @Test
    @DisplayName("Un pedido sin líneas no se puede aprobar")
    void pedidoSinLineas() {
        Regla detalle = regla(evaluar(contado(), SIN_DEUDA, "0", "0", "0", List.of()),
                ReglasValidacionComercial.PEDIDO_CON_DETALLE);

        assertTrue(detalle.impideAprobar());
    }

    @Test
    @DisplayName("Si el cliente no existe el pedido queda bloqueado")
    void clienteInexistente() {
        List<Regla> reglas = evaluar(null, SIN_DEUDA, "100", "0", "100", List.of(linea("1", "5", "0")));

        assertEquals(2, reglas.size());
        assertTrue(regla(reglas, ReglasValidacionComercial.CLIENTE_ACTIVO).impideAprobar());
    }
}
