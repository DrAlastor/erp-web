package com.uagrm.erp.backend.controller;

import com.uagrm.erp.backend.entity.Articulo;
import com.uagrm.erp.backend.service.ArticuloService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;

    @GetMapping
    public ResponseEntity<List<Articulo>> listar(
            @RequestParam(required = false) Long categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(articuloService.listarPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(articuloService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Articulo> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Articulo> crear(@Valid @RequestBody Articulo articulo) {
        Articulo creado = articuloService.crear(articulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Articulo> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Articulo articulo) {
        return ResponseEntity.ok(articuloService.actualizar(id, articulo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        articuloService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Artículo desactivado correctamente"));
    }
}
