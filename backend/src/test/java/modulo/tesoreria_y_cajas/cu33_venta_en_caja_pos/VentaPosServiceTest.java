package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosRequest;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosResponse;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.service.VentaPosService;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VentaPosServiceTest {

    private static final long CAJERO = 7L;

    private final TurnoCajaRepository turnos = mock(TurnoCajaRepository.class);
    private final FormaPagoRepository formas = mock(FormaPagoRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final VentaService ventas = mock(VentaService.class);
    private final VentaPosService service = new VentaPosService(turnos, formas, productos, ventas);

    private final TurnoCaja turno = TurnoCaja.builder().id(50L).sucursalId(3).cajeroId(CAJERO)
            .montoApertura(BigDecimal.TEN).fechaApertura(LocalDateTime.now()).estado("ABIERTO").build();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(CAJERO, UUID.randomUUID(), "Cajero", "c@erp.bo"), null, List.of()));
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.of(turno));
        when(formas.findById(1L)).thenReturn(Optional.of(forma(1L, "EFECTIVO")));
        when(formas.findById(2L)).thenReturn(Optional.of(forma(2L, "TARJETA")));
        when(formas.findById(5L)).thenReturn(Optional.of(forma(5L, "PASARELA_ONLINE")));
        when(productos.findById(10L)).thenReturn(Optional.of(Producto.builder().id(10L).nombre("Mouse").precioVenta(new BigDecimal("25.00")).build()));
        when(productos.findById(11L)).thenReturn(Optional.of(Producto.builder().id(11L).nombre("Cable").precioVenta(new BigDecimal("7.50")).build()));
        when(ventas.registrar(any())).thenAnswer(inv -> respuesta(inv.getArgument(0)));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static FormaPago forma(Long id, String codigo) {
        return FormaPago.builder().id(id).codigo(codigo).nombre(codigo).activo(true).build();
    }

    private static VentaResponse respuesta(VentaRequest r) {
        BigDecimal total = r.pagos().get(0).importe();
        return new VentaResponse(900L, r.comprobante(), 12, null, null, null, LocalDateTime.now(), "EMITIDA",
                r.tipoVenta(), r.sucursalId(), "Central", 1L, "0", "Cliente Pasante / Sin Nombre", CAJERO,
                r.turnoCajaId(), total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total,
                List.of(), List.of(), List.of(), null, null, null, null);
    }

    private static VentaPosRequest.Linea linea(long producto, String cantidad) {
        return new VentaPosRequest.Linea(producto, new BigDecimal(cantidad));
    }

    @Test
    void venderUsaElTurnoYSucursalDelCajeroAlContadoConCambio() {
        VentaPosResponse r = service.vender(new VentaPosRequest(null, "NOTA_VENTA", 1L, new BigDecimal("100"),
                List.of(linea(10, "2"), linea(11, "1"))));

        ArgumentCaptor<VentaRequest> captor = ArgumentCaptor.forClass(VentaRequest.class);
        verify(ventas).registrar(captor.capture());
        VentaRequest enviado = captor.getValue();
        assertThat(enviado.sucursalId()).isEqualTo(3);
        assertThat(enviado.turnoCajaId()).isEqualTo(50L);
        assertThat(enviado.tipoVenta()).isEqualTo("CONTADO");
        assertThat(enviado.pagos()).singleElement().satisfies(p -> {
            assertThat(p.formaPagoId()).isEqualTo(1L);
            assertThat(p.importe()).isEqualByComparingTo("57.50");
        });
        assertThat(r.total()).isEqualByComparingTo("57.50");
        assertThat(r.cambio()).isEqualByComparingTo("42.50");
        assertThat(r.montoRecibido()).isEqualByComparingTo("100");
        assertThat(r.turnoCajaId()).isEqualTo(50L);
    }

    @Test
    void lineasRepetidasDelMismoProductoSeAgrupan() {
        service.vender(new VentaPosRequest(null, null, 2L, null, List.of(linea(10, "1"), linea(10, "2"))));

        ArgumentCaptor<VentaRequest> captor = ArgumentCaptor.forClass(VentaRequest.class);
        verify(ventas).registrar(captor.capture());
        assertThat(captor.getValue().detalles()).singleElement()
                .satisfies(l -> assertThat(l.cantidad()).isEqualByComparingTo("3"));
        assertThat(captor.getValue().comprobante()).isEqualTo("NOTA_VENTA");
    }

    @Test
    void pagoNoEfectivoNoTieneCambio() {
        VentaPosResponse r = service.vender(new VentaPosRequest(null, "FACTURA", 2L, new BigDecimal("999"),
                List.of(linea(10, "1"))));
        assertThat(r.cambio()).isEqualByComparingTo("0");
        assertThat(r.montoRecibido()).isEqualByComparingTo("25.00");
    }

    @Test
    void sinTurnoAbiertoNoSePuedeVender() {
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vender(new VentaPosRequest(null, null, 1L, null, List.of(linea(10, "1")))))
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.productos(""))
                .hasMessageContaining("turno de caja abierto");
        verify(ventas, never()).registrar(any());
    }

    @Test
    void rechazaPasarelaEnLineaEfectivoInsuficienteYFormaInactiva() {
        assertThatThrownBy(() -> service.vender(new VentaPosRequest(null, null, 5L, null, List.of(linea(10, "1")))))
                .hasMessageContaining("presencial");
        assertThatThrownBy(() -> service.vender(new VentaPosRequest(null, null, 1L, new BigDecimal("10"), List.of(linea(10, "1")))))
                .hasMessageContaining("no cubre");
        FormaPago inactiva = forma(4L, "TRANSFERENCIA");
        inactiva.setActivo(false);
        when(formas.findById(4L)).thenReturn(Optional.of(inactiva));
        assertThatThrownBy(() -> service.vender(new VentaPosRequest(null, null, 4L, null, List.of(linea(10, "1")))))
                .hasMessageContaining("inactiva");
        verify(ventas, never()).registrar(any());
    }

    @Test
    void productosSeBuscanEnLaSucursalDelTurno() {
        service.productos("mou");
        verify(ventas).productos(3, "mou");
    }
}
