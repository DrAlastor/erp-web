package com.uagrm.erp.backend.seguridad.catalogo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Catálogo de los permisos del ERP: los 24 que salen de cruzar los seis módulos con las
 * cuatro acciones de la matriz de la HU-03.
 *
 * <p>Se genera a partir de los enumerados en lugar de escribirse a mano: así es imposible
 * que falte una combinación o que se cuele un código mal tipeado. El código de cada
 * permiso es {@code MODULO_ACCION}, por ejemplo {@code COMERCIAL_ANULAR}.
 */
public final class CatalogoPermisos {

    /** Los 24 permisos, en orden de módulo y después de acción. */
    public static final List<DefinicionPermiso> TODOS = construirCatalogo();

    private static final Set<String> CODIGOS = construirCodigos();

    private CatalogoPermisos() {
    }

    /** Código de permiso para un par módulo-acción. */
    public static String codigo(ModuloErp modulo, AccionErp accion) {
        return modulo.name() + "_" + accion.name();
    }

    /** Todos los códigos del catálogo. */
    public static Set<String> codigos() {
        return CODIGOS;
    }

    /** Indica si un código corresponde a un permiso del catálogo. */
    public static boolean existe(String codigo) {
        return CODIGOS.contains(codigo);
    }

    private static List<DefinicionPermiso> construirCatalogo() {
        List<DefinicionPermiso> permisos = new ArrayList<>();
        for (ModuloErp modulo : ModuloErp.values()) {
            for (AccionErp accion : AccionErp.values()) {
                permisos.add(new DefinicionPermiso(
                        codigo(modulo, accion),
                        modulo,
                        accion,
                        accion.etiqueta() + " en " + modulo.etiqueta()));
            }
        }
        return List.copyOf(permisos);
    }

    private static Set<String> construirCodigos() {
        Set<String> codigos = new LinkedHashSet<>();
        for (DefinicionPermiso permiso : TODOS) {
            codigos.add(permiso.codigo());
        }
        return Set.copyOf(codigos);
    }
}
