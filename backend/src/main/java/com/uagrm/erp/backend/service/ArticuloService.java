package com.uagrm.erp.backend.service;

import com.uagrm.erp.backend.entity.Articulo;
import com.uagrm.erp.backend.entity.Categoria;
import com.uagrm.erp.backend.repository.ArticuloRepository;
import com.uagrm.erp.backend.repository.CategoriaRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticuloService {

    private final ArticuloRepository articuloRepository;
    private final CategoriaRepository categoriaRepository;

    public Articulo crear(Articulo articulo) {
        validarPrecio(articulo.getPrecio());

        if (articuloRepository.existsBySku(articulo.getSku())) {
            throw new RuntimeException("SKU ya existe");
        }

        resolverCategoria(articulo);
        return articuloRepository.save(articulo);
    }

    @Transactional(readOnly = true)
    public List<Articulo> listarTodos() {
        return articuloRepository.findAllWithCategoria();
    }

    @Transactional(readOnly = true)
    public Articulo buscarPorId(Long id) {
        return articuloRepository.findByIdWithCategoria(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
    }

    public Articulo actualizar(Long id, Articulo datos) {
        validarPrecio(datos.getPrecio());

        Articulo existente = buscarPorId(id);

        if (datos.getSku() != null
                && articuloRepository.existsBySkuAndIdNot(datos.getSku(), id)) {
            throw new RuntimeException("SKU ya existe");
        }

        if (datos.getSku() != null) {
            existente.setSku(datos.getSku());
        }
        if (datos.getNombre() != null) {
            existente.setNombre(datos.getNombre());
        }
        existente.setDescripcion(datos.getDescripcion());
        if (datos.getPrecio() != null) {
            existente.setPrecio(datos.getPrecio());
        }
        existente.setStock(datos.getStock());
        existente.setImagenUrl(datos.getImagenUrl());
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }

        if (datos.getCategoria() != null) {
            resolverCategoria(datos);
            existente.setCategoria(datos.getCategoria());
        }

        return articuloRepository.save(existente);
    }

    public Articulo eliminar(Long id) {
        Articulo existente = buscarPorId(id);
        existente.setActivo(false);
        return articuloRepository.save(existente);
    }

    @Transactional(readOnly = true)
    public List<Articulo> listarPorCategoria(Long categoriaId) {
        return articuloRepository.findByCategoriaIdWithCategoria(categoriaId);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio inválido");
        }
    }

    private void resolverCategoria(Articulo articulo) {
        if (articulo.getCategoria() == null || articulo.getCategoria().getId() == null) {
            articulo.setCategoria(null);
            return;
        }
        Categoria categoria = categoriaRepository.findById(articulo.getCategoria().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoría no encontrada con id: " + articulo.getCategoria().getId()));
        articulo.setCategoria(categoria);
    }
}
