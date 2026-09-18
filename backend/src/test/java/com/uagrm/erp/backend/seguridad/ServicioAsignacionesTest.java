package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.auth.UsuarioPrincipal;
import com.uagrm.erp.backend.seguridad.catalogo.MatrizRolesDeSistema;
import com.uagrm.erp.backend.seguridad.dominio.BitacoraEvento;
import com.uagrm.erp.backend.seguridad.dominio.Rol;
import com.uagrm.erp.backend.seguridad.dominio.Usuario;
import com.uagrm.erp.backend.seguridad.dominio.UsuarioRol;
import com.uagrm.erp.backend.seguridad.dominio.UsuarioRolId;
import com.uagrm.erp.backend.seguridad.error.ReglaDeNegocio;
import com.uagrm.erp.backend.seguridad.error.RecursoNoEncontrado;
import com.uagrm.erp.backend.seguridad.repositorio.BitacoraRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.RolRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRolRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las reglas de este servicio son las validaciones del diagrama de actividad de la HU-03:
 * el rol tiene que estar activo, el usuario no puede tenerlo ya asignado, y el evento queda
 * en la bitácora antes de devolverle al cliente el menú autorizado.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicioAsignacionesTest {

    private static final UUID EMPRESA = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final UsuarioPrincipal ACTOR = new UsuarioPrincipal(
            UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"), EMPRESA, "Administrador Demo", "admin@demo.bo");

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private RolRepositorio rolRepositorio;

    @Mock
    private UsuarioRolRepositorio usuarioRolRepositorio;

    @Mock
    private BitacoraRepositorio bitacoraRepositorio;

    @Mock
    private ServicioAutorizacion servicioAutorizacion;

    private ServicioAsignaciones servicio;

    private Usuario usuario;
    private Rol cajero;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServicioAsignaciones(usuarioRepositorio, rolRepositorio, usuarioRolRepositorio,
                bitacoraRepositorio, servicioAutorizacion);

        usuario = new Usuario(EMPRESA, "cajero@demo.bo", "hash", "Cajero Demo");
        cajero = new Rol(EMPRESA, MatrizRolesDeSistema.porCodigo("CAJERO"), Set.of());

        when(usuarioRepositorio.findByIdAndEmpresaId(usuario.getId(), EMPRESA)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findByIdAndEmpresaId(cajero.getId(), EMPRESA)).thenReturn(Optional.of(cajero));
        when(usuarioRolRepositorio.existsById(new UsuarioRolId(usuario.getId(), cajero.getId()))).thenReturn(false);
    }

    @Test
    @DisplayName("asignar un rol lo guarda, lo audita y recalcula los permisos del usuario")
    void asignaUnRol() {
        servicio.asignar(EMPRESA, usuario.getId(), cajero.getId(), ACTOR);

        ArgumentCaptor<UsuarioRol> asignacion = ArgumentCaptor.captor();
        verify(usuarioRolRepositorio).save(asignacion.capture());
        assertThat(asignacion.getValue().getId().getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(asignacion.getValue().getId().getRolId()).isEqualTo(cajero.getId());
        assertThat(asignacion.getValue().getAsignadoPor()).isEqualTo(ACTOR.usuarioId());

        verify(servicioAutorizacion).invalidarCache(usuario.getId());

        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.captor();
        verify(bitacoraRepositorio).save(evento.capture());
        assertThat(evento.getValue().getAccion()).isEqualTo(BitacoraEvento.ROL_ASIGNADO);
        assertThat(evento.getValue().getDetalle()).contains("CAJERO").contains("cajero@demo.bo");
    }

    @Test
    @DisplayName("asignar dos veces el mismo rol es un error de negocio, no un error del servidor")
    void noSeAsignaDosVecesElMismoRol() {
        when(usuarioRolRepositorio.existsById(new UsuarioRolId(usuario.getId(), cajero.getId()))).thenReturn(true);

        assertThatThrownBy(() -> servicio.asignar(EMPRESA, usuario.getId(), cajero.getId(), ACTOR))
                .isInstanceOf(ReglaDeNegocio.class)
                .hasFieldOrPropertyWithValue("codigo", "ROL_YA_ASIGNADO");

        verify(usuarioRolRepositorio, never()).save(any());
        verify(bitacoraRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("no se puede asignar un rol inactivo")
    void noSeAsignaUnRolInactivo() {
        cajero.setActivo(false);

        assertThatThrownBy(() -> servicio.asignar(EMPRESA, usuario.getId(), cajero.getId(), ACTOR))
                .isInstanceOf(ReglaDeNegocio.class)
                .hasFieldOrPropertyWithValue("codigo", "ROL_INACTIVO");

        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("no se puede asignar un rol de otra empresa")
    void noSeAsignaUnRolDeOtraEmpresa() {
        UUID rolAjeno = UUID.randomUUID();
        when(rolRepositorio.findByIdAndEmpresaId(rolAjeno, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(EMPRESA, usuario.getId(), rolAjeno, ACTOR))
                .isInstanceOf(RecursoNoEncontrado.class)
                .hasFieldOrPropertyWithValue("codigo", "ROL_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("no se puede asignar un rol a un usuario de otra empresa")
    void noSeAsignaAUnUsuarioDeOtraEmpresa() {
        UUID usuarioAjeno = UUID.randomUUID();
        when(usuarioRepositorio.findByIdAndEmpresaId(usuarioAjeno, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(EMPRESA, usuarioAjeno, cajero.getId(), ACTOR))
                .isInstanceOf(RecursoNoEncontrado.class)
                .hasFieldOrPropertyWithValue("codigo", "USUARIO_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("quitar un rol asignado lo borra, lo audita y recalcula los permisos")
    void quitaUnRol() {
        UsuarioRolId clave = new UsuarioRolId(usuario.getId(), cajero.getId());
        when(usuarioRolRepositorio.existsById(clave)).thenReturn(true);

        servicio.quitar(EMPRESA, usuario.getId(), cajero.getId(), ACTOR);

        verify(usuarioRolRepositorio).deleteById(clave);
        verify(servicioAutorizacion).invalidarCache(usuario.getId());

        ArgumentCaptor<BitacoraEvento> evento = ArgumentCaptor.captor();
        verify(bitacoraRepositorio).save(evento.capture());
        assertThat(evento.getValue().getAccion()).isEqualTo(BitacoraEvento.ROL_QUITADO);
    }

    @Test
    @DisplayName("quitar un rol que el usuario no tiene es un 404, no un borrado silencioso")
    void quitarUnRolInexistenteFalla() {
        when(usuarioRolRepositorio.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.quitar(EMPRESA, usuario.getId(), cajero.getId(), ACTOR))
                .isInstanceOf(RecursoNoEncontrado.class)
                .hasFieldOrPropertyWithValue("codigo", "ASIGNACION_NO_ENCONTRADA");

        verify(usuarioRolRepositorio, never()).deleteById(any());
    }
}
