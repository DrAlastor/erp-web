package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service;

import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.PuntoSerie;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Arma la serie de ventas por dia, sin huecos: los dias sin ventas valen 0. */
public final class SerieDiaria {

    private SerieDiaria() {}

    /** Un punto por cada dia de [desde, hasta], ambos incluidos, en orden ascendente. */
    public static List<PuntoSerie> rellenar(Map<LocalDate, BigDecimal> totales, LocalDate desde, LocalDate hasta) {
        List<PuntoSerie> serie = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            BigDecimal total = totales.getOrDefault(dia, BigDecimal.ZERO);
            serie.add(new PuntoSerie(dia, total.setScale(2, RoundingMode.HALF_UP)));
        }
        return serie;
    }
}
