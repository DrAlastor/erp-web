package com.uagrm.erp.backend.seguridad.catalogo;

/**
 * Acciones que un permiso puede habilitar sobre un módulo.
 *
 * <p>La letra es la notación de la matriz del documento de la HU-03: C = crear,
 * L = consultar, M = modificar, A = anular.
 */
public enum AccionErp {

    CREAR('C', "Crear"),
    CONSULTAR('L', "Consultar"),
    MODIFICAR('M', "Modificar"),
    ANULAR('A', "Anular");

    private final char letra;
    private final String etiqueta;

    AccionErp(char letra, String etiqueta) {
        this.letra = letra;
        this.etiqueta = etiqueta;
    }

    /** Letra con la que la matriz del documento representa esta acción. */
    public char letra() {
        return letra;
    }

    /** Nombre de la acción tal como se muestra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }

    /**
     * Traduce una letra de la matriz del documento a su acción.
     *
     * @throws IllegalArgumentException si la letra no corresponde a ninguna acción
     */
    public static AccionErp desdeLetra(char letra) {
        char normalizada = Character.toUpperCase(letra);
        for (AccionErp accion : values()) {
            if (accion.letra == normalizada) {
                return accion;
            }
        }
        throw new IllegalArgumentException(
                "La letra '" + letra + "' no corresponde a ninguna acción de la matriz (C, L, M, A)");
    }
}
