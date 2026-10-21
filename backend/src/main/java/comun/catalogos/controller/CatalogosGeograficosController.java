package comun.catalogos.controller;

import comun.catalogos.dto.MonedaCatalogoResponse;
import comun.catalogos.dto.PaisCatalogoResponse;
import comun.catalogos.service.CatalogosGeograficosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/catalogos")
public class CatalogosGeograficosController {

    private final CatalogosGeograficosService service;

    public CatalogosGeograficosController(CatalogosGeograficosService service) {
        this.service = service;
    }

    @GetMapping("/paises")
    public List<PaisCatalogoResponse> listarPaises() {
        return service.listarPaises();
    }

    @GetMapping("/monedas")
    public List<MonedaCatalogoResponse> listarMonedas() {
        return service.listarMonedas();
    }

    @GetMapping("/paises/{codigoIso2}")
    public PaisCatalogoResponse buscarPais(@PathVariable String codigoIso2) {
        return service.buscarPais(codigoIso2);
    }
}
