package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU41 - Lo que devuelve una herramienta.
 *
 * @param filas            datos
 * @param resumen          totales o contexto (periodo analizado, etc.)
 * @param nota             aclaracion para que la IA no interprete de mas
 * @param disponible       false: el dato todavia no existe (p. ej. modelos sin entrenar)
 * @param columnasPrivadas columnas que se muestran en pantalla pero NO se envian a Google
 */
public record ResultadoHerramienta(List<Map<String, Object>> filas, Map<String, Object> resumen, String nota,
                                   Boolean disponible, List<String> columnasPrivadas) {

    public static ResultadoHerramienta de(List<Map<String, Object>> filas) {
        return new ResultadoHerramienta(filas, new LinkedHashMap<>(), null, null, List.of());
    }

    public ResultadoHerramienta conResumen(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>(resumen == null ? Map.of() : resumen);
        m.putAll(r);
        return new ResultadoHerramienta(filas, m, nota, disponible, columnasPrivadas);
    }

    public ResultadoHerramienta conNota(String n) {
        return new ResultadoHerramienta(filas, resumen, n, disponible, columnasPrivadas);
    }

    public ResultadoHerramienta noDisponible(String n) {
        return new ResultadoHerramienta(filas, resumen, n, false, columnasPrivadas);
    }

    public ResultadoHerramienta privadas(String... columnas) {
        return new ResultadoHerramienta(filas, resumen, nota, disponible, List.of(columnas));
    }
}
