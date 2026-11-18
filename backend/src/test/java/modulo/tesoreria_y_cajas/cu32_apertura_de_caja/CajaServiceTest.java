package modulo.tesoreria_y_cajas.cu32_apertura_de_caja;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.AperturaCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.EstadoCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.mapper.CajaMapper;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.repository.CajaConsultaRepository;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.service.CajaService;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.MovimientoCajaRepository;
import modulo.ventas_y_facturacion.compartido.repository.PagoVentaRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository.SecuenciaFacturacionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CajaServiceTest {

    private static final long CAJERO = 7L;

    private final AlmacenRepository almacenes = mock(AlmacenRepository.class);
    private final TurnoCajaRepository turnos = mock(TurnoCajaRepository.class);
    private final MovimientoCajaRepository movimientos = mock(MovimientoCajaRepository.class);
    private final FormaPagoRepository formas = mock(FormaPagoRepository.class);
    private final PagoVentaRepository pagos = mock(PagoVentaRepository.class);
    private final SecuenciaFacturacionRepository secuencias = mock(SecuenciaFacturacionRepository.class);
    private final CajaConsultaRepository consultas = mock(CajaConsultaRepository.class);

    private final CajaService service = new CajaService(almacenes, turnos, movimientos, formas, pagos,
            secuencias, consultas, new CajaMapper());

    @BeforeEach
    void autenticar() {
        UsuarioPrincipal principal = new UsuarioPrincipal(CAJERO, UUID.randomUUID(), "Cajero", "c@erp.bo");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(turnos.save(any())).thenAnswer(inv -> {
            TurnoCaja t = inv.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Almacen sucursal(boolean activa) {
        return Almacen.builder().id(1).nombre("Central").activo(activa).build();
    }

    private static TurnoCaja turnoAbierto(long cajero) {
        return TurnoCaja.builder().id(50L).sucursalId(1).cajeroId(cajero).montoApertura(new BigDecimal("100.00"))
                .fechaApertura(LocalDateTime.now()).estado(TurnoCaja.ESTADO_ABIERTO).build();
    }

    @Test
    void abreTurnoConElCajeroAutenticadoYObservacionLimpia() {
        when(almacenes.findById(1)).thenReturn(Optional.of(sucursal(true)));
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.empty());

        EstadoCajaResponse.TurnoActivo t = service.abrir(new AperturaCajaRequest(1, new BigDecimal("150.00"), "  Fondo  "));

        assertThat(t.id()).isEqualTo(100L);
        assertThat(t.cajeroId()).isEqualTo(CAJERO);
        assertThat(t.estado()).isEqualTo("ABIERTO");
        assertThat(t.montoApertura()).isEqualByComparingTo("150.00");
        assertThat(t.observaciones()).isEqualTo("Fondo");
        assertThat(t.sucursalNombre()).isEqualTo("Central");
    }

    @Test
    void noPermiteDosTurnosAbiertosDelMismoCajeroAunqueSeaOtraSucursal() {
        when(almacenes.findById(1)).thenReturn(Optional.of(sucursal(true)));
        TurnoCaja otro = turnoAbierto(CAJERO);
        otro.setSucursalId(2);
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.abrir(new AperturaCajaRequest(1, BigDecimal.TEN, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("#50");
        verify(turnos, never()).save(any());
    }

    @Test
    void rechazaSucursalInexistenteOInactiva() {
        when(almacenes.findById(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.abrir(new AperturaCajaRequest(9, BigDecimal.TEN, null)))
                .hasMessageContaining("no encontrada");

        when(almacenes.findById(1)).thenReturn(Optional.of(sucursal(false)));
        assertThatThrownBy(() -> service.abrir(new AperturaCajaRequest(1, BigDecimal.TEN, null)))
                .hasMessageContaining("inactiva");
    }

    @Test
    void estadoSinSucursalDevuelveElTurnoAbiertoDelCajeroConSuResumen() {
        TurnoCaja turno = turnoAbierto(CAJERO);
        when(almacenes.findByActivoTrue()).thenReturn(List.of(sucursal(true)));
        when(almacenes.findById(1)).thenReturn(Optional.of(sucursal(true)));
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.of(turno));
        when(pagos.sumarCobrosPorTurno(50L)).thenReturn(new BigDecimal("300.00"));
        when(pagos.sumarCobrosEfectivoPorTurno(50L)).thenReturn(new BigDecimal("200.00"));
        when(consultas.cobrosCxcEfectivo(50L)).thenReturn(new BigDecimal("20.00"));
        FormaPago efectivo = FormaPago.builder().id(1L).codigo("EFECTIVO").nombre("Efectivo").activo(true).build();
        FormaPago qr = FormaPago.builder().id(3L).codigo("QR_SIMPLE").nombre("QR").activo(true).build();
        when(movimientos.findByTurnoCajaIdOrderByFechaHoraAsc(50L)).thenReturn(List.of(
                MovimientoCaja.builder().tipo("INGRESO").monto(new BigDecimal("30.00")).formaPago(efectivo).build(),
                MovimientoCaja.builder().tipo("EGRESO").monto(new BigDecimal("15.00")).formaPago(efectivo).build(),
                MovimientoCaja.builder().tipo("EGRESO").monto(new BigDecimal("99.00")).formaPago(qr).build()));
        when(formas.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(efectivo, qr));
        when(secuencias.vigentes(any(), any())).thenReturn(List.of());

        EstadoCajaResponse estado = service.estado(null);

        assertThat(estado.turno().id()).isEqualTo(50L);
        assertThat(estado.facturaDisponible()).isFalse();
        assertThat(estado.formasPago()).hasSize(2);
        // 100 fondo + 200 ventas efectivo + 20 cobros CxC + 30 ingreso - 15 egreso (el egreso QR no cuenta)
        assertThat(estado.resumen().montoEsperado()).isEqualByComparingTo("335.00");
        assertThat(estado.resumen().otrosCobros()).isEqualByComparingTo("100.00");
    }

    @Test
    void estadoSinTurnoNoTraeResumen() {
        when(almacenes.findByActivoTrue()).thenReturn(List.of());
        when(turnos.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(CAJERO, "ABIERTO")).thenReturn(Optional.empty());
        when(secuencias.vigentes(any(), any())).thenReturn(List.of());

        EstadoCajaResponse estado = service.estado(null);

        assertThat(estado.turno()).isNull();
        assertThat(estado.resumen()).isNull();
        assertThat(estado.sucursales()).isEmpty();
    }

    @Test
    void cierraConArqueoYRegistraFaltante() {
        TurnoCaja turno = turnoAbierto(CAJERO);
        when(turnos.findById(50L)).thenReturn(Optional.of(turno));
        when(pagos.sumarCobrosPorTurno(anyLong())).thenReturn(new BigDecimal("50.00"));
        when(pagos.sumarCobrosEfectivoPorTurno(anyLong())).thenReturn(new BigDecimal("50.00"));
        when(consultas.cobrosCxcEfectivo(anyLong())).thenReturn(BigDecimal.ZERO);
        when(movimientos.findByTurnoCajaIdOrderByFechaHoraAsc(anyLong())).thenReturn(List.of());

        CierreCajaResponse cierre = service.cerrar(50L, new CierreCajaRequest(new BigDecimal("140.00"), "Arqueo"));

        assertThat(cierre.estado()).isEqualTo("CERRADO");
        assertThat(cierre.montoEsperado()).isEqualByComparingTo("150.00");
        assertThat(cierre.diferencia()).isEqualByComparingTo("-10.00");
        assertThat(turno.getFechaCierre()).isNotNull();
        assertThat(turno.getObservaciones()).isEqualTo("Arqueo");
    }

    @Test
    void soloElCajeroDuenoCierraYNoDosVeces() {
        when(turnos.findById(50L)).thenReturn(Optional.of(turnoAbierto(99L)));
        assertThatThrownBy(() -> service.cerrar(50L, new CierreCajaRequest(BigDecimal.ONE, null)))
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        TurnoCaja cerrado = turnoAbierto(CAJERO);
        cerrado.setEstado("CERRADO");
        when(turnos.findById(51L)).thenReturn(Optional.of(cerrado));
        assertThatThrownBy(() -> service.cerrar(51L, new CierreCajaRequest(BigDecimal.ONE, null)))
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
