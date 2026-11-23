package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.util.List;

/** CU41 - Esquema de un parametro de herramienta (lo que Gemini ve y lo que el registro valida). */
public record Parametro(Tipo tipo, String descripcion, List<String> enumeracion, boolean esFecha,
                        Double minimo, Double maximo) {

    public enum Tipo { STRING, INTEGER, NUMBER, BOOLEAN }

    public static Parametro texto(String descripcion) {
        return new Parametro(Tipo.STRING, descripcion, null, false, null, null);
    }

    public static Parametro opciones(String descripcion, String... valores) {
        return new Parametro(Tipo.STRING, descripcion, List.of(valores), false, null, null);
    }

    public static Parametro fecha(String descripcion) {
        return new Parametro(Tipo.STRING, descripcion + " Formato AAAA-MM-DD.", null, true, null, null);
    }

    public static Parametro entero(String descripcion, int minimo, int maximo) {
        return new Parametro(Tipo.INTEGER, descripcion, null, false, (double) minimo, (double) maximo);
    }

    public static Parametro logico(String descripcion) {
        return new Parametro(Tipo.BOOLEAN, descripcion, null, false, null, null);
    }

    // Parametros reutilizables
    public static final Parametro DESDE = fecha("Inicio del periodo (inclusive). Si se omite: hace 12 meses.");
    public static final Parametro HASTA = fecha("Fin del periodo (inclusive). Si se omite: hoy.");
    public static final Parametro SUCURSAL = texto("Nombre (o parte del nombre) de la sucursal. Omitir para todas.");
    public static final Parametro CATEGORIA = texto("Nombre (o parte) de la categoria de producto. Omitir para todas.");
    public static final Parametro PRODUCTO = texto("Nombre (o parte) del producto. Omitir para todos.");
    public static final Parametro LIMITE = entero("Cantidad maxima de filas (1 a 100).", 1, 100);
}
