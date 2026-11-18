package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service;

import modulo.contabilidad_y_finanzas.compartido.Solicitante;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaActualizarRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaNodoResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.mapper.PlanCuentasMapper;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.repository.PlanCuentasRepositorio;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service.BitacoraTenantAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanCuentasServiceTest {

    private final Solicitante contador = new Solicitante(7L, "contador@erp.com", "10.0.0.1");

    private PlanCuentasRepositorio repo;
    private BitacoraTenantAuditoriaService bitacora;
    private PlanCuentasService service;

    @BeforeEach
    void preparar() {
        repo = mock(PlanCuentasRepositorio.class);
        bitacora = mock(BitacoraTenantAuditoriaService.class);
        service = new PlanCuentasService(repo, new PlanCuentasMapper(), bitacora);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            CuentaContable c = inv.getArgument(0);
            if (c.getId() == null) c.setId(99L);
            return c;
        });
        when(repo.save(any())).thenAnswer(returnsFirstArg());
    }

    private static CuentaContable cuenta(long id, String codigo, Long padreId, boolean movimiento, boolean activo) {
        return CuentaContable.builder().id(id).codigo(codigo).nombre("Cuenta " + codigo)
                .nivel(ReglasPlanCuentas.nivel(codigo)).tipo("ACTIVO").naturaleza("DEUDORA")
                .permiteMovimiento(movimiento).padreId(padreId).activo(activo).build();
    }

    private static ResponseStatusException rechazo(Throwable t) {
        return (ResponseStatusException) t;
    }

    @Test
    void creaUnaSubcuentaHeredandoTipoYNaturalezaYElPadrePasaAAgrupacion() {
        CuentaContable padre = cuenta(3, "1.1.1", 2L, true, true);
        when(repo.bloquear(3L)).thenReturn(Optional.of(padre));

        CuentaResponse creada = service.crear(
                new CuentaRequest("1.1.1.03", "Caja Chica", 3L, "PASIVO", "ACREEDORA", null), contador);

        assertThat(creada.codigo()).isEqualTo("1.1.1.03");
        assertThat(creada.nivel()).isEqualTo(4);
        assertThat(creada.tipo()).isEqualTo("ACTIVO");
        assertThat(creada.naturaleza()).isEqualTo("DEUDORA");
        assertThat(creada.permiteMovimiento()).isTrue();
        assertThat(padre.getPermiteMovimiento()).isFalse();
        verify(bitacora).registrar("CUENTA_CREADA", "contador@erp.com", "cuentas_contables", 99L, "10.0.0.1");
    }

    @Test
    void rechazaUnCodigoDuplicado() {
        when(repo.existsByCodigo("1.1")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CuentaRequest("1.1", "X", 1L, null, null, null), contador))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(bitacora, never()).registrar(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void rechazaUnCodigoQueNoCuelgaDelPadre() {
        when(repo.bloquear(3L)).thenReturn(Optional.of(cuenta(3, "1.1.1", 2L, false, true)));

        assertThatThrownBy(() -> service.crear(new CuentaRequest("1.2.1.01", "X", 3L, null, null, null), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void rechazaColgarDeUnPadreInactivo() {
        when(repo.bloquear(3L)).thenReturn(Optional.of(cuenta(3, "1.1.1", 2L, false, false)));

        assertThatThrownBy(() -> service.crear(new CuentaRequest("1.1.1.09", "X", 3L, null, null, null), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rechazaColgarDeUnaCuentaConAsientos() {
        when(repo.bloquear(4L)).thenReturn(Optional.of(cuenta(4, "1.1.3.01", 3L, true, true)));
        when(repo.tieneMovimientos(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CuentaRequest("1.1.3.01.1", "X", 4L, null, null, null), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unaCuentaRaizExigeTipoValidoYUnSoloSegmento() {
        assertThatThrownBy(() -> service.crear(new CuentaRequest("6", "ORDEN", null, "OTRO", null, null), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> service.crear(new CuentaRequest("6.1", "ORDEN", null, "ACTIVO", null, null), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        CuentaResponse raiz = service.crear(new CuentaRequest("6", "Cuentas de orden", null, "pasivo", null, false), contador);
        assertThat(raiz.tipo()).isEqualTo("PASIVO");
        assertThat(raiz.naturaleza()).isEqualTo("ACREEDORA");
        assertThat(raiz.permiteMovimiento()).isFalse();
    }

    @Test
    void unaCuentaConSubcuentasNoPuedePermitirMovimiento() {
        when(repo.bloquear(3L)).thenReturn(Optional.of(cuenta(3, "1.1.1", 2L, false, true)));
        when(repo.existsByPadreId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(3L, new CuentaActualizarRequest("Disponible", true), contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void noDesactivaConSubcuentasActivasNiCuentasDeIntegracion() {
        when(repo.bloquear(3L)).thenReturn(Optional.of(cuenta(3, "1.1.3", 2L, false, true)));
        when(repo.existsByPadreIdAndActivoTrue(3L)).thenReturn(true);
        when(repo.bloquear(4L)).thenReturn(Optional.of(cuenta(4, "1.1.1.01", 3L, true, true)));

        assertThatThrownBy(() -> service.desactivar(3L, contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.desactivar(4L, contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void desactivaYReactivaRegistrandoEnBitacora() {
        CuentaContable hoja = cuenta(5, "1.1.3.02", 3L, true, true);
        when(repo.bloquear(5L)).thenReturn(Optional.of(hoja));
        when(repo.findById(3L)).thenReturn(Optional.of(cuenta(3, "1.1.3", 2L, false, true)));

        assertThat(service.desactivar(5L, contador).activo()).isFalse();
        assertThat(service.reactivar(5L, contador).activo()).isTrue();
        verify(bitacora).registrar("CUENTA_DESACTIVADA", "contador@erp.com", "cuentas_contables", 5L, "10.0.0.1");
        verify(bitacora).registrar("CUENTA_REACTIVADA", "contador@erp.com", "cuentas_contables", 5L, "10.0.0.1");
    }

    @Test
    void noReactivaSiElPadreEstaInactivo() {
        when(repo.bloquear(5L)).thenReturn(Optional.of(cuenta(5, "1.1.3.02", 3L, true, false)));
        when(repo.findById(3L)).thenReturn(Optional.of(cuenta(3, "1.1.3", 2L, false, false)));

        assertThatThrownBy(() -> service.reactivar(5L, contador))
                .satisfies(e -> assertThat(rechazo(e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void armaElArbolOrdenadoYMarcaLasCuentasConAsientos() {
        when(repo.findAllByOrderByCodigoAsc()).thenReturn(List.of(
                cuenta(1, "1", null, false, true),
                cuenta(3, "1.1.1", 2L, false, true),
                cuenta(2, "1.1", 1L, false, true),
                cuenta(4, "1.1.1.01", 3L, true, true),
                cuenta(9, "9.1", 77L, true, true)));
        when(repo.idsConMovimientos()).thenReturn(List.of(4));

        List<CuentaNodoResponse> arbol = service.arbol();

        assertThat(arbol).extracting(n -> n.cuenta().codigo()).containsExactly("1", "9.1");
        CuentaNodoResponse caja = arbol.get(0).hijos().get(0).hijos().get(0).hijos().get(0);
        assertThat(caja.cuenta().codigo()).isEqualTo("1.1.1.01");
        assertThat(caja.cuenta().tieneMovimientos()).isTrue();
        assertThat(caja.cuenta().cuentaIntegracion()).isTrue();
        assertThat(arbol.get(0).cuenta().tieneHijos()).isTrue();
    }
}
