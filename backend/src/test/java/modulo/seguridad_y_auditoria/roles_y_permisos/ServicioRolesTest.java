package modulo.seguridad_y_auditoria.roles_y_permisos;

import modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.CatalogoPermisos;
import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.MatrizRolesDeSistema;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.BitacoraEvento;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Permiso;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Rol;
import modulo.seguridad_y_auditoria.roles_y_permisos.error.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.roles_y_permisos.error.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.BitacoraRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.PermisoRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.RolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.RolDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioRolesTest {

    private static final UUID EMPRESA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTRA_EMPRESA = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UsuarioPrincipal ACTOR = new UsuarioPrincipal(
            1L, EMPRESA, "Administrador Demo", "admin@demo.bo");

    @Mock
    private RolRepositorio rolRepositorio;

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @Mock
    private BitacoraRepositorio bitacoraRepositorio;

    @Mock
    private ServicioAutorizacion servicioAutorizacion;

    private ServicioRoles servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServicioRoles(rolRepositorio, permisoRepositorio, bitacoraRepositorio, servicioAutorizacion);
    }

    private List<Permiso> catalogo() {
        return CatalogoPermisos.TODOS.stream().map(Permiso::new).collect(Collectors.toList());
    }

    private Rol rolDe(String codigo) {
        List<Permiso> catalogo = catalogo();
        Set<Permiso> permisos = MatrizRolesDeSistema.porCodigo(codigo).permisos().stream()
                .map(cod -> catalogo.stream().filter(p -> p.getCodigo().equals(cod)).findFirst().orElseThrow())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new Rol(EMPRESA, MatrizRolesDeSistema.porCodigo(codigo), permisos);
    }

    @Test
    @DisplayName("lista los roles de la empresa con su matriz")
    void listaLosRoles() {
        when(rolRepositorio.findByEmpresaIdOrderByNombre(EMPRESA))
                .thenReturn(List.of(rolDe("ADMINISTRADOR"), rolDe("CAJERO")));

        List<RolDto> roles = servicio.listar(EMPRESA);

        assertThat(roles).extracting(RolDto::codigo).containsExactly("ADMINISTRADOR", "CAJERO");
        assertThat(roles.get(0).permisos()).hasSize(24);
        assertThat(roles.get(1).permisos()).hasSize(6);
        assertThat(roles.get(0).esSistema()).isTrue();
    }

    @Test
    @DisplayName("el catálogo de permisos se entrega completo y con etiquetas para la pantalla")
    void entregaElCatalogo() {
        when(permisoRepositorio.findAllByOrderByModuloAscAccionAsc()).thenReturn(catalogo());

        assertThat(servicio.catalogo()).hasSize(24);
        assertThat(servicio.catalogo().get(0).etiquetaModulo()).isNotBlank();
        assertThat(servicio.catalogo().get(0).etiquetaAccion()).isNotBlank();
    }

    @Test
    @DisplayName("reemplazar la matriz de un rol guarda los permisos indicados")
    void reemplazaLaMatriz() {
        Rol cajero = rolDe("CAJERO");
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));
        when(permisoRepositorio.findByCodigoIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(catalogo().stream()
                        .filter(p -> p.getCodigo().equals("COMERCIAL_CONSULTAR") || p.getCodigo().equals("REPORTES_CONSULTAR"))
                        .collect(Collectors.toList()));

        RolDto resultado = servicio.reemplazarMatriz(EMPRESA, cajero.getId(),
                List.of("COMERCIAL_CONSULTAR", "REPORTES_CONSULTAR"), ACTOR);

        assertThat(resultado.permisos()).containsExactlyInAnyOrder("COMERCIAL_CONSULTAR", "REPORTES_CONSULTAR");
        verify(rolRepositorio).save(cajero);
    }

    @Test
    @DisplayName("reemplazar la matriz invalida la caché de todos los usuarios del rol")
    void reemplazarLaMatrizRecalculaLosPermisos() {
        Rol cajero = rolDe("CAJERO");
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));
        when(permisoRepositorio.findByCodigoIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());

        servicio.reemplazarMatriz(EMPRESA, cajero.getId(), List.of(), ACTOR);

        verify(servicioAutorizacion).invalidarCacheDeRol(cajero.getId());
    }

    @Test
    @DisplayName("reemplazar la matriz deja el evento en la bitácora")
    void reemplazarLaMatrizSeAudita() {
        Rol cajero = rolDe("CAJERO");
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));
        when(permisoRepositorio.findByCodigoIn(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of());

        servicio.reemplazarMatriz(EMPRESA, cajero.getId(), List.of(), ACTOR);

        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.captor();
        verify(bitacoraRepositorio).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(BitacoraEvento.MATRIZ_MODIFICADA);
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(EMPRESA);
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(ACTOR.usuarioId());
        assertThat(captor.getValue().getDetalle()).contains("CAJERO");
    }

    @Test
    @DisplayName("no se puede tocar un rol de otra empresa, ni conociendo su id")
    void noSePuedeTocarUnRolDeOtraEmpresa() {
        UUID rolAjeno = UUID.randomUUID();
        when(rolRepositorio.findByIdAndEmpresaId(rolAjeno, OTRA_EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.reemplazarMatriz(OTRA_EMPRESA, rolAjeno, List.of(), ACTOR))
                .isInstanceOf(RecursoNoEncontrado.class);

        verify(rolRepositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("un permiso que no existe en el catálogo se rechaza nombrándolo")
    void rechazaPermisosInexistentes() {
        Rol cajero = rolDe("CAJERO");
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));

        assertThatThrownBy(() -> servicio.reemplazarMatriz(EMPRESA, cajero.getId(),
                List.of("COMERCIAL_CONSULTAR", "VENTA_ANULAR"), ACTOR))
                .isInstanceOf(ReglaDeNegocio.class)
                .hasMessageContaining("VENTA_ANULAR");

        verify(rolRepositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("el rol Administrador no puede quedarse sin los permisos de seguridad")
    void elAdministradorNoPuedeAutobloquearse() {
        Rol administrador = rolDe("ADMINISTRADOR");
        when(rolRepositorio.findByIdAndEmpresaId(administrador.getId(), EMPRESA))
                .thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> servicio.reemplazarMatriz(EMPRESA, administrador.getId(),
                List.of("COMERCIAL_CONSULTAR"), ACTOR))
                .isInstanceOf(ReglaDeNegocio.class)
                .hasMessageContaining("SEGURIDAD");

        verify(rolRepositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("desactivar un rol lo guarda, lo audita y recalcula los permisos")
    void desactivaUnRol() {
        Rol cajero = rolDe("CAJERO");
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));

        RolDto resultado = servicio.cambiarEstado(EMPRESA, cajero.getId(), false, ACTOR);

        assertThat(resultado.activo()).isFalse();
        verify(rolRepositorio).save(cajero);
        verify(servicioAutorizacion).invalidarCacheDeRol(cajero.getId());

        ArgumentCaptor<BitacoraEvento> captor = ArgumentCaptor.captor();
        verify(bitacoraRepositorio).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(BitacoraEvento.ROL_ESTADO_CAMBIADO);
    }

    @Test
    @DisplayName("el rol Administrador no se puede desactivar")
    void elAdministradorNoSeDesactiva() {
        Rol administrador = rolDe("ADMINISTRADOR");
        when(rolRepositorio.findByIdAndEmpresaId(administrador.getId(), EMPRESA))
                .thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> servicio.cambiarEstado(EMPRESA, administrador.getId(), false, ACTOR))
                .isInstanceOf(ReglaDeNegocio.class);

        verify(rolRepositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("volver a activar el rol Administrador sí se permite")
    void elAdministradorSePuedeReactivar() {
        Rol administrador = rolDe("ADMINISTRADOR");
        administrador.setActivo(false);
        when(rolRepositorio.findByIdAndEmpresaId(administrador.getId(), EMPRESA))
                .thenReturn(Optional.of(administrador));

        assertThat(servicio.cambiarEstado(EMPRESA, administrador.getId(), true, ACTOR).activo()).isTrue();
    }
}
