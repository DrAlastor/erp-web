package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.MlClienteService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CU41 - Contexto de ejecucion. La empresa sale del token del usuario (nunca de la IA) y
 * {@code consultar} corre dentro de una transaccion READ ONLY con tiempo maximo.
 */
public record ContextoHerramienta(TenantDescriptor empresa, LocalDate hoy, Consultor consultor, MlClienteService ml) {

    @FunctionalInterface
    public interface Consultor {
        List<Map<String, Object>> consultar(Consulta q);
    }

    public List<Map<String, Object>> consultar(Consulta q) {
        return consultor.consultar(q);
    }

    public Map<String, Object> unaFila(Consulta q) {
        List<Map<String, Object>> filas = consultar(q);
        return filas.isEmpty() ? Map.of() : filas.getFirst();
    }
}
