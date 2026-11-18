package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera;

import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.BalanceGeneralResponse;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.EstadoResultadosResponse;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.InformacionFinancieraRepository;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.SaldoCuentaProjection;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.service.InformacionFinancieraServiceImpl;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformacionFinancieraServiceTest {

    @Mock
    private InformacionFinancieraRepository informacionFinancieraRepository;

    @Mock
    private CuentaContableRepository cuentaContableRepository;

    @Mock
    private PeriodoContableRepository periodoContableRepository;

    @InjectMocks
    private InformacionFinancieraServiceImpl service;

    private CuentaContable cActivoRaiz;
    private CuentaContable cActivoDetalle;
    private CuentaContable cPasivoRaiz;
    private CuentaContable cPasivoDetalle;
    private CuentaContable cPatrimonioRaiz;
    private CuentaContable cPatrimonioDetalle;
    private CuentaContable cIngresoRaiz;
    private CuentaContable cIngresoDetalle;
    private CuentaContable cEgresoRaiz;
    private CuentaContable cCostoDetalle;

    @BeforeEach
    void setUp() {
        cActivoRaiz = CuentaContable.builder().id(1L).codigo("1").nombre("ACTIVO").nivel(1).tipo("ACTIVO").naturaleza("DEUDORA").permiteMovimiento(false).activo(true).build();
        cActivoDetalle = CuentaContable.builder().id(2L).codigo("1.1.1.01").nombre("Caja MN").nivel(2).tipo("ACTIVO").naturaleza("DEUDORA").permiteMovimiento(true).padreId(1L).activo(true).build();

        cPasivoRaiz = CuentaContable.builder().id(3L).codigo("2").nombre("PASIVO").nivel(1).tipo("PASIVO").naturaleza("ACREEDORA").permiteMovimiento(false).activo(true).build();
        cPasivoDetalle = CuentaContable.builder().id(4L).codigo("2.1.1.01").nombre("Proveedores").nivel(2).tipo("PASIVO").naturaleza("ACREEDORA").permiteMovimiento(true).padreId(3L).activo(true).build();

        cPatrimonioRaiz = CuentaContable.builder().id(5L).codigo("3").nombre("PATRIMONIO").nivel(1).tipo("PATRIMONIO").naturaleza("ACREEDORA").permiteMovimiento(false).activo(true).build();
        cPatrimonioDetalle = CuentaContable.builder().id(6L).codigo("3.1.1.01").nombre("Capital Social").nivel(2).tipo("PATRIMONIO").naturaleza("ACREEDORA").permiteMovimiento(true).padreId(5L).activo(true).build();

        cIngresoRaiz = CuentaContable.builder().id(7L).codigo("4").nombre("INGRESOS").nivel(1).tipo("INGRESO").naturaleza("ACREEDORA").permiteMovimiento(false).activo(true).build();
        cIngresoDetalle = CuentaContable.builder().id(8L).codigo("4.1.1.01").nombre("Ventas").nivel(2).tipo("INGRESO").naturaleza("ACREEDORA").permiteMovimiento(true).padreId(7L).activo(true).build();

        cEgresoRaiz = CuentaContable.builder().id(9L).codigo("5").nombre("EGRESOS").nivel(1).tipo("EGRESO").naturaleza("DEUDORA").permiteMovimiento(false).activo(true).build();
        cCostoDetalle = CuentaContable.builder().id(10L).codigo("5.1.1.01").nombre("Costo de Ventas").nivel(2).tipo("EGRESO").naturaleza("DEUDORA").permiteMovimiento(true).padreId(9L).activo(true).build();
    }

    private SaldoCuentaProjection crearProjection(Long cuentaId, BigDecimal debe, BigDecimal haber) {
        SaldoCuentaProjection p = mock(SaldoCuentaProjection.class);
        when(p.getCuentaId()).thenReturn(cuentaId);
        when(p.getTotalDebe()).thenReturn(debe);
        when(p.getTotalHaber()).thenReturn(haber);
        return p;
    }

    @Test
    @DisplayName("Balance General: valida ecuación patrimonial Activo = Pasivo + Patrimonio y suma utilidad")
    void testGenerarBalanceGeneralCuadre() {
        when(cuentaContableRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(List.of(
                cActivoRaiz, cActivoDetalle,
                cPasivoRaiz, cPasivoDetalle,
                cPatrimonioRaiz, cPatrimonioDetalle,
                cIngresoRaiz, cIngresoDetalle,
                cEgresoRaiz, cCostoDetalle
        ));

        // Activo = 500,000 | Pasivo = 100,000 | Capital = 350,000
        // Ingresos = 70,000 | Costos = 20,000 -> Utilidad = 50,000
        // Total Patrimonio Neto = 350,000 + 50,000 = 400,000
        // Pasivo (100,000) + Patrimonio (400,000) = 500,000 (CUADRADO)
        List<SaldoCuentaProjection> saldos = List.of(
                crearProjection(2L, new BigDecimal("500000.00"), BigDecimal.ZERO),
                crearProjection(4L, BigDecimal.ZERO, new BigDecimal("100000.00")),
                crearProjection(6L, BigDecimal.ZERO, new BigDecimal("350000.00")),
                crearProjection(8L, BigDecimal.ZERO, new BigDecimal("70000.00")),
                crearProjection(10L, new BigDecimal("20000.00"), BigDecimal.ZERO)
        );

        when(informacionFinancieraRepository.obtenerSaldosAcumuladosAFecha(any())).thenReturn(saldos);

        BalanceGeneralResponse response = service.generarBalanceGeneral(2026, 1, null, 4);

        assertNotNull(response);
        assertEquals(new BigDecimal("500000.00"), response.cuadre().totalActivos());
        assertEquals(new BigDecimal("100000.00"), response.cuadre().totalPasivos());
        assertEquals(new BigDecimal("350000.00"), response.cuadre().totalPatrimonioCuentas());
        assertEquals(new BigDecimal("50000.00"), response.cuadre().utilidadEjercicio());
        assertEquals(new BigDecimal("400000.00"), response.cuadre().totalPatrimonioNeto());
        assertEquals(new BigDecimal("500000.00"), response.cuadre().totalPasivoMasPatrimonio());
        assertEquals(0, response.cuadre().diferencia().compareTo(BigDecimal.ZERO));
        assertTrue(response.cuadre().estaCuadrado());
    }

    @Test
    @DisplayName("Estado de Resultados: calcula utilidad bruta, neta y análisis vertical correctamente")
    void testGenerarEstadoResultadosEscalonado() {
        when(cuentaContableRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(List.of(
                cIngresoRaiz, cIngresoDetalle,
                cEgresoRaiz, cCostoDetalle
        ));

        // Ingresos = 100,000 | Costos = 40,000 -> Utilidad Bruta = 60,000 (60%)
        List<SaldoCuentaProjection> movs = List.of(
                crearProjection(8L, BigDecimal.ZERO, new BigDecimal("100000.00")),
                crearProjection(10L, new BigDecimal("40000.00"), BigDecimal.ZERO)
        );

        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 1, 31);
        when(informacionFinancieraRepository.obtenerMovimientosPeriodo(inicio, fin)).thenReturn(movs);

        EstadoResultadosResponse response = service.generarEstadoResultados(2026, inicio, fin, 4);

        assertNotNull(response);
        assertEquals(new BigDecimal("100000.00"), response.totalIngresosOperativos());
        assertEquals(new BigDecimal("40000.00"), response.totalCostoVentas());
        assertEquals(new BigDecimal("60000.00"), response.utilidadBruta());
        assertEquals(new BigDecimal("60000.00"), response.utilidadNeta());
        assertEquals(new BigDecimal("60.00"), response.margenBrutoPorcentaje());
        assertEquals(new BigDecimal("60.00"), response.margenNetoPorcentaje());
    }

    @Test
    @DisplayName("Validación: fecha inicial posterior a fecha final arroja BAD_REQUEST")
    void testValidacionFechas() {
        LocalDate inicio = LocalDate.of(2026, 5, 1);
        LocalDate fin = LocalDate.of(2026, 4, 30);

        assertThrows(ResponseStatusException.class, () ->
                service.generarEstadoResultados(null, inicio, fin, 4)
        );
    }
}
