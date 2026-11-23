package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/** CU41 - Argumentos YA validados por el registro, con lectura tipada y valores por defecto. */
public final class Argumentos {

    private final Map<String, Object> valores;

    public Argumentos(Map<String, Object> valores) {
        this.valores = Collections.unmodifiableMap(valores);
    }

    public Map<String, Object> comoMapa() {
        return valores;
    }

    public String texto(String nombre) {
        Object v = valores.get(nombre);
        return v == null ? null : v.toString();
    }

    public String texto(String nombre, String porDefecto) {
        String v = texto(nombre);
        return v == null ? porDefecto : v;
    }

    public int entero(String nombre, int porDefecto) {
        Object v = valores.get(nombre);
        return v instanceof Number n ? n.intValue() : porDefecto;
    }

    public boolean logico(String nombre, boolean porDefecto) {
        Object v = valores.get(nombre);
        return v instanceof Boolean b ? b : porDefecto;
    }

    public LocalDate fecha(String nombre, LocalDate porDefecto) {
        String v = texto(nombre);
        return v == null ? porDefecto : LocalDate.parse(v);
    }
}
