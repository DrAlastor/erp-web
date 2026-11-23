package modulo.inteligencia_de_negocio.cu41_agente_ia.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.ResultadoHerramienta;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.Visual;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** CU41 - Pedidos y respuestas REST del agente de IA. */
public final class AgenteDtos {

    private AgenteDtos() {
    }

    public record PreguntarRequest(
            @NotBlank(message = "La pregunta es obligatoria")
            @Size(max = 2000, message = "La pregunta no puede superar 2000 caracteres") String pregunta,
            @Min(value = 1, message = "Conversacion invalida") Long idConversacion) {
    }

    /** Una herramienta usada en la respuesta: lo que se muestra en "Como se calculo" y los graficos. */
    public record HerramientaUsada(String nombre, Map<String, Object> parametros, boolean ok, String error,
                                   ResultadoHerramienta resultado, Visual grafico) {
    }

    public record Tokens(int entrada, int salida) {
    }

    public record Respuesta(long idConversacion, String texto, List<HerramientaUsada> herramientas, Tokens tokens) {
    }

    public record ConversacionResumen(long id, String titulo, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
    }

    public record Mensaje(long id, String rol, String contenido, JsonNode herramientas, LocalDateTime fecha) {
    }

    public record ConversacionDetalle(long id, String titulo, List<Mensaje> mensajes) {
    }

    public record GuardarGraficoRequest(
            @NotBlank(message = "La herramienta es obligatoria") @Size(max = 60) String herramienta,
            JsonNode parametros,
            @Size(max = 100, message = "El titulo no puede superar 100 caracteres") String titulo) {
    }

    public record Grafico(long id, String titulo, String herramienta, JsonNode parametros, LocalDateTime fechaCreacion) {
    }

    public record GraficoDatos(Grafico grafico, boolean ok, String error, Visual visual, ResultadoHerramienta resultado) {
    }

    public record Estado(boolean configurado, String modelo, int herramientas, int preguntasPorDia, long preguntasHoy) {
    }
}
