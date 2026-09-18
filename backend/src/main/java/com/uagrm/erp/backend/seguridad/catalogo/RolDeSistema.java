package com.uagrm.erp.backend.seguridad.catalogo;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Definición de uno de los roles de sistema del ERP con su conjunto de permisos.
 *
 * <p>Se construye con {@link #desdeMatriz} usando la misma notación del documento de la
 * HU-03 —{@code "C L M A"} para todos los permisos de un módulo, {@code "L"} para solo
 * consultar, {@code "-"} para sin acceso— de modo que la definición en el código se pueda
 * leer al lado de la tabla del documento y compararlas de un vistazo.
 */
public record RolDeSistema(String codigo, String nombre, String descripcion, Set<String> permisos) {

    /** Marcas que la matriz del documento usa para "sin acceso al módulo". */
    private static final String SIN_ACCESO = "-–—";

    public RolDeSistema {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del rol es obligatorio");
        }
        permisos = Set.copyOf(permisos);
    }

    /**
     * Construye un rol a partir de una fila de la matriz de permisos.
     *
     * @param matriz una entrada por cada módulo del ERP, con las letras de las acciones
     *               habilitadas ({@code C}, {@code L}, {@code M}, {@code A}) o un guion
     * @throws IllegalArgumentException si falta declarar algún módulo o si aparece una letra desconocida
     */
    public static RolDeSistema desdeMatriz(String codigo, String nombre, String descripcion,
                                           Map<ModuloErp, String> matriz) {
        Set<ModuloErp> declarados = matriz.keySet();
        Set<ModuloErp> faltantes = EnumSet.allOf(ModuloErp.class);
        faltantes.removeAll(declarados);
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "La matriz del rol " + codigo + " debe declarar los seis módulos del ERP; faltan: " + faltantes);
        }

        Set<String> permisos = new LinkedHashSet<>();
        for (ModuloErp modulo : ModuloErp.values()) {
            permisos.addAll(permisosDeLaCelda(modulo, matriz.get(modulo)));
        }
        return new RolDeSistema(codigo, nombre, descripcion, permisos);
    }

    private static Set<String> permisosDeLaCelda(ModuloErp modulo, String celda) {
        Set<String> permisos = new LinkedHashSet<>();
        if (celda == null) {
            return permisos;
        }
        for (char letra : celda.toCharArray()) {
            if (Character.isWhitespace(letra) || SIN_ACCESO.indexOf(letra) >= 0) {
                continue;
            }
            permisos.add(CatalogoPermisos.codigo(modulo, AccionErp.desdeLetra(letra)));
        }
        return permisos;
    }
}
