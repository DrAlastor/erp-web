package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.*;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.repository.ResultadosMlRepository;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.repository.ResultadosMlRepository.EjecucionConMetricas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteligenciaNegocioServiceTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-23T14:00:00Z"), LA_PAZ);
    private static final TenantDescriptor EMPRESA = new TenantDescriptor(12L, "ASUSTE-FCF5A4");
    private static final String METRICAS = """
            {"pronostico":{"mape_total_mensual":0.0578,"mape_ingenuo_total_mensual":0.1255},
             "abandono":{"modelo":"RANDOM_FOREST","auc":0.88}}""";

    private ResultadosMlRepository repo;
    private MlClienteService ml;
    private InteligenciaNegocioService service;

    @BeforeEach
    void preparar() {
        repo = mock(ResultadosMlRepository.class);
        ml = mock(MlClienteService.class);
        service = new InteligenciaNegocioService(repo, ml, new ObjectMapper(), RELOJ, 24);
        TenantContext.set(EMPRESA);
    }

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    private Ejecucion ejecucion(String estado, LocalDateTime inicio) {
        return new Ejecucion(1, estado, inicio, inicio.plusMinutes(1), null);
    }

    @Test
    void resumenSinTablasEncolaPrimerEntrenamiento() {
        when(repo.existenResultados()).thenReturn(false);
        when(ml.configurado()).thenReturn(true);
        when(ml.entrenar(EMPRESA)).thenReturn(true);

        Resumen r = service.resumen();

        assertThat(r.entrenado()).isFalse();
        assertThat(r.entrenamientoAutomatico()).isTrue();
        assertThat(r.modelos()).hasSize(6).allSatisfy(m -> assertThat(m.metricas()).isNull());
        verify(ml).entrenar(EMPRESA);
    }

    @Test
    void resumenConResultadosRecientesNoReentrenaYMuestraMetricas() {
        LocalDateTime hace2h = LocalDateTime.now(RELOJ).minusHours(2);
        when(repo.existenResultados()).thenReturn(true);
        when(repo.ultimaEjecucion()).thenReturn(Optional.of(new EjecucionConMetricas(ejecucion("OK", hace2h), METRICAS)));
        when(repo.metricasVigentes()).thenReturn(Optional.of(METRICAS));
        when(ml.configurado()).thenReturn(true);

        Resumen r = service.resumen();

        assertThat(r.entrenado()).isTrue();
        assertThat(r.entrenamientoAutomatico()).isFalse();
        ModeloInfo abandono = r.modelos().stream().filter(m -> m.clave().equals("abandono")).findFirst().orElseThrow();
        assertThat(abandono.metricas().path("auc").asDouble()).isEqualTo(0.88);
        verify(ml, never()).entrenar(any());
    }

    @Test
    void reentrenaSiLosResultadosSonViejosOElEnCursoQuedoColgado() {
        LocalDateTime ahora = LocalDateTime.now(RELOJ);
        assertThat(service.necesitaEntrenar(null)).isTrue();
        assertThat(service.necesitaEntrenar(ejecucion("OK", ahora.minusHours(25)))).isTrue();
        assertThat(service.necesitaEntrenar(ejecucion("OK", ahora.minusHours(3)))).isFalse();
        assertThat(service.necesitaEntrenar(ejecucion("EN_CURSO", ahora.minusMinutes(10)))).isFalse();
        assertThat(service.necesitaEntrenar(ejecucion("EN_CURSO", ahora.minusHours(3)))).isTrue();
    }

    @Test
    void resumenNoFallaSiElMlEstaCaido() {
        when(repo.existenResultados()).thenReturn(false);
        when(ml.configurado()).thenReturn(true);
        when(ml.entrenar(any())).thenThrow(new MlClienteService.MlNoDisponibleException("caido"));

        assertThat(service.resumen().entrenamientoAutomatico()).isFalse();
    }

    @Test
    void entrenarSinConfiguracionDa503() {
        when(ml.configurado()).thenReturn(false);
        assertThatThrownBy(() -> service.entrenar())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
    }

    @Test
    void entrenarUsaLaEmpresaDelToken() {
        when(ml.configurado()).thenReturn(true);
        when(ml.entrenar(EMPRESA)).thenReturn(true);
        assertThat(service.entrenar().encolado()).isTrue();
    }

    @Test
    void historiaMensualTiene24MesesCompletosConCeros() {
        when(repo.historiaVentas(eq("MENSUAL"), any(), any(), any(), any()))
                .thenReturn(Map.of(LocalDate.of(2026, 10, 1), new BigDecimal("500")));

        List<PuntoHistoria> h = service.historia("MENSUAL", null, null);

        assertThat(h).hasSize(24);
        assertThat(h.getFirst().periodo()).isEqualTo(LocalDate.of(2024, 11, 1));
        assertThat(h.getLast().periodo()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(h.getLast().valor()).isEqualByComparingTo("500");
        assertThat(h.get(0).valor()).isEqualByComparingTo("0");
        verify(repo).historiaVentas("MENSUAL", LocalDate.of(2024, 11, 1), LocalDate.of(2026, 11, 1), null, null);
    }

    @Test
    void historiaSemanalEmpiezaEnLunes() {
        when(repo.historiaVentas(any(), any(), any(), any(), any())).thenReturn(Map.of());
        List<PuntoHistoria> h = service.historia("SEMANAL", 1L, null);
        assertThat(h).hasSize(52);
        assertThat(h).allSatisfy(p -> assertThat(p.periodo().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY));
        assertThat(h.getLast().periodo()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void pronosticoPrecalculadoLeeLaTablaPorSucursal() {
        when(repo.historiaVentas(any(), any(), any(), any(), any())).thenReturn(Map.of());
        when(repo.existenResultados()).thenReturn(true);
        when(repo.metricasVigentes()).thenReturn(Optional.of(METRICAS));
        when(repo.pronostico("SUCURSAL", "3", "MENSUAL", 6)).thenReturn(List.of(new PuntoPronostico(
                LocalDate.of(2026, 12, 1), BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("20"))));
        when(repo.modeloDeSerie("SUCURSAL", "3", "MENSUAL")).thenReturn(Optional.of("HOLT"));

        Pronostico p = service.pronostico(null, 3L, null, null);

        assertThat(p.disponible()).isTrue();
        assertThat(p.origen()).isEqualTo("PRECALCULADO");
        assertThat(p.modelo()).isEqualTo("HOLT");
        assertThat(p.ambito()).isEqualTo("SUCURSAL");
        verify(ml, never()).pronostico(any(), any(), any(), any(), anyInt());
    }

    @Test
    void horizonteLargoVaAlServicioDeMl() {
        when(repo.historiaVentas(any(), any(), any(), any(), any())).thenReturn(Map.of());
        when(repo.existenResultados()).thenReturn(true);
        when(repo.metricasVigentes()).thenReturn(Optional.of(METRICAS));
        when(ml.pronostico(EMPRESA, "CATEGORIA", "Laptops", "MENSUAL", 18)).thenReturn(new ObjectMapper().createObjectNode()
                .put("disponible", true).put("modelo", "HOLT_WINTERS_ADITIVO")
                .set("puntos", new ObjectMapper().createArrayNode().add(new ObjectMapper().createObjectNode()
                        .put("periodo", "2026-12-01").put("valor", 10).put("bajo", 5).put("alto", 15))));

        Pronostico p = service.pronostico("mensual", null, "Laptops", 18);

        assertThat(p.origen()).isEqualTo("AL_MOMENTO");
        assertThat(p.pronostico()).hasSize(1);
        verify(repo, never()).pronostico(any(), any(), any(), anyInt());
    }

    @Test
    void pronosticoSinEntrenarAvisaEnVezDeMostrarVacio() {
        when(repo.historiaVentas(any(), any(), any(), any(), any())).thenReturn(Map.of());
        when(repo.existenResultados()).thenReturn(false);
        Pronostico p = service.pronostico("SEMANAL", null, null, null);
        assertThat(p.entrenado()).isFalse();
        assertThat(p.aviso()).isEqualTo(InteligenciaNegocioService.AVISO_SIN_ENTRENAR);
    }

    @Test
    void validaFiltros() {
        assertThatThrownBy(() -> service.pronostico("ANUAL", null, null, null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.pronostico("MENSUAL", 1L, "Laptops", null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.pronostico("MENSUAL", null, null, 25)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.clientesEnRiesgo("CRITICO", null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.canasta(null, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.anomalias(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listasDistinguenSinEntrenarDeSinResultados() {
        when(repo.existenResultados()).thenReturn(true);
        when(repo.reglas(any(), anyInt())).thenReturn(List.of());
        when(repo.metricasVigentes()).thenReturn(Optional.of(METRICAS));
        Lista<ReglaAsociacion> conFiltro = service.canasta("Inexistente", null);
        assertThat(conFiltro.entrenado()).isTrue();
        assertThat(conFiltro.aviso()).isEqualTo(InteligenciaNegocioService.AVISO_SIN_RESULTADOS);

        when(repo.existenResultados()).thenReturn(false);
        Lista<SugerenciaCompra> sinTablas = service.sugerenciaCompra(null, null, null);
        assertThat(sinTablas.entrenado()).isFalse();
        assertThat(sinTablas.aviso()).isEqualTo(InteligenciaNegocioService.AVISO_SIN_ENTRENAR);
    }

    @Test
    void clientesEnRiesgoParseaFactores() {
        when(repo.existenResultados()).thenReturn(true);
        when(repo.clientesEnRiesgo("ALTO", 100)).thenReturn(List.of(new ResultadosMlRepository.FilaRiesgo(
                7, "Ana Paz", "MLD-1", new BigDecimal("0.91"), "ALTO",
                "[{\"variable\":\"Dias desde su ultima compra\",\"valor\":210}]")));
        when(repo.conteoPorNivel()).thenReturn(Map.of("ALTO", 1L, "MEDIO", 0L, "BAJO", 0L));

        ClientesRiesgo r = service.clientesEnRiesgo("alto", null);

        assertThat(r.clientes()).hasSize(1);
        assertThat(r.clientes().getFirst().factores().get(0).path("valor").asInt()).isEqualTo(210);
        assertThat(r.aviso()).isNull();
    }
}
