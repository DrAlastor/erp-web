package modulo.catalogo_e_inventario.cu12_consultar_kardex;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.KardexCalculador;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.KardexCalculador.Calculo;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.MetodoCosteo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KardexCalculadorTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 1, 8, 0);

    private static KardexMovimiento mov(int orden, String tipo, String cantidad, String costo, String saldoCantidad) {
        return KardexMovimiento.builder()
                .id((long) orden)
                .tipoMovimiento(tipo)
                .cantidad(new BigDecimal(cantidad))
                .costoUnitario(new BigDecimal(costo))
                .saldoCantidad(new BigDecimal(saldoCantidad))
                .saldoValorado(BigDecimal.ZERO)
                .fecha(INICIO.plusDays(orden))
                .build();
    }

    private static void assertDecimal(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " pero fue " + real);
    }

    @Test
    @DisplayName("Promedio ponderado: la salida sale al costo promedio vigente")
    void promedioPonderado() {
        List<KardexMovimiento> movimientos = List.of(
                mov(1, "ENTRADA", "10", "100", "10"),
                mov(2, "ENTRADA", "10", "120", "20"),
                mov(3, "SALIDA", "5", "120", "15"));

        Calculo calculo = KardexCalculador.calcular(movimientos, MetodoCosteo.PROMEDIO);

        assertDecimal("0", calculo.aperturaCantidad());
        assertDecimal("2200", calculo.pasos().get(1).saldoValor());
        assertDecimal("110", calculo.pasos().get(1).saldoCostoUnitario());
        assertDecimal("110", calculo.pasos().get(2).costoUnitario());
        assertDecimal("550", calculo.pasos().get(2).valor());
        assertDecimal("15", calculo.saldoFinalCantidad());
        assertDecimal("1650", calculo.saldoFinalValor());
    }

    @Test
    @DisplayName("PEPS: la salida consume primero el lote más antiguo")
    void peps() {
        List<KardexMovimiento> movimientos = List.of(
                mov(1, "ENTRADA", "10", "100", "10"),
                mov(2, "ENTRADA", "10", "120", "20"),
                mov(3, "SALIDA", "5", "120", "15"),
                mov(4, "SALIDA", "8", "120", "7"));

        Calculo calculo = KardexCalculador.calcular(movimientos, MetodoCosteo.PEPS);

        assertDecimal("500", calculo.pasos().get(2).valor());
        assertDecimal("1700", calculo.pasos().get(2).saldoValor());
        // 5 unidades del lote a 100 y 3 del lote a 120
        assertDecimal("860", calculo.pasos().get(3).valor());
        assertDecimal("7", calculo.saldoFinalCantidad());
        assertDecimal("840", calculo.saldoFinalValor());
    }

    @Test
    @DisplayName("El stock previo al primer movimiento se toma como saldo de apertura")
    void aperturaImplicita() {
        // El primer registro es una salida de 3 que dejó 7: antes había 10 sin movimientos.
        List<KardexMovimiento> movimientos = List.of(mov(1, "SALIDA", "3", "50", "7"));

        Calculo calculo = KardexCalculador.calcular(movimientos, MetodoCosteo.PROMEDIO);

        assertDecimal("10", calculo.aperturaCantidad());
        assertDecimal("500", calculo.aperturaValor());
        assertDecimal("150", calculo.pasos().get(0).valor());
        assertDecimal("7", calculo.saldoFinalCantidad());
        assertDecimal("350", calculo.saldoFinalValor());
    }

    @Test
    @DisplayName("Una salida mayor a la existencia no deja saldo negativo")
    void salidaMayorQueExistencia() {
        List<KardexMovimiento> movimientos = List.of(
                mov(1, "ENTRADA", "2", "10", "2"),
                mov(2, "SALIDA", "5", "10", "0"));

        for (MetodoCosteo metodo : MetodoCosteo.values()) {
            Calculo calculo = KardexCalculador.calcular(movimientos, metodo);

            assertDecimal("0", calculo.saldoFinalCantidad());
            assertDecimal("0", calculo.saldoFinalValor());
            assertDecimal("50", calculo.pasos().get(1).valor());
        }
    }

    @Test
    @DisplayName("Sin movimientos el Kardex es vacío")
    void sinMovimientos() {
        Calculo calculo = KardexCalculador.calcular(List.of(), MetodoCosteo.PEPS);

        assertTrue(calculo.pasos().isEmpty());
        assertDecimal("0", calculo.saldoFinalCantidad());
        assertDecimal("0", calculo.saldoFinalValor());
    }

    @Test
    @DisplayName("Los ajustes se clasifican como entrada o salida según su signo")
    void clasificaTipos() {
        assertTrue(KardexCalculador.esEntrada("ENTRADA"));
        assertTrue(KardexCalculador.esEntrada("AJUSTE_POSITIVO"));
        assertFalse(KardexCalculador.esEntrada("SALIDA"));
        assertFalse(KardexCalculador.esEntrada("AJUSTE_NEGATIVO"));
        assertFalse(KardexCalculador.esEntrada(null));
    }
}
