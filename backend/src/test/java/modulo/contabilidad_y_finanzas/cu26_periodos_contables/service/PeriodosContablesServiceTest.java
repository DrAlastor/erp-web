package modulo.contabilidad_y_finanzas.cu26_periodos_contables.service;

import modulo.contabilidad_y_finanzas.compartido.Solicitante;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.GestionResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.PeriodoResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.repository.PeriodosContablesRepositorio;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service.BitacoraTenantAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeriodosContablesServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("UTC"));

    private final Solicitante contador = new Solicitante(7L, "contador@erp.com", "10.0.0.1");

    private PeriodosContablesRepositorio repo;
    private BitacoraTenantAuditoriaService bitacora;
    private PeriodosContablesService service;
    private List<PeriodoContable> gestion2026;

    @BeforeEach
    void preparar() {
        repo = mock(PeriodosContablesRepositorio.class);
        bitacora = mock(BitacoraTenantAuditoriaService.class);
        service = new PeriodosContablesService(repo, bitacora, RELOJ);
        gestion2026 = new ArrayList<>();
        AtomicLong ids = new AtomicLong(100);
        when(repo.bloquearGestion(2026)).thenAnswer(inv -> List.copyOf(gestion2026));
        when(repo.findByGestionOrderByMesAsc(2026)).thenAnswer(inv -> List.copyOf(gestion2026));
        when(repo.saveAllAndFlush(anyList())).thenAnswer(inv -> {
            List<PeriodoContable> nuevos = inv.getArgument(0);
            nuevos.forEach(p -> p.setId(ids.incrementAndGet()));
            gestion2026.addAll(nuevos);
            return nuevos;
        });
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PeriodoContable periodo(int mes, String estado) {
        PeriodoContable p = ReglasPeriodos.nuevo(2026, mes);
        p.setId((long) mes);
        p.setEstado(estado);
        gestion2026.add(p);
        when(repo.findById((long) mes)).thenReturn(Optional.of(p));
        return p;
    }

    private static HttpStatus estado(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    @Test
    void armaElPeriodoMensualConNombreYFechas() {
        PeriodoContable feb = ReglasPeriodos.nuevo(2028, 2);
        assertThat(feb.getNombre()).isEqualTo("Febrero 2028");
        assertThat(feb.getFechaInicio()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(feb.getFechaFin()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(feb.estaAbierto()).isTrue();
    }

    @Test
    void generaSoloLosMesesQueFaltanYEsIdempotente() {
        periodo(1, PeriodoContable.ESTADO_ABIERTO);
        periodo(11, PeriodoContable.ESTADO_ABIERTO);

        GestionResponse primera = service.generarGestion(2026, contador);
        assertThat(primera.creados()).isEqualTo(10);
        assertThat(primera.periodos()).hasSize(12);

        GestionResponse segunda = service.generarGestion(2026, contador);
        assertThat(segunda.creados()).isZero();
        verify(bitacora).registrar("PERIODO_GENERADO", "contador@erp.com", "periodos_contables", 101L, "10.0.0.1");
    }

    @Test
    void rechazaGestionesFueraDelRango() {
        assertThatThrownBy(() -> service.generarGestion(2019, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> service.generarGestion(2028, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void cierraEnOrden() {
        periodo(1, PeriodoContable.ESTADO_CERRADO);
        periodo(2, PeriodoContable.ESTADO_ABIERTO);
        periodo(3, PeriodoContable.ESTADO_ABIERTO);

        assertThatThrownBy(() -> service.cerrar(3L, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));

        PeriodoResponse febrero = service.cerrar(2L, contador);
        assertThat(febrero.estado()).isEqualTo(PeriodoContable.ESTADO_CERRADO);
        assertThat(febrero.fechaCierre()).isNotNull();
        verify(bitacora).registrar("PERIODO_CERRADO", "contador@erp.com", "periodos_contables", 2L, "10.0.0.1");
    }

    @Test
    void noCierraSiFaltaUnMesAnteriorNiUnoYaCerrado() {
        periodo(1, PeriodoContable.ESTADO_CERRADO);
        periodo(3, PeriodoContable.ESTADO_ABIERTO);

        assertThatThrownBy(() -> service.cerrar(3L, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.cerrar(1L, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void soloReabreElUltimoPeriodoCerrado() {
        periodo(1, PeriodoContable.ESTADO_CERRADO);
        PeriodoContable febrero = periodo(2, PeriodoContable.ESTADO_CERRADO);
        periodo(3, PeriodoContable.ESTADO_ABIERTO);

        assertThatThrownBy(() -> service.reabrir(1L, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.reabrir(3L, contador))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));

        PeriodoResponse reabierto = service.reabrir(2L, contador);
        assertThat(reabierto.estado()).isEqualTo(PeriodoContable.ESTADO_ABIERTO);
        assertThat(febrero.getFechaCierre()).isNull();
        verify(bitacora).registrar("PERIODO_REABIERTO", "contador@erp.com", "periodos_contables", 2L, "10.0.0.1");
    }

    @Test
    void noRegistraEnBitacoraCuandoSeRechaza() {
        periodo(1, PeriodoContable.ESTADO_ABIERTO);

        assertThatThrownBy(() -> service.reabrir(1L, contador)).isInstanceOf(ResponseStatusException.class);
        verify(bitacora, never()).registrar(anyString(), anyString(), anyString(), any(), anyString());
    }
}
