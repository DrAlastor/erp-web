package modulo.inventario_y_almacenes.catalogo_de_articulos.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import modulo.inventario_y_almacenes.catalogo_de_articulos.dto.ArticuloRequest;
import modulo.inventario_y_almacenes.catalogo_de_articulos.dto.ArticuloResponse;
import modulo.inventario_y_almacenes.catalogo_de_articulos.service.ArticuloService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * HU-05 / CU-08: catálogo maestro de artículos.
 *
 * <p>La protección sale del catálogo de la CU03 con {@code hasPermission} sobre el módulo
 * INVENTARIO: consultar para leer, crear para el alta y modificar para editar o dar de baja.
 */
@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
@Validated
public class ArticuloController {

    private final ArticuloService service;

    @GetMapping
    @PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
    public List<ArticuloResponse> listar(@RequestParam(required = false) Integer categoriaId) {
        if (categoriaId != null) {
            return service.listarPorCategoria(categoriaId);
        }
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
    public ArticuloResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('INVENTARIO','CREAR')")
    public ResponseEntity<ArticuloResponse> crear(@Valid @RequestBody ArticuloRequest cuerpo) {
        ArticuloResponse creado = service.crear(cuerpo);
        return ResponseEntity.created(URI.create("/api/articulos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('INVENTARIO','MODIFICAR')")
    public ArticuloResponse actualizar(@PathVariable Long id, @Valid @RequestBody ArticuloRequest cuerpo) {
        return service.actualizar(id, cuerpo);
    }

    /** Baja lógica del artículo: queda inactivo, no se borra. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('INVENTARIO','MODIFICAR')")
    public ArticuloResponse eliminar(@PathVariable Long id) {
        return service.desactivar(id);
    }
}
