package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CU40 - Inteligencia de Negocio: respuestas REST de los modelos de Machine Learning.
 *
 * <p>Todo es solo lectura sobre las tablas {@code ml_*} que escribe el servicio Python
 * (ml-service). {@code entrenado = false} distingue "los modelos nunca se entrenaron" de
 * "no hay resultados para estos filtros", para que la pantalla no muestre un vacio enganoso.
 */
public final class InteligenciaNegocioDtos {

    private InteligenciaNegocioDtos() {
    }

    public record Ejecucion(long id, String estado, LocalDateTime inicio, LocalDateTime fin, String error) {
    }

    public record ModeloInfo(String clave, String titulo, String tipo, String tecnica, String pregunta,
                             JsonNode metricas) {
    }

    public record Resumen(boolean servicioConfigurado, boolean entrenado, boolean entrenamientoAutomatico,
                          Ejecucion ultimaEjecucion, List<ModeloInfo> modelos) {
    }

    public record EntrenarResponse(boolean encolado, String mensaje) {
    }

    public record Opcion(long id, String nombre) {
    }

    public record Opciones(List<Opcion> sucursales, List<String> categorias) {
    }

    /** Resultados de una seccion con el aviso de por que podria estar vacia. */
    public record Lista<T>(boolean entrenado, String aviso, List<T> items) {
    }

    // ------------------------------------------------------------------ pronostico

    public record PuntoHistoria(LocalDate periodo, BigDecimal valor) {
    }

    public record PuntoPronostico(LocalDate periodo, BigDecimal valor, BigDecimal bajo, BigDecimal alto) {
    }

    public record Pronostico(boolean entrenado, boolean disponible, String aviso, String frecuencia,
                             String ambito, String clave, String modelo, String origen,
                             Double mape, Double mapeIngenuo,
                             List<PuntoHistoria> historia, List<PuntoPronostico> pronostico) {
    }

    // ------------------------------------------------------------------ clientes

    public record ClienteRiesgo(long clienteId, String cliente, String nitCi, BigDecimal probabilidad,
                                String nivel, JsonNode factores) {
    }

    public record ClientesRiesgo(boolean entrenado, String aviso, Map<String, Long> porNivel,
                                 List<ClienteRiesgo> clientes) {
    }

    public record Segmento(String segmento, long clientes, double recenciaPromedio, double frecuenciaPromedio,
                           BigDecimal montoPromedio, BigDecimal montoTotal) {
    }

    public record ClienteSegmento(long clienteId, String cliente, String segmento, int recenciaDias,
                                  int frecuencia, BigDecimal monto) {
    }

    public record Segmentos(boolean entrenado, String aviso, List<Segmento> segmentos,
                            List<ClienteSegmento> clientes) {
    }

    // ------------------------------------------------------------------ productos y sucursales

    public record ReglaAsociacion(long productoAId, String productoA, long productoBId, String productoB,
                                  BigDecimal soporte, BigDecimal confianza, BigDecimal lift, int ventas) {
    }

    public record Anomalia(LocalDate fecha, long sucursalId, String sucursal, String metrica,
                           BigDecimal valorReal, BigDecimal valorEsperado, BigDecimal desviacion,
                           String gravedad) {
    }

    public record SugerenciaCompra(long productoId, String codigo, String producto, String categoria,
                                   long sucursalId, String sucursal, BigDecimal demandaEsperada,
                                   BigDecimal stockActual, BigDecimal stockSeguridad, int cantidadSugerida,
                                   BigDecimal costoEstimado, String motivo) {
    }
}
