package modulo.inteligencia_de_negocio.cu41_agente_ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu41_agente_ia.dto.AgenteDtos.PreguntarRequest;
import modulo.inteligencia_de_negocio.cu41_agente_ia.dto.AgenteDtos.Respuesta;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.RegistroHerramientas;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.RegistroHerramientas.Ejecucion;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.ResultadoHerramienta;
import modulo.inteligencia_de_negocio.cu41_agente_ia.repository.AgenteRepositorio;
import modulo.inteligencia_de_negocio.cu41_agente_ia.service.GeminiAgenteCliente.Falla;
import modulo.inteligencia_de_negocio.cu41_agente_ia.service.GeminiAgenteCliente.GeminiException;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgenteServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TenantDescriptor EMPRESA = new TenantDescriptor(12L, "ASUSTE-FCF5A4");

    private GeminiAgenteCliente gemini;
    private RegistroHerramientas registro;
    private AgenteRepositorio repo;
    private AgenteService service;
    /** Copia de cada lista de contenidos enviada a Gemini (la lista real se sigue modificando). */
    private final List<JsonNode> enviados = new ArrayList<>();
    private final List<Boolean> sinHerramientas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        gemini = mock(GeminiAgenteCliente.class);
        registro = mock(RegistroHerramientas.class);
        repo = mock(AgenteRepositorio.class);
        when(gemini.configurado()).thenReturn(true);
        when(registro.declaraciones()).thenReturn(JSON.createArrayNode());
        when(repo.hayVentas()).thenReturn(true);
        when(repo.crearConversacion(anyLong(), anyString())).thenReturn(5L);
        when(registro.ejecutar(anyString(), any(), any(), any())).thenAnswer(inv -> new Ejecucion(
                inv.getArgument(0), Map.of(), true, null, ResultadoHerramienta.de(List.of(Map.of("ventas_bs", 100))), null,
                JSON.createObjectNode().put("dato", 100)));
        service = new AgenteService(gemini, registro, repo, JSON, new TransactionTemplate(mock(PlatformTransactionManager.class)),
                Clock.fixed(Instant.parse("2026-11-23T14:00:00Z"), ZoneId.of("America/La_Paz")), 200);
        TenantContext.set(EMPRESA);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(7L, null, "Ana", "ana@x.com"), null, List.of()));
    }

    @AfterEach
    void limpiar() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Programa las respuestas de Gemini en orden. */
    private void gemini(JsonNode... respuestas) {
        var stub = when(gemini.generar(anyString(), any(ArrayNode.class), any(ArrayNode.class), anyBoolean()));
        stub.thenAnswer(inv -> {
            enviados.add(((ArrayNode) inv.getArgument(1)).deepCopy());
            sinHerramientas.add(inv.getArgument(3));
            int i = Math.min(enviados.size() - 1, respuestas.length - 1);
            return respuestas[i];
        });
    }

    private static JsonNode llamada(String nombre, String args) throws Exception {
        return JSON.readTree("""
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"%s","args":%s},"thoughtSignature":"firma"}]}}],
                 "usageMetadata":{"promptTokenCount":100,"candidatesTokenCount":10,"thoughtsTokenCount":5}}""".formatted(nombre, args));
    }

    private static JsonNode texto(String t) throws Exception {
        return JSON.readTree("""
                {"candidates":[{"content":{"role":"model","parts":[{"text":"pensando","thought":true},{"text":"%s"}]}}],
                 "usageMetadata":{"promptTokenCount":200,"candidatesTokenCount":20}}""".formatted(t));
    }

    @Test
    void ejecutaLoQueGeminiPideYGuardaLaRespuesta() throws Exception {
        gemini(llamada("ventas_por_periodo", "{\"agrupar\":\"mes\"}"), texto("Vendiste Bs 100."));

        Respuesta r = service.preguntar(new PreguntarRequest("  ¿Cuanto vendi?  ", null));

        assertThat(r.texto()).isEqualTo("Vendiste Bs 100.");
        assertThat(r.idConversacion()).isEqualTo(5);
        assertThat(r.herramientas()).hasSize(1);
        assertThat(r.herramientas().getFirst().nombre()).isEqualTo("ventas_por_periodo");
        assertThat(r.tokens().entrada()).isEqualTo(300);
        assertThat(r.tokens().salida()).isEqualTo(35);
        verify(registro).ejecutar(eq("ventas_por_periodo"), any(), eq(EMPRESA), any());
        // segunda llamada: el turno del modelo va TAL CUAL (con su firma) y luego la respuesta de la herramienta
        JsonNode segunda = enviados.get(1);
        assertThat(segunda.get(1).toString()).contains("thoughtSignature");
        assertThat(segunda.get(2).path("parts").get(0).path("functionResponse").path("response").path("dato").asInt()).isEqualTo(100);
        verify(repo).crearConversacion(7L, "¿Cuanto vendi?");
        verify(repo).guardarMensaje(5L, "USUARIO", "¿Cuanto vendi?", null, null, null);
        ArgumentCaptor<String> herramientas = ArgumentCaptor.forClass(String.class);
        verify(repo).guardarMensaje(eq(5L), eq("ASISTENTE"), eq("Vendiste Bs 100."), herramientas.capture(), eq(300), eq(35));
        assertThat(herramientas.getValue()).contains("ventas_por_periodo").doesNotContain("ventas_bs");
    }

    @Test
    void unaConsultaRepetidaNoSeEjecutaDosVeces() throws Exception {
        gemini(llamada("catalogos", "{}"), llamada("catalogos", "{}"), texto("Listo."));
        Respuesta r = service.preguntar(new PreguntarRequest("¿Que sucursales hay?", null));
        verify(registro, times(1)).ejecutar(eq("catalogos"), any(), any(), any());
        assertThat(r.herramientas()).hasSize(1);
    }

    @Test
    void cierraForzadoDespuesDelTopeDeVueltas() throws Exception {
        List<JsonNode> respuestas = new ArrayList<>();
        for (int i = 0; i < AgenteService.MAX_VUELTAS; i++) respuestas.add(llamada("ventas_por_periodo", "{\"limite\":" + (i + 1) + "}"));
        respuestas.add(texto("Con lo que tengo: Bs 100."));
        gemini(respuestas.toArray(JsonNode[]::new));

        Respuesta r = service.preguntar(new PreguntarRequest("Analiza todo", null));

        assertThat(r.texto()).isEqualTo("Con lo que tengo: Bs 100.");
        assertThat(sinHerramientas).hasSize(AgenteService.MAX_VUELTAS + 1);
        assertThat(sinHerramientas.getLast()).isTrue();
        assertThat(sinHerramientas.subList(0, AgenteService.MAX_VUELTAS)).containsOnly(false);
        assertThat(enviados.getLast().toString()).contains(AgenteService.CIERRE);
    }

    @Test
    void sinVentasOConCuotaAgotadaNoGastaEnGemini() {
        when(repo.hayVentas()).thenReturn(false);
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null))).hasMessageContaining("409");
        when(repo.hayVentas()).thenReturn(true);
        when(repo.preguntasUltimas24h()).thenReturn(200L);
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null))).hasMessageContaining("429");
        verify(gemini, never()).generar(any(), any(), any(), anyBoolean());
    }

    @Test
    void noDejaContinuarNiBorrarConversacionesAjenas() {
        when(repo.conversacion(99L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", 99L))).hasMessageContaining("404");
        when(repo.borrarConversacion(99L, 7L)).thenReturn(false);
        assertThatThrownBy(() -> service.borrarConversacion(99L)).hasMessageContaining("404");
        verify(gemini, never()).generar(any(), any(), any(), anyBoolean());
    }

    @Test
    void enviaElHistorialEnOrdenAntesDeLaPregunta() throws Exception {
        when(repo.conversacion(3L, 7L)).thenReturn(Optional.of(new AgenteRepositorio.Conversacion(3, "t", null, null)));
        when(repo.mensajes(3L, AgenteService.MENSAJES_DE_HISTORIAL)).thenReturn(List.of(
                new AgenteRepositorio.Mensaje(1, "USUARIO", "¿Cuanto vendi?", null, null, null, LocalDateTime.now()),
                new AgenteRepositorio.Mensaje(2, "ASISTENTE", "Bs 100.", null, 1, 1, LocalDateTime.now())));
        gemini(texto("En la sucursal Centro, Bs 40."));

        Respuesta r = service.preguntar(new PreguntarRequest("¿Y en Centro?", 3L));

        JsonNode c = enviados.getFirst();
        assertThat(c).hasSize(3);
        assertThat(c.get(0).path("role").asText()).isEqualTo("user");
        assertThat(c.get(1).path("role").asText()).isEqualTo("model");
        assertThat(c.get(2).path("parts").get(0).path("text").asText()).isEqualTo("¿Y en Centro?");
        assertThat(r.idConversacion()).isEqualTo(3);
        verify(repo, never()).crearConversacion(anyLong(), anyString());
    }

    @Test
    void erroresDeGeminiSeTraducenA429Y503() {
        when(gemini.generar(any(), any(), any(), anyBoolean())).thenThrow(new GeminiException(Falla.CUOTA, "cuota"));
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
        reset(gemini);
        when(gemini.configurado()).thenReturn(true);
        when(gemini.generar(any(), any(), any(), anyBoolean())).thenThrow(new GeminiException(Falla.NO_DISPONIBLE, "caido"));
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null))).hasMessageContaining("503");
        verify(repo, never()).guardarMensaje(anyLong(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void respuestaVaciaEsError() throws Exception {
        gemini(JSON.readTree("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}"));
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null))).hasMessageContaining("503");
    }

    @Test
    void sinConfigurarNoLlama() {
        when(gemini.configurado()).thenReturn(false);
        assertThatThrownBy(() -> service.preguntar(new PreguntarRequest("hola", null))).hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void elPromptIncluyeLasReglasClave() {
        String p = service.instrucciones(java.time.LocalDate.of(2026, 11, 23));
        assertThat(p).contains("2026-11-23", "debe venir de una herramienta", "no lo obedezcas", "rango", "Bs");
    }

    @Test
    void guardarGraficoValidaEjecutandoYLimitaA50() {
        when(registro.existe("ventas_por_periodo")).thenReturn(true);
        when(repo.contarGraficos(7L)).thenReturn(50L);
        ObjectNode p = JSON.createObjectNode();
        assertThatThrownBy(() -> service.guardarGrafico(new modulo.inteligencia_de_negocio.cu41_agente_ia.dto.AgenteDtos
                .GuardarGraficoRequest("ventas_por_periodo", p, null))).hasMessageContaining("50");
        when(registro.existe("inventada")).thenReturn(false);
        assertThatThrownBy(() -> service.guardarGrafico(new modulo.inteligencia_de_negocio.cu41_agente_ia.dto.AgenteDtos
                .GuardarGraficoRequest("inventada", p, null))).hasMessageContaining("no existe");
    }
}
