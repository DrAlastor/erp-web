package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CU41 - Una herramienta del agente: lo unico que Gemini puede pedir.
 *
 * <p>Gemini solo ve {@code nombre}, {@code descripcion} y {@code parametros}; nunca el SQL. El SQL
 * lo arma la propia herramienta con {@link Consulta} (siempre parametrizado) y lo ejecuta el
 * {@link RegistroHerramientas} en una transaccion de solo lectura. {@code visual} decide con reglas
 * fijas que grafico corresponde al resultado: no lo decide el modelo.
 */
public record Herramienta(
        String nombre,
        String descripcion,
        Map<String, Parametro> parametros,
        List<String> requeridos,
        Function<Argumentos, Visual> visual,
        Ejecutor ejecutor) {

    @FunctionalInterface
    public interface Ejecutor {
        ResultadoHerramienta ejecutar(Argumentos args, ContextoHerramienta contexto);
    }

    public static Constructor de(String nombre, String descripcion) {
        return new Constructor(nombre, descripcion);
    }

    /** Construccion legible de una herramienta. */
    public static final class Constructor {
        private final String nombre;
        private final String descripcion;
        private final Map<String, Parametro> parametros = new LinkedHashMap<>();
        private List<String> requeridos = List.of();
        private Function<Argumentos, Visual> visual = a -> null;

        private Constructor(String nombre, String descripcion) {
            this.nombre = nombre;
            this.descripcion = descripcion;
        }

        public Constructor parametro(String nombre, Parametro p) {
            parametros.put(nombre, p);
            return this;
        }

        public Constructor requeridos(String... nombres) {
            requeridos = List.of(nombres);
            return this;
        }

        public Constructor visual(Function<Argumentos, Visual> v) {
            visual = v;
            return this;
        }

        public Herramienta ejecutar(Ejecutor e) {
            return new Herramienta(nombre, descripcion, Map.copyOf(parametros), requeridos, visual, e);
        }
    }
}
