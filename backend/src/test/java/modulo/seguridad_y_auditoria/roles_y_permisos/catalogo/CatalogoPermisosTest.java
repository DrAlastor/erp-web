package modulo.seguridad_y_auditoria.roles_y_permisos.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El catálogo de permisos es el vocabulario de la autorización: si acá falta o sobra
 * un código, la matriz de roles del documento de la HU-03 deja de ser representable.
 */
class CatalogoPermisosTest {

    @Test
    @DisplayName("el catálogo tiene 24 permisos: 6 módulos por 4 acciones")
    void tieneVeinticuatroPermisos() {
        assertThat(ModuloErp.values()).hasSize(6);
        assertThat(AccionErp.values()).hasSize(4);
        assertThat(CatalogoPermisos.TODOS).hasSize(24);
    }

    @Test
    @DisplayName("los módulos son los seis de la matriz del documento")
    void losModulosSonLosDelDocumento() {
        assertThat(ModuloErp.values())
                .extracting(Enum::name)
                .containsExactly("SEGURIDAD", "COMERCIAL", "INVENTARIO", "CONTABILIDAD", "FACTURACION", "REPORTES");
    }

    @Test
    @DisplayName("las acciones son crear, consultar, modificar y anular")
    void lasAccionesSonLasDelDocumento() {
        assertThat(AccionErp.values())
                .extracting(Enum::name)
                .containsExactly("CREAR", "CONSULTAR", "MODIFICAR", "ANULAR");
    }

    @Test
    @DisplayName("el código de cada permiso es MODULO_ACCION")
    void elCodigoEsModuloGuionBajoAccion() {
        for (DefinicionPermiso permiso : CatalogoPermisos.TODOS) {
            assertThat(permiso.codigo())
                    .isEqualTo(permiso.modulo().name() + "_" + permiso.accion().name());
        }
    }

    @Test
    @DisplayName("no hay códigos repetidos")
    void losCodigosSonUnicos() {
        Set<String> codigos = CatalogoPermisos.TODOS.stream()
                .map(DefinicionPermiso::codigo)
                .collect(Collectors.toSet());

        assertThat(codigos).hasSize(CatalogoPermisos.TODOS.size());
    }

    @Test
    @DisplayName("está cubierta toda combinación de módulo y acción")
    void cubreTodasLasCombinaciones() {
        Set<String> esperadas = new HashSet<>();
        for (ModuloErp modulo : ModuloErp.values()) {
            for (AccionErp accion : AccionErp.values()) {
                esperadas.add(modulo + "_" + accion);
            }
        }

        assertThat(CatalogoPermisos.codigos()).isEqualTo(esperadas);
    }

    @Test
    @DisplayName("cada permiso trae una descripción legible")
    void cadaPermisoTieneDescripcion() {
        assertThat(CatalogoPermisos.TODOS)
                .allSatisfy(permiso -> assertThat(permiso.descripcion()).isNotBlank());
    }

    @Test
    @DisplayName("el ejemplo VENTA_ANULAR del documento corresponde a COMERCIAL_ANULAR")
    void elAnularVentaDelDocumentoEsComercialAnular() {
        assertThat(CatalogoPermisos.codigo(ModuloErp.COMERCIAL, AccionErp.ANULAR))
                .isEqualTo("COMERCIAL_ANULAR");
        assertThat(CatalogoPermisos.codigos()).contains("COMERCIAL_ANULAR");
    }

    @Test
    @DisplayName("el catálogo es inmutable")
    void elCatalogoEsInmutable() {
        assertThat(CatalogoPermisos.TODOS).isUnmodifiable();
    }
}
