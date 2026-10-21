package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository.ClienteRepository;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.ItemCotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.PedidoGeneradoResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.Cotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.CotizacionDetalle;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.EstadoCotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.mapper.CotizacionMapper;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.repository.CotizacionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

class CotizacionServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 13);

    private final CotizacionRepository cotizaciones = mock(CotizacionRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final AlmacenRepository sucursales = mock(AlmacenRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);

    private CotizacionService service;
    private Cliente cliente;
    private Producto arroz;
    private Producto aceite;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new CotizacionService(cotizaciones, clientes, productos, sucursales, pedidos, usuarios,
                new CotizacionMapper(), new BigDecimal("15"), reloj);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Distribuidora Norte");
        cliente.setNitCi("1234567");
        cliente.setActivo(true);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));

        arroz = producto(10L, "Arroz 1kg", "12.50");
        aceite = producto(11L, "Aceite 1L", "20.00");

        when(cotizaciones.saveAndFlush(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion c = inv.getArgument(0);
            if (c.getId() == null) c.setId(123);
            return c;
        });
        when(pedidos.saveAndFlush(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(12);
            return p;
        });
    }

    @Test
    void registrarTomaElPrecioDelCatalogoYCalculaLosTotales() {
        CotizacionResponse r = service.registrar(peticion(List.of(
                new ItemCotizacionRequest(10L, new BigDecimal("4"), new BigDecimal("2.00")),
                new ItemCotizacionRequest(11L, new BigDecimal("1"), null)), new BigDecimal("3.00")), 7L);

        // 4 x 12.50 + 1 x 20.00 = 70.00 ; descuentos 2.00 + 3.00 = 5.00
        assertThat(r.subtotal()).isEqualByComparingTo("70.00");
        assertThat(r.descuento()).isEqualByComparingTo("5.00");
        assertThat(r.descuentoGlobal()).isEqualByComparingTo("3.00");
        assertThat(r.total()).isEqualByComparingTo("65.00");
        assertThat(r.items().get(0).precioUnitario()).isEqualByComparingTo("12.50");
        assertThat(r.items().get(0).subtotal()).isEqualByComparingTo("48.00");
        assertThat(r.numeroCotizacion()).isEqualTo("COT-000123");
        assertThat(r.estado()).isEqualTo("EMITIDA");
        assertThat(r.vendedorId()).isEqualTo(7L);
        assertThat(r.fechaVencimiento()).isEqualTo(HOY.plusDays(15));
    }

    @Test
    void registrarComoBorradorNoLaEmite() {
        CotizacionRequest p = new CotizacionRequest(1L, 10, null, null, true,
                List.of(new ItemCotizacionRequest(10L, BigDecimal.ONE, null)));
        assertThat(service.registrar(p, 7L).estado()).isEqualTo("BORRADOR");
    }

    @Test
    void rechazaUnDescuentoQueSuperaLaPolitica() {
        // 15% de 50.00 = 7.50 ; se piden 8.00
        CotizacionRequest p = peticion(List.of(
                new ItemCotizacionRequest(10L, new BigDecimal("4"), null)), new BigDecimal("8.00"));

        assertThatThrownBy(() -> service.registrar(p, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                .hasMessageContaining("política de descuentos");
        verify(cotizaciones, never()).saveAndFlush(any());
    }

    @Test
    void aceptaUnDescuentoJustoEnElLimite() {
        CotizacionRequest p = peticion(List.of(
                new ItemCotizacionRequest(10L, new BigDecimal("4"), null)), new BigDecimal("7.50"));
        assertThat(service.registrar(p, 7L).total()).isEqualByComparingTo("42.50");
    }

    @Test
    void rechazaClienteInactivo() {
        cliente.setActivo(false);
        assertThatThrownBy(() -> service.registrar(peticion(List.of(
                new ItemCotizacionRequest(10L, BigDecimal.ONE, null)), null), 7L))
                .hasMessageContaining("inactivo");
    }

    @Test
    void rechazaProductoInactivoYProductoRepetido() {
        arroz.setActivo(false);
        assertThatThrownBy(() -> service.registrar(peticion(List.of(
                new ItemCotizacionRequest(10L, BigDecimal.ONE, null)), null), 7L))
                .hasMessageContaining("inactivo");

        arroz.setActivo(true);
        assertThatThrownBy(() -> service.registrar(peticion(List.of(
                new ItemCotizacionRequest(10L, BigDecimal.ONE, null),
                new ItemCotizacionRequest(10L, BigDecimal.ONE, null)), null), 7L))
                .hasMessageContaining("repetido");
    }

    @Test
    void noSeModificaUnaCotizacionConvertida() {
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.CONVERTIDA, HOY);
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.modificar(5, peticion(List.of(
                new ItemCotizacionRequest(10L, BigDecimal.ONE, null)), null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void convertirCreaElPedidoPendienteYMarcaLaCotizacion() {
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.EMITIDA, HOY.minusDays(3));
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));
        when(sucursales.findById(1)).thenReturn(Optional.of(sucursal()));

        PedidoGeneradoResponse r = service.convertirEnPedido(5, 1, 7L);

        assertThat(r.numeroPedido()).isEqualTo("PED-000012");
        assertThat(r.estado()).isEqualTo(EstadoPedido.PENDIENTE.name());
        assertThat(r.total()).isEqualByComparingTo(c.getTotal());
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CONVERTIDA);
        assertThat(c.getObservaciones()).contains("PED-000012");
    }

    @Test
    void noConvierteUnaCotizacionVencida() {
        // emitida hace 20 días con 15 de validez
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.EMITIDA, HOY.minusDays(20));
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.convertirEnPedido(5, 1, 7L))
                .hasMessageContaining("vigente");
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void noConvierteSiSuperaElLimiteDeCredito() {
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.EMITIDA, HOY);
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));
        when(cotizaciones.limiteCreditoDe(1L)).thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.convertirEnPedido(5, 1, 7L))
                .hasMessageContaining("límite de crédito");
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void noSeAnulaDosVeces() {
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.ANULADA, HOY);
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.anular(5, "error")).hasMessageContaining("no se puede anular");
    }

    @Test
    void anularGuardaElMotivo() {
        Cotizacion c = cotizacionGuardada(EstadoCotizacion.EMITIDA, HOY);
        when(cotizaciones.buscarCompleta(5)).thenReturn(Optional.of(c));
        assertThat(service.anular(5, "  el cliente desistió ").estado()).isEqualTo("ANULADA");
        assertThat(c.getObservaciones()).endsWith("el cliente desistió");
    }

    // --------------------------------------------------------------- apoyo --

    private CotizacionRequest peticion(List<ItemCotizacionRequest> items, BigDecimal descuentoGlobal) {
        return new CotizacionRequest(1L, null, descuentoGlobal, null, null, items);
    }

    private Producto producto(Long id, String nombre, String precio) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigoSku("SKU-" + id);
        p.setNombre(nombre);
        p.setPrecioVenta(new BigDecimal(precio));
        p.setCostoPromedio(new BigDecimal("8.00"));
        p.setActivo(true);
        when(productos.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    private Almacen sucursal() {
        Almacen a = new Almacen();
        a.setId(1);
        a.setNombre("Central");
        a.setActivo(true);
        return a;
    }

    private Cotizacion cotizacionGuardada(EstadoCotizacion estado, LocalDate fecha) {
        Cotizacion c = new Cotizacion();
        c.setId(5);
        c.setNumeroCotizacion("COT-000005");
        c.setCliente(cliente);
        c.setVendedorId(7L);
        c.setFecha(fecha);
        c.setValidezDias(15);
        CotizacionDetalle d = new CotizacionDetalle();
        d.setProducto(arroz);
        d.setCantidad(new BigDecimal("2"));
        d.setPrecioUnitario(new BigDecimal("12.50"));
        c.agregarDetalle(d);
        c.calcularTotales(BigDecimal.ZERO);
        c.setEstado(estado);
        return c;
    }
}
