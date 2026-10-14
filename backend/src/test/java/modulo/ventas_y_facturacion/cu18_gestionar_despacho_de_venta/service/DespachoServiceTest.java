package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto.MovimientoInventarioRequest;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.service.MovimientoInventarioService;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.ventas_y_facturacion.compartido.entity.EstadoPedido;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;
import modulo.ventas_y_facturacion.compartido.entity.PedidoDetalle;
import modulo.ventas_y_facturacion.compartido.repository.PedidoRepository;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.ItemDespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.DespachoDetalle;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.NotaDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento.DespachoAnulado;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.evento.DespachoConfirmado;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.exception.StockInsuficienteException;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.mapper.DespachoMapper;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.repository.NotaDespachoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pruebas del despacho. El repositorio de notas se simula con una lista en memoria, así
 * las sumas de "comprometido" y "despachado" responden al estado real de las notas.
 */
class DespachoServiceTest {

    private final NotaDespachoRepository notas = mock(NotaDespachoRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final AlmacenRepository sucursales = mock(AlmacenRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final MovimientoInventarioService inventario = mock(MovimientoInventarioService.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    private DespachoService service;
    private final List<NotaDespacho> guardadas = new ArrayList<>();
    private final Map<Long, BigDecimal> stock = new HashMap<>();
    private Pedido pedido;
    private Almacen central;
    private Producto arroz;
    private Producto aceite;

    @BeforeEach
    void preparar() {
        service = new DespachoService(notas, pedidos, sucursales, usuarios, inventario, eventos,
                new DespachoMapper());

        central = new Almacen();
        central.setId(1);
        central.setNombre("Central");
        central.setActivo(true);
        when(sucursales.findById(1)).thenReturn(Optional.of(central));

        arroz = producto(10L, "Arroz 1kg");
        aceite = producto(11L, "Aceite 1L");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Distribuidora Norte");
        pedido = new Pedido();
        pedido.setId(12);
        pedido.setNumeroPedido("PED-000012");
        pedido.setCliente(cliente);
        pedido.setSucursal(central);
        pedido.setEstado(EstadoPedido.APROBADO);
        pedido.agregarDetalle(renglon(arroz, "10"));
        pedido.agregarDetalle(renglon(aceite, "5"));
        when(pedidos.bloquear(12)).thenReturn(Optional.of(pedido));

        stock.put(10L, new BigDecimal("100"));
        stock.put(11L, new BigDecimal("100"));
        when(inventario.obtenerStockDisponible(any(), eq(1)))
                .thenAnswer(inv -> stock.getOrDefault((Long) inv.getArgument(0), BigDecimal.ZERO));

        when(notas.saveAndFlush(any(NotaDespacho.class))).thenAnswer(inv -> {
            NotaDespacho n = inv.getArgument(0);
            n.setId(guardadas.size() + 45);
            guardadas.add(n);
            return n;
        });
        when(notas.bloquear(any())).thenAnswer(inv -> buscar(inv.getArgument(0)));
        when(notas.buscarCompleta(any())).thenAnswer(inv -> buscar(inv.getArgument(0)));
        when(notas.sumarPorProducto(eq(12), anyCollection(), any())).thenAnswer(inv -> {
            Collection<EstadoDespacho> estados = inv.getArgument(1);
            Integer excluir = inv.getArgument(2);
            Map<Long, BigDecimal> suma = new HashMap<>();
            for (NotaDespacho n : guardadas) {
                if (!estados.contains(n.getEstado()) || n.getId().equals(excluir)) continue;
                for (DespachoDetalle d : n.getDetalles()) {
                    suma.merge(d.getProducto().getId(), d.getCantidad(), BigDecimal::add);
                }
            }
            List<Object[]> filas = new ArrayList<>();
            suma.forEach((k, v) -> filas.add(new Object[] {k, v}));
            return filas;
        });
    }

    @Test
    void registrarDejaLaNotaEnPreparacionSinMoverStock() {
        DespachoResponse r = service.registrar(peticion(item(10L, "4")), 3L);

        assertThat(r.numeroDespacho()).isEqualTo("DES-000045");
        assertThat(r.estado()).isEqualTo("PREPARACION");
        assertThat(r.confirmable()).isTrue();
        verify(inventario, never()).registrarMovimiento(any(), any());
    }

    @Test
    void noDespachaUnPedidoQueNoFueAprobado() {
        pedido.setEstado(EstadoPedido.PENDIENTE);
        assertThatThrownBy(() -> service.registrar(peticion(item(10L, "1")), 3L))
                .hasMessageContaining("aprobados");
    }

    @Test
    void noDespachaMasDeLoPendienteContandoOtrasNotas() {
        service.registrar(peticion(item(10L, "7")), 3L);           // quedan 3 de arroz
        assertThatThrownBy(() -> service.registrar(peticion(item(10L, "4")), 3L))
                .hasMessageContaining("quedan 3");
    }

    @Test
    void noDespachaUnProductoQueNoEstaEnElPedido() {
        assertThatThrownBy(() -> service.registrar(peticion(item(99L, "1")), 3L))
                .hasMessageContaining("no forma parte del pedido");
    }

    @Test
    void informaTodosLosFaltantesJuntos() {
        stock.put(10L, new BigDecimal("2"));
        stock.put(11L, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.registrar(peticion(item(10L, "4"), item(11L, "1")), 3L))
                .isInstanceOf(StockInsuficienteException.class)
                .satisfies(e -> assertThat(((StockInsuficienteException) e).getFaltantes())
                        .extracting(f -> f.productoId()).containsExactly(10L, 11L));
    }

    @Test
    void confirmarParcialDescuentaStockYDejaElPedidoEnDespachoParcial() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();

        DespachoResponse r = service.confirmar(id);

        ArgumentCaptor<MovimientoInventarioRequest> mov = ArgumentCaptor.forClass(MovimientoInventarioRequest.class);
        verify(inventario).registrarMovimiento(mov.capture(), any());
        assertThat(mov.getValue().getTipoMovimiento()).isEqualTo("SALIDA");
        assertThat(mov.getValue().getCantidad()).isEqualByComparingTo("4");
        assertThat(mov.getValue().getReferenciaDoc()).isEqualTo("DES-000045");
        assertThat(r.estado()).isEqualTo("DESPACHADO");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.DESPACHO_PARCIAL);

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue()).isInstanceOf(DespachoConfirmado.class);
        // 4 x costo 8.00
        assertThat(((DespachoConfirmado) evento.getValue()).costoTotal()).isEqualByComparingTo("32.00");
    }

    @Test
    void confirmarTodoDejaElPedidoDespachado() {
        int id = service.registrar(peticion(item(10L, "10"), item(11L, "5")), 3L).id();
        service.confirmar(id);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.DESPACHADO);
    }

    @Test
    void siElStockSeAcaboEntreRegistrarYConfirmarNoSaleNada() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();
        stock.put(10L, new BigDecimal("1"));

        assertThatThrownBy(() -> service.confirmar(id)).isInstanceOf(StockInsuficienteException.class);
        verify(inventario, never()).registrarMovimiento(any(), any());
        assertThat(guardadas.get(0).getEstado()).isEqualTo(EstadoDespacho.PREPARACION);
    }

    @Test
    void noSeConfirmaDosVeces() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();
        service.confirmar(id);
        assertThatThrownBy(() -> service.confirmar(id)).hasMessageContaining("PREPARACION");
    }

    @Test
    void anularUnDespachoConfirmadoDevuelveElStockYElPedidoVuelveAAprobado() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();
        service.confirmar(id);

        DespachoResponse r = service.anular(id, "se cayó la carga");

        ArgumentCaptor<MovimientoInventarioRequest> mov = ArgumentCaptor.forClass(MovimientoInventarioRequest.class);
        verify(inventario, times(2)).registrarMovimiento(mov.capture(), any());
        assertThat(mov.getAllValues().get(1).getTipoMovimiento()).isEqualTo("ENTRADA");
        assertThat(r.estado()).isEqualTo("ANULADO");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.APROBADO);
        verify(eventos).publishEvent(any(DespachoAnulado.class));
    }

    @Test
    void anularEnPreparacionNoMueveStock() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();
        service.anular(id, "error de carga");
        verify(inventario, never()).registrarMovimiento(any(), any());
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.APROBADO);
    }

    @Test
    void unDespachoEntregadoNoSeAnula() {
        int id = service.registrar(peticion(item(10L, "4")), 3L).id();
        service.confirmar(id);
        service.marcarEntregado(id);
        assertThatThrownBy(() -> service.anular(id, "tarde")).hasMessageContaining("no se puede anular");
    }

    // --------------------------------------------------------------- apoyo --

    private Optional<NotaDespacho> buscar(Integer id) {
        return guardadas.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    private DespachoRequest peticion(ItemDespachoRequest... items) {
        return new DespachoRequest(12, 1, "Juan Pérez", "1234-ABC", null, List.of(items));
    }

    private ItemDespachoRequest item(Long productoId, String cantidad) {
        return new ItemDespachoRequest(productoId, new BigDecimal(cantidad));
    }

    private Producto producto(Long id, String nombre) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigoSku("SKU-" + id);
        p.setNombre(nombre);
        p.setCostoPromedio(new BigDecimal("8.00"));
        p.setActivo(true);
        return p;
    }

    private PedidoDetalle renglon(Producto producto, String cantidad) {
        PedidoDetalle d = new PedidoDetalle();
        d.setProducto(producto);
        d.setCantidad(new BigDecimal(cantidad));
        d.setPrecioUnitario(BigDecimal.TEN);
        d.setSubtotal(BigDecimal.TEN);
        return d;
    }
}
