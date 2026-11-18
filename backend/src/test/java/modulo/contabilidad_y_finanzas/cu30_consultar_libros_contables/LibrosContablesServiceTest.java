package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables;

import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto.LibroDiarioResponse;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto.LibroMayorResponse;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository.LibroDiarioRepository;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository.LibroMayorRepository;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.service.LibrosContablesServiceImpl;
import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibrosContablesServiceTest {

    @Mock
    private LibroDiarioRepository libroDiarioRepository;

    @Mock
    private LibroMayorRepository libroMayorRepository;

    @Mock
    private PeriodoContableRepository periodoContableRepository;

    @Mock
    private CuentaContableRepository cuentaContableRepository;

    @InjectMocks
    private LibrosContablesServiceImpl service;

    private PeriodoContable periodo;
    private CuentaContable cuentaCaja;
    private CuentaContable cuentaVenta;

    @BeforeEach
    void setUp() {
        periodo = PeriodoContable.builder()
                .id(1L)
                .gestion(2026)
                .mes(1)
                .nombre("Enero 2026")
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 31))
                .estado("ABIERTO")
                .build();

        cuentaCaja = CuentaContable.builder()
                .id(10L)
                .codigo("1.1.1.01")
                .nombre("Caja Moneda Nacional")
                .nivel(4)
                .tipo("ACTIVO")
                .naturaleza("DEUDORA")
                .permiteMovimiento(true)
                .activo(true)
                .build();

        cuentaVenta = CuentaContable.builder()
                .id(40L)
                .codigo("4.1.1.01")
                .nombre("Ventas de Mercadería")
                .nivel(4)
                .tipo("INGRESO")
                .naturaleza("ACREEDORA")
                .permiteMovimiento(true)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Libro Diario: calcula totales y verifica partida doble correctamente")
    @SuppressWarnings("unchecked")
    void testLibroDiarioCuadre() {
        when(periodoContableRepository.findById(1L)).thenReturn(Optional.of(periodo));

        AsientoContable asiento = AsientoContable.builder()
                .id(100L)
                .numeroAsiento("AS-2026-0001")
                .fecha(LocalDate.of(2026, 1, 15))
                .tipoAsiento("VENTAS")
                .glosa("Venta de productos en efectivo")
                .totalDebe(new BigDecimal("1500.00"))
                .totalHaber(new BigDecimal("1500.00"))
                .estado("MAYORIZADO")
                .detalles(new ArrayList<>())
                .build();

        AsientoDetalle d1 = AsientoDetalle.builder()
                .id(1L)
                .asiento(asiento)
                .cuentaContable(cuentaCaja)
                .glosa("Cobro en caja")
                .debe(new BigDecimal("1500.00"))
                .haber(BigDecimal.ZERO)
                .build();

        AsientoDetalle d2 = AsientoDetalle.builder()
                .id(2L)
                .asiento(asiento)
                .cuentaContable(cuentaVenta)
                .glosa("Ingreso por ventas")
                .debe(BigDecimal.ZERO)
                .haber(new BigDecimal("1500.00"))
                .build();

        asiento.getDetalles().add(d1);
        asiento.getDetalles().add(d2);

        when(libroDiarioRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(asiento));

        LibroDiarioResponse response = service.obtenerLibroDiario(1L, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.totalAsientos());
        assertEquals(new BigDecimal("1500.00"), response.totalDebeGeneral());
        assertEquals(new BigDecimal("1500.00"), response.totalHaberGeneral());
        assertTrue(response.estaCuadrado());
        assertEquals("AS-2026-0001", response.asientos().get(0).numeroAsiento());
        assertEquals(2, response.asientos().get(0).detalles().size());
    }

    @Test
    @DisplayName("Libro Mayor: calcula saldo anterior y saldo progresivo según naturaleza deudora")
    void testLibroMayorDeudora() {
        when(cuentaContableRepository.findById(10L)).thenReturn(Optional.of(cuentaCaja));

        LocalDate inicio = LocalDate.of(2026, 1, 10);
        LocalDate fin = LocalDate.of(2026, 1, 31);

        // Saldo anterior: Debe 500, Haber 200 => Saldo anterior = 300
        when(libroMayorRepository.sumDebeAnterior(10L, inicio)).thenReturn(new BigDecimal("500.00"));
        when(libroMayorRepository.sumHaberAnterior(10L, inicio)).thenReturn(new BigDecimal("200.00"));

        AsientoContable asiento = AsientoContable.builder()
                .id(101L)
                .numeroAsiento("AS-2026-0005")
                .fecha(LocalDate.of(2026, 1, 15))
                .tipoAsiento("VENTAS")
                .glosa("Venta de contado")
                .estado("MAYORIZADO")
                .build();

        AsientoDetalle detalle = AsientoDetalle.builder()
                .id(50L)
                .asiento(asiento)
                .cuentaContable(cuentaCaja)
                .glosa("Ingreso en efectivo")
                .debe(new BigDecimal("700.00"))
                .haber(BigDecimal.ZERO)
                .build();

        when(libroMayorRepository.findMovimientosMayor(10L, inicio, fin)).thenReturn(List.of(detalle));

        LibroMayorResponse response = service.obtenerLibroMayor(10L, null, inicio, fin);

        assertNotNull(response);
        assertEquals(1, response.totalCuentas());
        var cuentaMayor = response.cuentas().get(0);
        assertEquals(new BigDecimal("300.00"), cuentaMayor.saldoAnterior());
        assertEquals(new BigDecimal("700.00"), cuentaMayor.totalDebe());
        assertEquals(BigDecimal.ZERO, cuentaMayor.totalHaber());
        // Saldo final = 300 + 700 - 0 = 1000.00
        assertEquals(new BigDecimal("1000.00"), cuentaMayor.saldoFinal());
        assertEquals(1, cuentaMayor.movimientos().size());
        assertEquals(new BigDecimal("1000.00"), cuentaMayor.movimientos().get(0).saldoAcumulado());
    }

    @Test
    @DisplayName("Validación: fecha de inicio posterior a fecha de fin arroja BAD_REQUEST")
    void testValidacionFechasInvalidas() {
        LocalDate inicio = LocalDate.of(2026, 2, 10);
        LocalDate fin = LocalDate.of(2026, 2, 1);

        assertThrows(ResponseStatusException.class, () ->
                service.obtenerLibroDiario(null, inicio, fin, null, null, null)
        );

        assertThrows(ResponseStatusException.class, () ->
                service.obtenerLibroMayor(null, null, inicio, fin)
        );
    }
}
