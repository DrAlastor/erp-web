package modulo.ventas_y_facturacion.cu16_pedidos_de_venta;

import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service.CalculadoraPedido;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service.CalculadoraPedido.LineaEntrada;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPedidoTest {

    private static LineaEntrada linea(long id, String cantidad, String precio, String descuento) {
        return new LineaEntrada(
                id, "Producto " + id, new BigDecimal(cantidad), new BigDecimal(precio),
                new BigDecimal(descuento));
    }

    @Test
    void calculaSubtotalesYTotalConRedondeoHalfUp() {
        CalculadoraPedido.Resultado r = CalculadoraPedido.calcular(
                List.of(linea(1, "3", "10.005", "0"), linea(2, "2", "5", "1.50")),
                new BigDecimal("2"));

        assertThat(r.lineas().get(0).precioUnitario()).isEqualByComparingTo("10.01");
        assertThat(r.lineas().get(0).subtotal()).isEqualByComparingTo("30.03");
        assertThat(r.lineas().get(1).subtotal()).isEqualByComparingTo("8.50");
        assertThat(r.subtotal()).isEqualByComparingTo("38.53");
        assertThat(r.descuento()).isEqualByComparingTo("2.00");
        assertThat(r.total()).isEqualByComparingTo("36.53");
        assertThat(r.total().scale()).isEqualTo(2);
    }

    @Test
    void descuentoGlobalNuloEsCero() {
        CalculadoraPedido.Resultado r =
                CalculadoraPedido.calcular(List.of(linea(1, "1", "100", "0")), null);
        assertThat(r.total()).isEqualByComparingTo("100");
    }

    @Test
    void rechazaLineaConDescuentoMayorAlImporte() {
        assertThatThrownBy(() -> CalculadoraPedido.calcular(
                        List.of(linea(1, "1", "10", "10.01")), BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void rechazaDescuentoGlobalMayorAlSubtotal() {
        assertThatThrownBy(() -> CalculadoraPedido.calcular(
                        List.of(linea(1, "1", "10", "0")), new BigDecimal("10.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("descuento global");
    }

    @Test
    void permiteDescuentoGlobalIgualAlSubtotal() {
        CalculadoraPedido.Resultado r = CalculadoraPedido.calcular(
                List.of(linea(1, "1", "10", "0")), new BigDecimal("10"));
        assertThat(r.total()).isEqualByComparingTo("0");
    }
}
