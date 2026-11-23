package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.controller;

import lombok.RequiredArgsConstructor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.*;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.InteligenciaNegocioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * CU40 - Inteligencia de Negocio (Machine Learning).
 *
 * <p>Lectura con REPORTES / CONSULTAR; entrenar con REPORTES / MODIFICAR. La empresa sale del
 * token (TenantContext), nunca de un parametro.
 */
@RestController
@RequestMapping("/api/inteligencia-negocio")
@RequiredArgsConstructor
public class InteligenciaNegocioController {

    private final InteligenciaNegocioService service;

    @GetMapping("/resumen")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Resumen resumen() {
        return service.resumen();
    }

    @PostMapping("/entrenar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasPermission('REPORTES','MODIFICAR')")
    public EntrenarResponse entrenar() {
        return service.entrenar();
    }

    @GetMapping("/opciones")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Opciones opciones() {
        return service.opciones();
    }

    @GetMapping("/pronostico")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Pronostico pronostico(@RequestParam(required = false) String frecuencia,
                                 @RequestParam(required = false) Long sucursalId,
                                 @RequestParam(required = false) String categoria,
                                 @RequestParam(required = false) Integer horizonte) {
        return service.pronostico(frecuencia, sucursalId, categoria, horizonte);
    }

    @GetMapping("/clientes-riesgo")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public ClientesRiesgo clientesRiesgo(@RequestParam(required = false) String nivel,
                                         @RequestParam(required = false) Integer limite) {
        return service.clientesEnRiesgo(nivel, limite);
    }

    @GetMapping("/segmentos")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Segmentos segmentos(@RequestParam(required = false) String segmento,
                               @RequestParam(required = false) Integer limite) {
        return service.segmentos(segmento, limite);
    }

    @GetMapping("/canasta")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Lista<ReglaAsociacion> canasta(@RequestParam(required = false) String producto,
                                          @RequestParam(required = false) Integer limite) {
        return service.canasta(producto, limite);
    }

    @GetMapping("/anomalias")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Lista<Anomalia> anomalias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) Integer limite) {
        return service.anomalias(desde, hasta, sucursalId, limite);
    }

    @GetMapping("/sugerencia-compra")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public Lista<SugerenciaCompra> sugerenciaCompra(@RequestParam(required = false) Long sucursalId,
                                                    @RequestParam(required = false) String categoria,
                                                    @RequestParam(required = false) Integer limite) {
        return service.sugerenciaCompra(sucursalId, categoria, limite);
    }
}
