package modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.service;

import lombok.RequiredArgsConstructor;

import modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.dto.ArticuloRequest;
import modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.dto.ArticuloResponse;
import modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.entity.Articulo;
import modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.mapper.ArticuloMapper;
import modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.repository.ArticuloRepository;
import modulo.catalogo_e_inventario.compartido.entity.Categoria;
import modulo.catalogo_e_inventario.compartido.repository.CategoriaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import comun.saas.TenantEntitlementsService;

/**
 * HU-05: catálogo maestro de artículos.
 *
 * <p>El SKU es único en todo el catálogo y la categoría es la compartida del módulo de
 * inventario. La baja es lógica (el artículo queda inactivo) para no romper los movimientos
 * de Kardex que lo referencian.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticuloService {

    private final ArticuloRepository articulos;
    private final CategoriaRepository categorias;
    private final ArticuloMapper mapper;
    private final TenantEntitlementsService entitlements;
    private final modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService bitacoraAuditoriaService;

    public List<ArticuloResponse> listarTodos() {
        return articulos.findAllWithCategoria().stream().map(mapper::toResponse).toList();
    }

    public List<ArticuloResponse> listarPorCategoria(Integer categoriaId) {
        return articulos.findByCategoriaIdWithCategoria(categoriaId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ArticuloResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarEntidad(id));
    }

    @Transactional
    public ArticuloResponse crear(ArticuloRequest request) {
        entitlements.requireCapacity("PRODUCTOS", articulos.count());
        String sku = request.sku().trim();
        if (articulos.existsBySku(sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU " + sku + " ya existe");
        }

        Articulo articulo = mapper.toEntity(request);
        articulo.setCategoria(resolverCategoria(request.categoriaId()));
        Articulo guardado = articulos.save(articulo);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "CREAR_ARTICULO",
                    "CATALOGO",
                    String.format("Creación de nuevo artículo '%s' con SKU '%s' en categoría '%s'.",
                            guardado.getNombre(), guardado.getSku(),
                            guardado.getCategoria() != null ? guardado.getCategoria().getNombre() : "Sin Categoría"),
                    "EXITO",
                    "MEDIA",
                    String.format("{\"articuloId\":%d,\"sku\":\"%s\",\"nombre\":\"%s\"}",
                            guardado.getId(), guardado.getSku(), guardado.getNombre())
            );
        } catch (Exception ignored) {}

        return mapper.toResponse(guardado);
    }

    @Transactional
    public ArticuloResponse actualizar(Long id, ArticuloRequest request) {
        Articulo articulo = buscarEntidad(id);
        String sku = request.sku().trim();
        if (articulos.existsBySkuAndIdNot(sku, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU " + sku + " ya existe");
        }

        mapper.aplicar(articulo, request);
        articulo.setCategoria(resolverCategoria(request.categoriaId()));
        Articulo guardado = articulos.save(articulo);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "MODIFICAR_ARTICULO",
                    "CATALOGO",
                    String.format("Modificación del artículo #%d ('%s', SKU: '%s').",
                            guardado.getId(), guardado.getNombre(), guardado.getSku()),
                    "EXITO",
                    "MEDIA",
                    String.format("{\"articuloId\":%d,\"sku\":\"%s\",\"nombre\":\"%s\"}",
                            guardado.getId(), guardado.getSku(), guardado.getNombre())
            );
        } catch (Exception ignored) {}

        return mapper.toResponse(guardado);
    }

    /** Baja lógica: desactiva el artículo sin borrarlo. */
    @Transactional
    public ArticuloResponse desactivar(Long id) {
        Articulo articulo = buscarEntidad(id);
        articulo.setActivo(false);
        Articulo guardado = articulos.save(articulo);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "DESACTIVAR_ARTICULO",
                    "CATALOGO",
                    String.format("Baja lógica del artículo #%d ('%s', SKU: '%s').",
                            guardado.getId(), guardado.getNombre(), guardado.getSku()),
                    "EXITO",
                    "ALTA",
                    String.format("{\"articuloId\":%d,\"sku\":\"%s\"}",
                            guardado.getId(), guardado.getSku())
            );
        } catch (Exception ignored) {}

        return mapper.toResponse(guardado);
    }

    private Articulo buscarEntidad(Long id) {
        return articulos.findByIdWithCategoria(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Artículo no encontrado con id: " + id));
    }

    private Categoria resolverCategoria(Integer categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categorias.findById(categoriaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Categoría no encontrada con id: " + categoriaId));
    }
}
