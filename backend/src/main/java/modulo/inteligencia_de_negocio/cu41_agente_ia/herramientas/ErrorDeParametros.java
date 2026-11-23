package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

/** CU41 - Parametro invalido: vuelve a Gemini como resultado para que corrija el pedido. */
public class ErrorDeParametros extends RuntimeException {
    public ErrorDeParametros(String mensaje) {
        super(mensaje);
    }
}
