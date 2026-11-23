package modulo.inteligencia_de_negocio.cu41_agente_ia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.inteligencia_de_negocio.cu41_agente_ia.dto.AgenteDtos.*;
import modulo.inteligencia_de_negocio.cu41_agente_ia.service.AgenteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CU41 - Agente de IA (chat). JWT + permiso REPORTES / CONSULTAR. La empresa y el usuario salen
 * del token; conversaciones y graficos siempre filtrados por el usuario.
 */
@RestController
@RequestMapping("/api/inteligencia-negocio/agente")
@RequiredArgsConstructor
public class AgenteController {

    private final AgenteService service;

    @GetMapping("/estado")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Estado estado() {
        return service.estado();
    }

    @PostMapping("/preguntar")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Respuesta preguntar(@Valid @RequestBody PreguntarRequest pedido) {
        return service.preguntar(pedido);
    }

    @GetMapping("/conversaciones")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public List<ConversacionResumen> conversaciones() {
        return service.conversaciones();
    }

    @GetMapping("/conversaciones/{id}")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public ConversacionDetalle conversacion(@PathVariable long id) {
        return service.conversacion(id);
    }

    @DeleteMapping("/conversaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public void borrarConversacion(@PathVariable long id) {
        service.borrarConversacion(id);
    }

    @PostMapping("/graficos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Grafico guardarGrafico(@Valid @RequestBody GuardarGraficoRequest pedido) {
        return service.guardarGrafico(pedido);
    }

    @GetMapping("/graficos")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public List<Grafico> graficos() {
        return service.graficos();
    }

    @GetMapping("/graficos/{id}/datos")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public GraficoDatos datosGrafico(@PathVariable long id) {
        return service.datosGrafico(id);
    }

    @DeleteMapping("/graficos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public void borrarGrafico(@PathVariable long id) {
        service.borrarGrafico(id);
    }
}
