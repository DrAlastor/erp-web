package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis;

import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.PuntoSerie;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service.SerieDiaria;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SerieDiariaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    @Test
    void rellenaLosDiasSinVentaConCeroYMantieneElOrden() {
        LocalDate desde = HOY.minusDays(29);
        List<PuntoSerie> serie = SerieDiaria.rellenar(
                Map.of(HOY, new BigDecimal("120.5"), desde, new BigDecimal("80")), desde, HOY);

        assertThat(serie).hasSize(30);
        assertThat(serie.get(0).fecha()).isEqualTo(desde);
        assertThat(serie.get(0).total()).isEqualByComparingTo("80.00");
        assertThat(serie.get(1).total()).isEqualByComparingTo("0.00");
        assertThat(serie.get(29).fecha()).isEqualTo(HOY);
        assertThat(serie.get(29).total()).isEqualByComparingTo("120.50");
        assertThat(serie).allSatisfy(p -> assertThat(p.total().scale()).isEqualTo(2));
    }

    @Test
    void cruzaCambiosDeMesSinSaltarDias() {
        List<PuntoSerie> serie = SerieDiaria.rellenar(
                Map.of(), LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 2));

        assertThat(serie).extracting(PuntoSerie::fecha).containsExactly(
                LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
    }
}
