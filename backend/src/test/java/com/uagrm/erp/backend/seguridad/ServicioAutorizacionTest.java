package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRolRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ServicioAutorizacion es la clase central del diagrama de clases de la HU-03 y la única
 * fuente de verdad de la autorización. Estas pruebas fijan las tres cosas que tienen que
 * ser ciertas: los permisos efectivos son la unión de los roles activos, la consulta está
 * cacheada, y la caché se invalida cuando el administrador cambia algo — que es el paso
 * "recalcula los permisos efectivos" del diagrama de actividad.
 */
@ExtendWith(MockitoExtension.class)
class ServicioAutorizacionTest {

    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTRO_USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ROL = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private UsuarioRolRepositorio usuarioRolRepositorio;

    private ServicioAutorizacion servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServicioAutorizacion(usuarioRolRepositorio);
    }

    @Test
    @DisplayName("los permisos efectivos son la unión de los roles del usuario, sin duplicados")
    void unionDeLosRolesSinDuplicados() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO)).thenReturn(
                List.of("COMERCIAL_CONSULTAR", "FACTURACION_CREAR", "COMERCIAL_CONSULTAR"));

        assertThat(servicio.permisosEfectivos(USUARIO))
                .containsExactlyInAnyOrder("COMERCIAL_CONSULTAR", "FACTURACION_CREAR");
    }

    @Test
    @DisplayName("tienePermiso responde según los permisos efectivos")
    void tienePermisoConsultaLosEfectivos() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));

        assertThat(servicio.tienePermiso(USUARIO, "COMERCIAL_CONSULTAR")).isTrue();
        assertThat(servicio.tienePermiso(USUARIO, "COMERCIAL_ANULAR")).isFalse();
    }

    @Test
    @DisplayName("un usuario sin roles no tiene ningún permiso")
    void sinRolesNoHayPermisos() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO)).thenReturn(List.of());

        assertThat(servicio.permisosEfectivos(USUARIO)).isEmpty();
        assertThat(servicio.tienePermiso(USUARIO, "COMERCIAL_CONSULTAR")).isFalse();
    }

    @Test
    @DisplayName("los roles inactivos no aportan permisos: la consulta ya los descarta")
    void losRolesInactivosNoAportan() {
        // El repositorio filtra r.activo = true, así que un usuario cuyo único rol está
        // desactivado no recibe nada aunque la asignación siga existiendo.
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO)).thenReturn(List.of());

        assertThat(servicio.permisosEfectivos(USUARIO)).isEmpty();
        verify(usuarioRolRepositorio).findCodigosDePermisosEfectivos(USUARIO);
    }

    @Test
    @DisplayName("dos consultas seguidas pegan una sola vez a la base")
    void losPermisosQuedanEnCache() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));

        servicio.permisosEfectivos(USUARIO);
        servicio.permisosEfectivos(USUARIO);
        servicio.tienePermiso(USUARIO, "COMERCIAL_CONSULTAR");

        verify(usuarioRolRepositorio, times(1)).findCodigosDePermisosEfectivos(USUARIO);
    }

    @Test
    @DisplayName("la caché es por usuario: no se mezclan dos usuarios")
    void laCacheEsPorUsuario() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(OTRO_USUARIO))
                .thenReturn(List.of("CONTABILIDAD_CONSULTAR"));

        assertThat(servicio.permisosEfectivos(USUARIO)).containsExactly("COMERCIAL_CONSULTAR");
        assertThat(servicio.permisosEfectivos(OTRO_USUARIO)).containsExactly("CONTABILIDAD_CONSULTAR");
    }

    @Test
    @DisplayName("invalidar la caché de un usuario obliga a releer sus permisos")
    void invalidarUsuarioRelee() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"), List.of("COMERCIAL_CONSULTAR", "COMERCIAL_ANULAR"));

        assertThat(servicio.permisosEfectivos(USUARIO)).hasSize(1);

        servicio.invalidarCache(USUARIO);

        assertThat(servicio.permisosEfectivos(USUARIO)).hasSize(2);
        verify(usuarioRolRepositorio, times(2)).findCodigosDePermisosEfectivos(USUARIO);
    }

    @Test
    @DisplayName("cambiar un rol invalida la caché de todos sus usuarios")
    void invalidarPorRolAlcanzaATodosSusUsuarios() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(OTRO_USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));
        when(usuarioRolRepositorio.findUsuarioIdsPorRol(ROL)).thenReturn(List.of(USUARIO, OTRO_USUARIO));

        servicio.permisosEfectivos(USUARIO);
        servicio.permisosEfectivos(OTRO_USUARIO);

        servicio.invalidarCacheDeRol(ROL);

        servicio.permisosEfectivos(USUARIO);
        servicio.permisosEfectivos(OTRO_USUARIO);

        verify(usuarioRolRepositorio, times(2)).findCodigosDePermisosEfectivos(USUARIO);
        verify(usuarioRolRepositorio, times(2)).findCodigosDePermisosEfectivos(OTRO_USUARIO);
    }

    @Test
    @DisplayName("invalidar un rol no toca la caché de quien no lo tiene")
    void invalidarPorRolNoAfectaAOtros() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(OTRO_USUARIO))
                .thenReturn(List.of("CONTABILIDAD_CONSULTAR"));
        when(usuarioRolRepositorio.findUsuarioIdsPorRol(ROL)).thenReturn(List.of(USUARIO));

        servicio.permisosEfectivos(OTRO_USUARIO);
        servicio.invalidarCacheDeRol(ROL);
        servicio.permisosEfectivos(OTRO_USUARIO);

        verify(usuarioRolRepositorio, times(1)).findCodigosDePermisosEfectivos(OTRO_USUARIO);
    }

    @Test
    @DisplayName("el conjunto de permisos que se entrega es inmutable")
    void losPermisosEntregadosSonInmutables() {
        when(usuarioRolRepositorio.findCodigosDePermisosEfectivos(USUARIO))
                .thenReturn(List.of("COMERCIAL_CONSULTAR"));

        Set<String> permisos = servicio.permisosEfectivos(USUARIO);

        assertThat(permisos).isUnmodifiable();
    }

    @Test
    @DisplayName("sin usuario o sin código de permiso no se autoriza nada ni se consulta la base")
    void entradasVaciasNoAutorizanNiConsultan() {
        assertThat(servicio.permisosEfectivos(null)).isEmpty();
        assertThat(servicio.tienePermiso(null, "COMERCIAL_CONSULTAR")).isFalse();
        assertThat(servicio.tienePermiso(USUARIO, null)).isFalse();
        assertThat(servicio.tienePermiso(USUARIO, "  ")).isFalse();

        verify(usuarioRolRepositorio, never()).findCodigosDePermisosEfectivos(org.mockito.ArgumentMatchers.any());
    }
}
