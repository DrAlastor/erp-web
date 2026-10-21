package modulo.compras_y_proveedores.cu23_ordenes_de_compra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.compras_y_proveedores.compartido.service.AvisosCompras;
import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPago;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.mapper.ProveedorMapper;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.repository.ProveedorRepository;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.ItemOrdenCompraRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.SugerenciaReposicionResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.EstadoOrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.mapper.OrdenCompraMapper;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.repository.OrdenCompraRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;

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

class OrdenCompraServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);

    private final OrdenCompraRepository ordenes = mock(OrdenCompraRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final AlmacenRepository sucursales = mock(AlmacenRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AvisosCompras avisos = mock(AvisosCompras.class);

    private final Solicitante almacenero = new Solicitante(7L, "leonardo", "127.0.0.1");
    private final Solicitante admin = new Solicitante(1L, "admin", "127.0.0.1");

    private OrdenCompraService service;
    private Proveedor proveedor;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new OrdenCompraService(ordenes, proveedores, productos, sucursales, usuarios,
                new OrdenCompraMapper(), new ProveedorMapper(), avisos, reloj);

        proveedor = new Proveedor();
        proveedor.setId(3);
        proveedor.setRazonSocial("Importadora Sur");
        proveedor.setNitCi("998877");
        proveedor.setCondicionPago(CondicionPago.CREDITO);
        proveedor.setDiasCredito(30);
        proveedor.setActivo(true);
        when(proveedores.findById(3)).thenReturn(Optional.of(proveedor));

        when(productos.findById(10L)).thenReturn(Optional.of(producto(10L, "Arroz 1kg")));
        when(productos.findById(11L)).thenReturn(Optional.of(producto(11L, "Aceite 1L")));

        when(ordenes.ultimoCorrelativo("OC-2026-")).thenReturn(1);
        when(ordenes.saveAndFlush(any(OrdenCompra.class))).thenAnswer(inv -> {
            OrdenCompra o = inv.getArgument(0);
            if (o.getId() == null) o.setId(55);
            return o;
        });
        when(usuarios.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void registraConTotalesNumeroYQuedaPendiente() {
        OrdenCompraResponse r = service.registrar(peticion(new BigDecimal("10.00"), false,
                new ItemOrdenCompraRequest(10L, new BigDecimal("4"), new BigDecimal("12.50")),
                new ItemOrdenCompraRequest(11L, new BigDecimal("2"), new BigDecimal("20.00"))), almacenero);

        // 4 x 12.50 + 2 x 20.00 = 90.00 ; descuento 10.00
        assertThat(r.subtotal()).isEqualByComparingTo("90.00");
        assertThat(r.descuento()).isEqualByComparingTo("10.00");
        assertThat(r.total()).isEqualByComparingTo("80.00");
        assertThat(r.detalles().get(0).subtotal()).isEqualByComparingTo("50.00");
        assertThat(r.numeroOrden()).isEqualTo("OC-2026-0002");
        assertThat(r.estado()).isEqualTo("PENDIENTE");
        assertThat(r.creadoPorId()).isEqualTo(7L);
        assertThat(r.registradaPorMi()).isTrue();
        verify(ordenes).bloquearNumeracion();
        verify(avisos).registrar("ORDEN_CREADA", almacenero, "ordenes_compra", 55);
        verify(avisos).avisarAdministradores(eq("Orden de compra por aprobar"), anyString());
    }

    @Test
    void comoBorradorNoAvisaParaAprobar() {
        OrdenCompraResponse r = service.registrar(peticion(null, true,
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN)), almacenero);

        assertThat(r.estado()).isEqualTo("BORRADOR");
        verify(avisos, never()).avisarAdministradores(anyString(), anyString());
    }

    @Test
    void elDescuentoNoPuedeSuperarElSubtotal() {
        assertThatThrownBy(() -> service.registrar(peticion(new BigDecimal("50.01"), false,
                new ItemOrdenCompraRequest(10L, new BigDecimal("4"), new BigDecimal("12.50"))), almacenero))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                .hasMessageContaining("no puede superar el subtotal");
        verify(ordenes, never()).saveAndFlush(any());
    }

    @Test
    void rechazaProveedorInactivo() {
        proveedor.setActivo(false);
        assertThatThrownBy(() -> service.registrar(peticion(null, false,
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN)), almacenero))
                .hasMessageContaining("inactivo");
    }

    @Test
    void rechazaProductoRepetido() {
        assertThatThrownBy(() -> service.registrar(peticion(null, false,
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN),
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN)), almacenero))
                .hasMessageContaining("repetido");
    }

    @Test
    void quienLaRegistroNoPuedeAprobarla() {
        OrdenCompra orden = existente(EstadoOrdenCompra.PENDIENTE, 1L);

        assertThatThrownBy(() -> service.aprobar(orden.getId(), admin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("separación de funciones");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
    }

    @Test
    void otroAdministradorLaApruebaYSeAvisaAlCreador() {
        OrdenCompra orden = existente(EstadoOrdenCompra.PENDIENTE, 7L);

        OrdenCompraResponse r = service.aprobar(orden.getId(), admin);

        assertThat(r.estado()).isEqualTo("APROBADA");
        assertThat(r.observaciones()).contains("Aprobada por admin");
        verify(avisos).registrar("ORDEN_APROBADA", admin, "ordenes_compra", orden.getId());
        verify(avisos).avisarUsuario(eq(7L), eq("Orden de compra aprobada"), anyString());
    }

    @Test
    void alAprobarSeVuelveAVerificarElProveedor() {
        OrdenCompra orden = existente(EstadoOrdenCompra.PENDIENTE, 7L);
        proveedor.setActivo(false);

        assertThatThrownBy(() -> service.aprobar(orden.getId(), admin)).hasMessageContaining("inactivo");
    }

    @Test
    void rechazarLaDevuelveABorradorConElMotivo() {
        OrdenCompra orden = existente(EstadoOrdenCompra.PENDIENTE, 7L);

        OrdenCompraResponse r = service.rechazar(orden.getId(), "Costo del aceite desactualizado", admin);

        assertThat(r.estado()).isEqualTo("BORRADOR");
        assertThat(r.observaciones()).contains("Costo del aceite desactualizado");
    }

    @Test
    void unaAprobadaNoSeModifica() {
        OrdenCompra orden = existente(EstadoOrdenCompra.APROBADA, 7L);

        assertThatThrownBy(() -> service.modificar(orden.getId(), peticion(null, false,
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN)), almacenero))
                .hasMessageContaining("ya no se puede modificar");
    }

    @Test
    void soloUnaAprobadaSeEnvia() {
        OrdenCompra pendiente = existente(EstadoOrdenCompra.PENDIENTE, 7L);
        assertThatThrownBy(() -> service.enviar(pendiente.getId(), admin)).hasMessageContaining("debe estar APROBADA");

        pendiente.setEstado(EstadoOrdenCompra.APROBADA);
        assertThat(service.enviar(pendiente.getId(), admin).estado()).isEqualTo("ENVIADA");
    }

    @Test
    void unaRecibidaNoSeAnula() {
        OrdenCompra orden = existente(EstadoOrdenCompra.RECIBIDA, 7L);

        assertThatThrownBy(() -> service.anular(orden.getId(), "Ya no se necesita", admin))
                .hasMessageContaining("no se puede anular");
    }

    @Test
    void anularDejaElMotivo() {
        OrdenCompra orden = existente(EstadoOrdenCompra.ENVIADA, 7L);

        OrdenCompraResponse r = service.anular(orden.getId(), "El proveedor no tiene stock", admin);

        assertThat(r.estado()).isEqualTo("ANULADA");
        assertThat(r.observaciones()).contains("El proveedor no tiene stock");
        verify(avisos).registrar("ORDEN_ANULADA", admin, "ordenes_compra", orden.getId());
    }

    @Test
    void elCorrelativoEmpiezaEnUnoCadaAnio() {
        when(ordenes.ultimoCorrelativo("OC-2026-")).thenReturn(0);

        assertThat(service.registrar(peticion(null, false,
                new ItemOrdenCompraRequest(10L, BigDecimal.ONE, BigDecimal.TEN)), almacenero).numeroOrden())
                .isEqualTo("OC-2026-0001");
    }

    @Test
    void sugiereLoQueFaltaParaLlegarAlMinimo() {
        when(ordenes.bajoStockMinimo(1)).thenReturn(List.<Object[]>of(
                new Object[]{10L, "ARR-1", "Arroz 1kg", new BigDecimal("3.00"), new BigDecimal("20.00"), new BigDecimal("11.80")}));

        List<SugerenciaReposicionResponse> s = service.sugerencias(1);

        assertThat(s).hasSize(1);
        assertThat(s.get(0).cantidadSugerida()).isEqualByComparingTo("17.00");
        assertThat(s.get(0).ultimoCosto()).isEqualByComparingTo("11.80");
    }

    // --------------------------------------------------------------- apoyo --

    private OrdenCompra existente(EstadoOrdenCompra estado, Long creadoPor) {
        OrdenCompra orden = new OrdenCompra();
        orden.setId(80);
        orden.setNumeroOrden("OC-2026-0005");
        orden.setProveedor(proveedor);
        orden.setCreadoPor(creadoPor);
        orden.setFecha(HOY);
        orden.setEstado(estado);
        when(ordenes.buscarCompleta(80)).thenReturn(Optional.of(orden));
        return orden;
    }

    private static OrdenCompraRequest peticion(BigDecimal descuento, boolean borrador, ItemOrdenCompraRequest... items) {
        return new OrdenCompraRequest(3, HOY.plusDays(5), descuento, null, borrador, List.of(items));
    }

    private static Producto producto(Long id, String nombre) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setCodigoSku("SKU-" + id);
        p.setActivo(true);
        return p;
    }
}
