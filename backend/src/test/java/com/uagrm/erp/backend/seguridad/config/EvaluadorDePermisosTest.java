package com.uagrm.erp.backend.seguridad.config;

import com.uagrm.erp.backend.auth.UsuarioPrincipal;
import com.uagrm.erp.backend.seguridad.ServicioAutorizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El evaluador es el puente entre {@code @PreAuthorize("hasPermission('MODULO','ACCION')")}
 * y el ServicioAutorizacion. Falla cerrado: ante cualquier duda, no autoriza.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluadorDePermisosTest {

    private static final UsuarioPrincipal PRINCIPAL = new UsuarioPrincipal(
            UUID.fromString("12121212-1212-1212-1212-121212121212"),
            UUID.fromString("13131313-1313-1313-1313-131313131313"),
            "Administrador Demo",
            "admin@demo.bo");

    @Mock
    private ServicioAutorizacion servicioAutorizacion;

    private EvaluadorDePermisos evaluador;

    @BeforeEach
    void prepararEvaluador() {
        evaluador = new EvaluadorDePermisos(servicioAutorizacion);
    }

    private Authentication autenticado() {
        return new UsernamePasswordAuthenticationToken(PRINCIPAL, null, List.of());
    }

    @Test
    @DisplayName("compone el código MODULO_ACCION y delega en el servicio de autorización")
    void componeElCodigoYDelega() {
        when(servicioAutorizacion.tienePermiso(PRINCIPAL.usuarioId(), "SEGURIDAD_MODIFICAR")).thenReturn(true);

        assertThat(evaluador.hasPermission(autenticado(), "SEGURIDAD", "MODIFICAR")).isTrue();

        verify(servicioAutorizacion).tienePermiso(PRINCIPAL.usuarioId(), "SEGURIDAD_MODIFICAR");
    }

    @Test
    @DisplayName("si el servicio dice que no, no autoriza")
    void sinPermisoNoAutoriza() {
        when(servicioAutorizacion.tienePermiso(PRINCIPAL.usuarioId(), "CONTABILIDAD_ANULAR")).thenReturn(false);

        assertThat(evaluador.hasPermission(autenticado(), "CONTABILIDAD", "ANULAR")).isFalse();
    }

    @Test
    @DisplayName("también acepta el código completo en un solo argumento")
    void aceptaElCodigoCompleto() {
        when(servicioAutorizacion.tienePermiso(PRINCIPAL.usuarioId(), "COMERCIAL_ANULAR")).thenReturn(true);

        assertThat(evaluador.hasPermission(autenticado(), null, "COMERCIAL_ANULAR")).isTrue();
    }

    @Test
    @DisplayName("sin nadie autenticado no autoriza ni consulta")
    void sinAutenticacionNoAutoriza() {
        assertThat(evaluador.hasPermission(null, "SEGURIDAD", "CONSULTAR")).isFalse();

        verify(servicioAutorizacion, never()).tienePermiso(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("una autenticación anónima no autoriza")
    void anonimoNoAutoriza() {
        Authentication anonimo = new AnonymousAuthenticationToken(
                "clave", "anonimo", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertThat(evaluador.hasPermission(anonimo, "SEGURIDAD", "CONSULTAR")).isFalse();
    }

    @Test
    @DisplayName("si el principal no es un UsuarioPrincipal no autoriza")
    void principalDeOtroTipoNoAutoriza() {
        Authentication otro = new UsernamePasswordAuthenticationToken("un-string-cualquiera", null, List.of());

        assertThat(evaluador.hasPermission(otro, "SEGURIDAD", "CONSULTAR")).isFalse();
    }

    @Test
    @DisplayName("un código que no existe en el catálogo no autoriza: protege contra un typo en la anotación")
    void codigoInexistenteNoAutoriza() {
        assertThat(evaluador.hasPermission(autenticado(), "SEGURIDAD", "VOLAR")).isFalse();
        assertThat(evaluador.hasPermission(autenticado(), "MODULO_INVENTADO", "CONSULTAR")).isFalse();

        verify(servicioAutorizacion, never()).tienePermiso(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("argumentos vacíos o basura no autorizan y no lanzan excepción")
    void argumentosInvalidosNoAutorizan() {
        assertThat(evaluador.hasPermission(autenticado(), null, null)).isFalse();
        assertThat(evaluador.hasPermission(autenticado(), "", "")).isFalse();
        assertThat(evaluador.hasPermission(autenticado(), "SEGURIDAD", null)).isFalse();
        assertThat(evaluador.hasPermission(autenticado(), 42, 7)).isFalse();
    }

    @Test
    @DisplayName("la variante por id de objeto no se usa en el ERP y no autoriza")
    void laVarianteConIdNoAutoriza() {
        assertThat(evaluador.hasPermission(autenticado(), UUID.randomUUID(), "Rol", "SEGURIDAD_MODIFICAR")).isFalse();
    }
}
