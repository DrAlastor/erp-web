package modulo.contabilidad_y_finanzas.cu26_periodos_contables.service;

import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Reglas puras de los periodos contables mensuales (sin base de datos). */
public final class ReglasPeriodos {

    /** Años hacia atrás y hacia adelante del año actual en los que se puede generar una gestión. */
    public static final int ANIOS_ATRAS = 5;
    public static final int ANIOS_ADELANTE = 1;

    private ReglasPeriodos() {
    }

    public static boolean gestionPermitida(int gestion, int anioActual) {
        return gestion >= anioActual - ANIOS_ATRAS && gestion <= anioActual + ANIOS_ADELANTE;
    }

    /** Periodo nuevo, ABIERTO, para el mes indicado: "Noviembre 2026", del 1 al último día. */
    public static PeriodoContable nuevo(int gestion, int mes) {
        YearMonth ym = YearMonth.of(gestion, mes);
        return PeriodoContable.builder()
                .gestion(gestion)
                .mes(mes)
                .nombre(nombre(gestion, mes))
                .fechaInicio(ym.atDay(1))
                .fechaFin(ym.atEndOfMonth())
                .estado(PeriodoContable.ESTADO_ABIERTO)
                .build();
    }

    public static String nombre(int gestion, int mes) {
        String texto = Month.of(mes).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1) + " " + gestion;
    }

    public static boolean contiene(PeriodoContable periodo, LocalDate fecha) {
        return !fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin());
    }
}
