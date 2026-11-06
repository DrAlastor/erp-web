package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.entity.PedidoDetalle;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.repository.NotaDespachoRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service.FacturacionPedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacturacionPedidoServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final NotaDespachoRepository despachos = mock(NotaDespachoRepository.class);
    private final FacturacionPedidoService servicio = new FacturacionPedidoService(pedidos, despachos);
    private Pedido pedido;

    @BeforeEach
    void pedidoDe3Unidades() {
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", 7L);
        Almacen sucursal = new Almacen();
        sucursal.setId(1);
        Producto producto = new Producto();
        producto.setId(5L);

        PedidoDetalle linea = new PedidoDetalle();
        linea.setProducto(producto);
        linea.setCantidad(new BigDecimal("3.00"));
        linea.setPrecioUnitario(new BigDecimal("100.00"));
        linea.setDescuento(new BigDecimal("30.00"));
        linea.setSubtotal(new BigDecimal("270.00"));

        pedido = new Pedido();
        pedido.setId(9);
        pedido.setNumeroPedido("PED-9");
        pedido.setCliente(cliente);
        pedido.setSucursal(sucursal);
        pedido.setEstado(EstadoPedido.DESPACHO_PARCIAL);
        pedido.setSubtotal(new BigDecimal("270.00"));
        pedido.setDescuento(new BigDecimal("27.00"));
        pedido.setDetalles(new ArrayList<>(List.of(linea)));
        when(pedidos.findById(9)).thenReturn(Optional.of(pedido));
    }

    private void despachadas(String cantidad) {
        when(despachos.sumarPorProducto(eq(9), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {5L, new BigDecimal(cantidad)}));
    }

    private VentaRequest venta(String cantidad, Long clienteId) {
        return new VentaRequest(1, clienteId, null, "CONTADO", "FACTURA", null, null, 9,
                List.of(new VentaRequest.Linea(5L, new BigDecimal(cantidad), null, null)), List.of());
    }

    @Test
    void facturaLoDespachadoPendiente() {
        despachadas("2.00");
        assertSame(pedido, servicio.validar(venta("2", 7L)));
    }

    @Test
    void noFacturaMasDeLoDespachado() {
        despachadas("2.00");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.validar(venta("3", 7L)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void descuentaLoYaFacturado() {
        despachadas("2.00");
        when(despachos.facturadoPorProducto(9L)).thenReturn(List.<Object[]>of(new Object[] {5L, new BigDecimal("2.00")}));
        assertThrows(ResponseStatusException.class, () -> servicio.validar(venta("1", 7L)));
    }

    @Test
    void rechazaOtroCliente() {
        despachadas("2.00");
        assertThrows(ResponseStatusException.class, () -> servicio.validar(venta("1", 8L)));
    }

    @Test
    void pedidoSinDespachoNoSeFactura() {
        pedido.setEstado(EstadoPedido.APROBADO);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.validar(venta("1", 7L)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void prorrateaDescuentoDeLineaYGlobal() {
        // Línea 30 + global 27 = 57 por 3 unidades: a 2 unidades le tocan 38.
        assertEquals(new BigDecimal("38.00"), servicio.descuento(pedido, 5L, new BigDecimal("2")));
    }

    @Test
    void cierraElPedidoCuandoTodoEstaFacturado() {
        pedido.setEstado(EstadoPedido.DESPACHADO);
        when(pedidos.bloquear(9)).thenReturn(Optional.of(pedido));
        when(despachos.facturadoPorProducto(9L)).thenReturn(List.<Object[]>of(new Object[] {5L, new BigDecimal("3.00")}));
        servicio.actualizarEstado(9L);
        assertEquals(EstadoPedido.COMPLETADO, pedido.getEstado());
    }
}
