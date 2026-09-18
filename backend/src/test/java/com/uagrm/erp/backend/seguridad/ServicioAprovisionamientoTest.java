package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.seguridad.catalogo.CatalogoPermisos;
import com.uagrm.erp.backend.seguridad.catalogo.DefinicionPermiso;
import com.uagrm.erp.backend.seguridad.dominio.Permiso;
import com.uagrm.erp.backend.seguridad.dominio.Rol;
import com.uagrm.erp.backend.seguridad.repositorio.PermisoRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.RolRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El aprovisionamiento es lo que lleva el catálogo y la matriz definidos en Java hasta la
 * base de datos de cada empresa. Tiene que ser idempotente: corre en cada arranque y no
 * puede pisar la matriz que el administrador haya editado a mano.
 */
@ExtendWith(MockitoExtension.class)
class ServicioAprovisionamientoTest {

    private static final UUID EMPRESA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @Mock
    private RolRepositorio rolRepositorio;

    private ServicioAprovisionamiento servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServicioAprovisionamiento(permisoRepositorio, rolRepositorio);
    }

    private List<Permiso> catalogoCompletoEnBase() {
        return CatalogoPermisos.TODOS.stream().map(Permiso::new).collect(Collectors.toList());
    }

    @Test
    @DisplayName("con la base vacía siembra los 24 permisos del catálogo")
    void siembraElCatalogoCompleto() {
        when(permisoRepositorio.findAll()).thenReturn(List.of());

        int creados = servicio.sincronizarCatalogoDePermisos();

        assertThat(creados).isEqualTo(24);

        ArgumentCaptor<List<Permiso>> captor = ArgumentCaptor.captor();
        verify(permisoRepositorio).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Permiso::getCodigo)
                .containsExactlyInAnyOrderElementsOf(CatalogoPermisos.codigos());
    }

    @Test
    @DisplayName("si el catálogo ya está completo no inserta nada")
    void noDuplicaPermisos() {
        when(permisoRepositorio.findAll()).thenReturn(catalogoCompletoEnBase());

        int creados = servicio.sincronizarCatalogoDePermisos();

        assertThat(creados).isZero();
        verify(permisoRepositorio, never()).saveAll(any());
    }

    @Test
    @DisplayName("inserta únicamente los permisos que falten")
    void insertaSoloLosFaltantes() {
        List<Permiso> parcial = new ArrayList<>(catalogoCompletoEnBase());
        Permiso quitado = parcial.remove(0);
        when(permisoRepositorio.findAll()).thenReturn(parcial);

        int creados = servicio.sincronizarCatalogoDePermisos();

        assertThat(creados).isEqualTo(1);

        ArgumentCaptor<List<Permiso>> captor = ArgumentCaptor.captor();
        verify(permisoRepositorio).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Permiso::getCodigo)
                .containsExactly(quitado.getCodigo());
    }

    @Test
    @DisplayName("aprovisionar una empresa crea sus siete roles de sistema")
    void creaLosSieteRoles() {
        when(permisoRepositorio.findAll()).thenReturn(catalogoCompletoEnBase());
        when(rolRepositorio.findByEmpresaIdAndCodigo(eq(EMPRESA), anyString())).thenReturn(Optional.empty());

        int creados = servicio.aprovisionarEmpresa(EMPRESA);

        assertThat(creados).isEqualTo(7);

        ArgumentCaptor<Rol> captor = ArgumentCaptor.captor();
        verify(rolRepositorio, org.mockito.Mockito.times(7)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Rol::getCodigo)
                .containsExactlyInAnyOrder("ADMINISTRADOR", "GERENTE_GENERAL", "CONTADOR",
                        "CAJERO", "ENCARGADO_ALMACEN", "PREVENTISTA", "AUDITOR_INTERNO");
        assertThat(captor.getAllValues()).allSatisfy(rol -> {
            assertThat(rol.getEmpresaId()).isEqualTo(EMPRESA);
            assertThat(rol.isEsSistema()).isTrue();
            assertThat(rol.isActivo()).isTrue();
        });
    }

    @Test
    @DisplayName("el administrador se siembra con los 24 permisos y el preventista con 3")
    void siembraLaMatrizDelDocumento() {
        when(permisoRepositorio.findAll()).thenReturn(catalogoCompletoEnBase());
        when(rolRepositorio.findByEmpresaIdAndCodigo(eq(EMPRESA), anyString())).thenReturn(Optional.empty());

        servicio.aprovisionarEmpresa(EMPRESA);

        ArgumentCaptor<Rol> captor = ArgumentCaptor.captor();
        verify(rolRepositorio, org.mockito.Mockito.times(7)).save(captor.capture());

        Rol administrador = captor.getAllValues().stream()
                .filter(rol -> rol.getCodigo().equals("ADMINISTRADOR")).findFirst().orElseThrow();
        Rol preventista = captor.getAllValues().stream()
                .filter(rol -> rol.getCodigo().equals("PREVENTISTA")).findFirst().orElseThrow();

        assertThat(administrador.getPermisos()).hasSize(24);
        assertThat(preventista.getPermisos())
                .extracting(Permiso::getCodigo)
                .containsExactlyInAnyOrder("COMERCIAL_CREAR", "COMERCIAL_CONSULTAR", "INVENTARIO_CONSULTAR");
    }

    @Test
    @DisplayName("aprovisionar dos veces no duplica roles ni pisa la matriz ya editada")
    void esIdempotente() {
        when(permisoRepositorio.findAll()).thenReturn(catalogoCompletoEnBase());
        when(rolRepositorio.findByEmpresaIdAndCodigo(eq(EMPRESA), anyString()))
                .thenAnswer(invocacion -> Optional.of(new Rol()));

        int creados = servicio.aprovisionarEmpresa(EMPRESA);

        assertThat(creados).isZero();
        verify(rolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("si al catálogo de la base le falta un permiso, el aprovisionamiento lo dice claro")
    void avisaSiElCatalogoEstaIncompleto() {
        List<Permiso> parcial = catalogoCompletoEnBase().stream()
                .filter(permiso -> !permiso.getCodigo().equals("COMERCIAL_CREAR"))
                .collect(Collectors.toList());
        when(permisoRepositorio.findAll()).thenReturn(parcial);
        when(rolRepositorio.findByEmpresaIdAndCodigo(eq(EMPRESA), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aprovisionarEmpresa(EMPRESA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMERCIAL_CREAR");
    }

    @Test
    @DisplayName("un permiso construido desde el catálogo conserva módulo, acción y descripción")
    void elPermisoSeConstruyeDesdeLaDefinicion() {
        DefinicionPermiso definicion = CatalogoPermisos.TODOS.stream()
                .filter(permiso -> permiso.codigo().equals("COMERCIAL_ANULAR"))
                .findFirst().orElseThrow();

        Permiso permiso = new Permiso(definicion);

        assertThat(permiso.getId()).isNotNull();
        assertThat(permiso.getCodigo()).isEqualTo("COMERCIAL_ANULAR");
        assertThat(permiso.getModulo()).isEqualTo(definicion.modulo());
        assertThat(permiso.getAccion()).isEqualTo(definicion.accion());
        assertThat(permiso.getDescripcion()).isEqualTo(definicion.descripcion());
    }

    @Test
    @DisplayName("un rol sembrado guarda exactamente los permisos que se le pasaron")
    void elRolSeConstruyeConSusPermisos() {
        Permiso permiso = new Permiso(CatalogoPermisos.TODOS.get(0));

        Rol rol = new Rol(EMPRESA,
                com.uagrm.erp.backend.seguridad.catalogo.MatrizRolesDeSistema.porCodigo("CAJERO"),
                Set.of(permiso));

        assertThat(rol.getId()).isNotNull();
        assertThat(rol.getEmpresaId()).isEqualTo(EMPRESA);
        assertThat(rol.getCodigo()).isEqualTo("CAJERO");
        assertThat(rol.getNombre()).isEqualTo("Cajero");
        assertThat(rol.getPermisos()).containsExactly(permiso);
        assertThat(rol.isEsSistema()).isTrue();
        assertThat(rol.isActivo()).isTrue();
    }
}
