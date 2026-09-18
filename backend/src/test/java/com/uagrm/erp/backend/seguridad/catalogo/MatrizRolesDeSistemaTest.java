package com.uagrm.erp.backend.seguridad.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Esta clase de prueba es el guardián del documento entregado: la matriz de permisos
 * por rol de la HU-03 tiene totales exactos y acá se verifican uno por uno. Si alguien
 * cambia la matriz sin actualizar el documento, estas pruebas se caen.
 */
class MatrizRolesDeSistemaTest {

    @Test
    @DisplayName("están los siete roles del documento")
    void estanLosSieteRoles() {
        assertThat(MatrizRolesDeSistema.codigos()).containsExactlyInAnyOrder(
                "ADMINISTRADOR",
                "GERENTE_GENERAL",
                "CONTADOR",
                "CAJERO",
                "ENCARGADO_ALMACEN",
                "PREVENTISTA",
                "AUDITOR_INTERNO");
    }

    @Test
    @DisplayName("los totales de permisos por rol son los de la matriz del documento")
    void losTotalesCoincidenConElDocumento() {
        Map<String, Integer> totalesDelDocumento = Map.of(
                "ADMINISTRADOR", 24,
                "GERENTE_GENERAL", 5,
                "CONTADOR", 9,
                "CAJERO", 6,
                "ENCARGADO_ALMACEN", 5,
                "PREVENTISTA", 3,
                "AUDITOR_INTERNO", 6);

        totalesDelDocumento.forEach((codigoRol, total) ->
                assertThat(MatrizRolesDeSistema.porCodigo(codigoRol).permisos())
                        .as("permisos del rol %s", codigoRol)
                        .hasSize(total));
    }

    @Test
    @DisplayName("el administrador tiene el catálogo completo")
    void elAdministradorTieneTodo() {
        assertThat(MatrizRolesDeSistema.porCodigo("ADMINISTRADOR").permisos())
                .isEqualTo(CatalogoPermisos.codigos());
    }

    @Test
    @DisplayName("el cajero tiene exactamente sus seis permisos")
    void elCajeroTieneSusSeisPermisos() {
        assertThat(MatrizRolesDeSistema.porCodigo("CAJERO").permisos())
                .containsExactlyInAnyOrder(
                        "COMERCIAL_CREAR",
                        "COMERCIAL_CONSULTAR",
                        "INVENTARIO_CONSULTAR",
                        "FACTURACION_CREAR",
                        "FACTURACION_CONSULTAR",
                        "FACTURACION_ANULAR");
    }

    @Test
    @DisplayName("el contador manda en contabilidad y solo consulta el resto")
    void elContadorMandaEnContabilidad() {
        assertThat(MatrizRolesDeSistema.porCodigo("CONTADOR").permisos())
                .containsExactlyInAnyOrder(
                        "COMERCIAL_CONSULTAR",
                        "INVENTARIO_CONSULTAR",
                        "CONTABILIDAD_CREAR",
                        "CONTABILIDAD_CONSULTAR",
                        "CONTABILIDAD_MODIFICAR",
                        "CONTABILIDAD_ANULAR",
                        "FACTURACION_CONSULTAR",
                        "REPORTES_CREAR",
                        "REPORTES_CONSULTAR");
    }

    @Test
    @DisplayName("el preventista solo registra en comercial y consulta inventario")
    void elPreventistaEsElMasRestringido() {
        assertThat(MatrizRolesDeSistema.porCodigo("PREVENTISTA").permisos())
                .containsExactlyInAnyOrder(
                        "COMERCIAL_CREAR",
                        "COMERCIAL_CONSULTAR",
                        "INVENTARIO_CONSULTAR");
    }

    @Test
    @DisplayName("el auditor interno solo consulta, en los seis módulos")
    void elAuditorSoloConsulta() {
        Set<String> permisos = MatrizRolesDeSistema.porCodigo("AUDITOR_INTERNO").permisos();

        assertThat(permisos).hasSize(6);
        assertThat(permisos).allSatisfy(codigo -> assertThat(codigo).endsWith("_CONSULTAR"));
    }

    @Test
    @DisplayName("solo el administrador puede escribir en el módulo de seguridad")
    void soloElAdministradorEscribeEnSeguridad() {
        for (RolDeSistema rol : MatrizRolesDeSistema.ROLES) {
            if (rol.codigo().equals("ADMINISTRADOR")) {
                continue;
            }
            assertThat(rol.permisos())
                    .as("el rol %s no debe poder modificar la seguridad", rol.codigo())
                    .doesNotContain("SEGURIDAD_CREAR", "SEGURIDAD_MODIFICAR", "SEGURIDAD_ANULAR");
        }
    }

    @Test
    @DisplayName("el cajero no toca el módulo de seguridad ni el de contabilidad")
    void elCajeroNoTocaSeguridadNiContabilidad() {
        assertThat(MatrizRolesDeSistema.porCodigo("CAJERO").permisos())
                .noneMatch(codigo -> codigo.startsWith("SEGURIDAD_") || codigo.startsWith("CONTABILIDAD_"));
    }

    @Test
    @DisplayName("todo permiso de la matriz existe en el catálogo")
    void ningunRolReferenciaUnPermisoInexistente() {
        for (RolDeSistema rol : MatrizRolesDeSistema.ROLES) {
            assertThat(CatalogoPermisos.codigos())
                    .as("permisos del rol %s", rol.codigo())
                    .containsAll(rol.permisos());
        }
    }

    @Test
    @DisplayName("cada rol trae nombre y descripción para mostrar en pantalla")
    void cadaRolEsPresentable() {
        assertThat(MatrizRolesDeSistema.ROLES).allSatisfy(rol -> {
            assertThat(rol.nombre()).isNotBlank();
            assertThat(rol.descripcion()).isNotBlank();
        });
    }

    @Test
    @DisplayName("pedir un rol que no existe es un error explícito")
    void pedirUnRolInexistenteFalla() {
        assertThatThrownBy(() -> MatrizRolesDeSistema.porCodigo("GERENTE_DE_NADA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GERENTE_DE_NADA");
    }

    @Test
    @DisplayName("definir un rol obliga a declarar los seis módulos")
    void definirUnRolExigeLosSeisModulos() {
        assertThatThrownBy(() -> RolDeSistema.desdeMatriz(
                "INCOMPLETO", "Incompleto", "Le falta un módulo",
                Map.of(ModuloErp.SEGURIDAD, "L")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("módulos");
    }

    @Test
    @DisplayName("una letra desconocida en la matriz es un error")
    void unaLetraInvalidaFalla() {
        assertThatThrownBy(() -> RolDeSistema.desdeMatriz(
                "RARO", "Raro", "Usa una letra que no existe",
                Map.of(
                        ModuloErp.SEGURIDAD, "-",
                        ModuloErp.COMERCIAL, "X",
                        ModuloErp.INVENTARIO, "-",
                        ModuloErp.CONTABILIDAD, "-",
                        ModuloErp.FACTURACION, "-",
                        ModuloErp.REPORTES, "-")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X");
    }

    @Test
    @DisplayName("el guion en la matriz significa sin acceso al módulo")
    void elGuionEsSinAcceso() {
        RolDeSistema rol = RolDeSistema.desdeMatriz(
                "SOLO_VENTAS", "Solo ventas", "Prueba de la notación de la matriz",
                Map.of(
                        ModuloErp.SEGURIDAD, "-",
                        ModuloErp.COMERCIAL, "CL",
                        ModuloErp.INVENTARIO, "-",
                        ModuloErp.CONTABILIDAD, "-",
                        ModuloErp.FACTURACION, "-",
                        ModuloErp.REPORTES, "-"));

        assertThat(rol.permisos()).containsExactlyInAnyOrder("COMERCIAL_CREAR", "COMERCIAL_CONSULTAR");
    }

    @Test
    @DisplayName("los permisos de un rol son inmutables")
    void losPermisosSonInmutables() {
        assertThat(MatrizRolesDeSistema.porCodigo("CAJERO").permisos()).isUnmodifiable();
        assertThat(MatrizRolesDeSistema.ROLES).isUnmodifiable();
    }
}
