package modulo.catalogo_e_inventario.cu12_consultar_kardex.service;

/**
 * Métodos de valuación del Kardex que contempla el alcance del módulo de inventarios:
 * Promedio Ponderado y PEPS (primero en entrar, primero en salir).
 */
public enum MetodoCosteo {
    PROMEDIO("Promedio Ponderado"),
    PEPS("PEPS / FIFO");

    private final String etiqueta;

    MetodoCosteo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
