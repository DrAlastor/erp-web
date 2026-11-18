package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cálculo puro del arqueo de un turno de caja, sin dependencias de base de datos.
 *
 * <p>Efectivo esperado = monto de apertura + ventas en efectivo + ingresos en efectivo
 * − egresos en efectivo. Las partidas llegan ya sumadas por forma de pago y origen (VENTA,
 * INGRESO, EGRESO) y aquí se agrupan en una línea por forma de pago. Los totales de ingresos
 * y egresos son de movimientos de caja de todas las formas de pago (las ventas van aparte).
 */
public final class CalculadoraArqueo {

    public static final String VENTA = "VENTA";
    public static final String INGRESO = "INGRESO";
    public static final String EGRESO = "EGRESO";

    private CalculadoraArqueo() {}

    /** Monto sumado de una forma de pago para un origen (VENTA, INGRESO o EGRESO). */
    public record Partida(Long formaPagoId, String nombre, boolean efectivo, String origen, BigDecimal monto) {}

    public record Linea(Long formaPagoId, String nombre, boolean efectivo,
                        BigDecimal ingresos, BigDecimal egresos, BigDecimal ventas) {}

    public record Arqueo(
            BigDecimal montoApertura,
            BigDecimal ventasEfectivo,
            BigDecimal ingresosEfectivo,
            BigDecimal egresosEfectivo,
            BigDecimal efectivoEsperado,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            List<Linea> porFormaPago) {}

    public static Arqueo calcular(BigDecimal montoApertura, List<Partida> partidas) {
        Map<Long, BigDecimal[]> montos = new TreeMap<>();
        Map<Long, Partida> formas = new TreeMap<>();
        for (Partida p : partidas) {
            int i = indice(p.origen());
            if (i < 0 || p.formaPagoId() == null) continue;
            formas.putIfAbsent(p.formaPagoId(), p);
            BigDecimal[] m = montos.computeIfAbsent(p.formaPagoId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            m[i] = m[i].add(escala(p.monto()));
        }
        List<Linea> lineas = new ArrayList<>();
        BigDecimal[] efectivo = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalEgresos = BigDecimal.ZERO;
        for (var e : montos.entrySet()) {
            Partida forma = formas.get(e.getKey());
            BigDecimal[] m = e.getValue();
            lineas.add(new Linea(e.getKey(), forma.nombre(), forma.efectivo(), escala(m[0]), escala(m[1]), escala(m[2])));
            totalIngresos = totalIngresos.add(m[0]);
            totalEgresos = totalEgresos.add(m[1]);
            if (forma.efectivo()) {
                for (int i = 0; i < 3; i++) efectivo[i] = efectivo[i].add(m[i]);
            }
        }
        BigDecimal apertura = escala(montoApertura);
        return new Arqueo(apertura, escala(efectivo[2]), escala(efectivo[0]), escala(efectivo[1]),
                efectivoEsperado(apertura, efectivo[2], efectivo[0], efectivo[1]),
                escala(totalIngresos), escala(totalEgresos), lineas);
    }

    public static BigDecimal efectivoEsperado(
            BigDecimal montoApertura, BigDecimal ventasEfectivo, BigDecimal ingresosEfectivo, BigDecimal egresosEfectivo) {
        return escala(escala(montoApertura).add(escala(ventasEfectivo))
                .add(escala(ingresosEfectivo)).subtract(escala(egresosEfectivo)));
    }

    /** Contado − esperado: positivo es sobrante, negativo es faltante. */
    public static BigDecimal diferencia(BigDecimal montoContado, BigDecimal efectivoEsperado) {
        return escala(escala(montoContado).subtract(escala(efectivoEsperado)));
    }

    public static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    /** 0 = ingresos, 1 = egresos, 2 = ventas; -1 si el origen no se reconoce. */
    private static int indice(String origen) {
        if (origen == null) return -1;
        return switch (origen.trim().toUpperCase(Locale.ROOT)) {
            case INGRESO -> 0;
            case EGRESO -> 1;
            case VENTA -> 2;
            default -> -1;
        };
    }
}
