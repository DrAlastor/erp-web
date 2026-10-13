package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.catalogo_e_inventario.compartido.entity.Categoria;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarCategoriaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CategoriaDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CategoriaNodoDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearCategoriaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.repository.CategoriaMaestroRepository;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaMaestroRepository categoriaRepository;
    private final BitacoraLogRepository bitacoraLogRepository;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarCategorias(UUID empresaId) {
        List<Categoria> categorias = empresaId != null
                ? categoriaRepository.findByEmpresaIdOrEmpresaIdIsNullOrderByNombreAsc(empresaId)
                : categoriaRepository.findAllByOrderByNombreAsc();

        Map<Integer, String> nombresMap = categorias.stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre, (a, b) -> a));

        Map<Integer, Long> articulosCountMap = contarArticulosPorCategoriaSeguro();
        Map<Integer, Long> subcategoriasCountMap = categorias.stream()
                .filter(c -> c.getPadreId() != null)
                .collect(Collectors.groupingBy(Categoria::getPadreId, Collectors.counting()));

        return categorias.stream()
                .map(c -> CategoriaDTO.builder()
                        .id(c.getId())
                        .nombre(c.getNombre())
                        .descripcion(c.getDescripcion())
                        .padreId(c.getPadreId())
                        .padreNombre(c.getPadreId() != null ? nombresMap.getOrDefault(c.getPadreId(), "Categoría #" + c.getPadreId()) : null)
                        .activo(Boolean.TRUE.equals(c.getActivo()))
                        .articulosCount(articulosCountMap.getOrDefault(c.getId(), 0L))
                        .subcategoriasCount(subcategoriasCountMap.getOrDefault(c.getId(), 0L))
                        .fechaCreacion(c.getFechaCreacion() != null ? c.getFechaCreacion() : LocalDateTime.now())
                        .build())
                .toList();
    }

    /**
     * Procesa la consulta recursiva de categorías para entregar el árbol anidado JSON en una sola petición optimizada.
     */
    @Transactional(readOnly = true)
    public List<CategoriaNodoDTO> obtenerArbol(UUID empresaId) {
        List<Categoria> todas = empresaId != null
                ? categoriaRepository.findByEmpresaIdOrEmpresaIdIsNullOrderByNombreAsc(empresaId)
                : categoriaRepository.findAllByOrderByNombreAsc();

        Map<Integer, Long> articulosCountMap = contarArticulosPorCategoriaSeguro();

        // 1. Mapear cada categoría a su nodo DTO
        Map<Integer, CategoriaNodoDTO> nodosMap = new HashMap<>();
        for (Categoria c : todas) {
            CategoriaNodoDTO nodo = CategoriaNodoDTO.builder()
                    .id(c.getId())
                    .nombre(c.getNombre())
                    .descripcion(c.getDescripcion())
                    .padreId(c.getPadreId())
                    .activo(Boolean.TRUE.equals(c.getActivo()))
                    .articulosCount(articulosCountMap.getOrDefault(c.getId(), 0L))
                    .fechaCreacion(c.getFechaCreacion() != null ? c.getFechaCreacion() : LocalDateTime.now())
                    .subcategorias(new ArrayList<>())
                    .build();
            nodosMap.put(c.getId(), nodo);
        }

        // 2. Construir la estructura de árbol
        List<CategoriaNodoDTO> raices = new ArrayList<>();
        for (Categoria c : todas) {
            CategoriaNodoDTO actual = nodosMap.get(c.getId());
            if (c.getPadreId() == null) {
                raices.add(actual);
            } else {
                CategoriaNodoDTO padre = nodosMap.get(c.getPadreId());
                if (padre != null) {
                    padre.getSubcategorias().add(actual);
                } else {
                    raices.add(actual);
                }
            }
        }

        return raices;
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtenerCategoria(Integer id, UUID empresaId) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada con ID: " + id));

        String padreNombre = null;
        if (categoria.getPadreId() != null) {
            padreNombre = categoriaRepository.findById(categoria.getPadreId())
                    .map(Categoria::getNombre)
                    .orElse(null);
        }

        long subcatCount = categoriaRepository.countByPadreId(id);
        long articulosCount = contarArticulosDeCategoriaSeguro(id);

        return CategoriaDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .padreId(categoria.getPadreId())
                .padreNombre(padreNombre)
                .activo(Boolean.TRUE.equals(categoria.getActivo()))
                .articulosCount(articulosCount)
                .subcategoriasCount(subcatCount)
                .fechaCreacion(categoria.getFechaCreacion())
                .build();
    }

    @Transactional
    public CategoriaDTO crearCategoria(CrearCategoriaRequest req, UUID empresaId, String actor, String ip) {
        String nombre = req.getNombre().trim();

        // 1. Validar unicidad de nombre
        boolean nombreExiste = empresaId != null
                ? categoriaRepository.existsByNombreIgnoreCaseAndEmpresaId(nombre, empresaId)
                : categoriaRepository.existsByNombreIgnoreCase(nombre);

        if (nombreExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con el nombre '" + nombre + "'.");
        }

        // 2. Validar padre si fue especificado
        String padreNombre = null;
        if (req.getPadreId() != null) {
            Categoria padre = categoriaRepository.findById(req.getPadreId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría superior especificada no existe."));
            padreNombre = padre.getNombre();
        }

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(req.getDescripcion() != null ? req.getDescripcion().trim() : null)
                .padreId(req.getPadreId())
                .empresaId(empresaId)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Categoria guardada = categoriaRepository.save(categoria);
        registrarBitacora("CREAR_CATEGORIA", actor, guardada.getId().longValue(), ip,
                "Creada categoría: " + guardada.getNombre() + (padreNombre != null ? " bajo " + padreNombre : " (Raíz)"));

        return CategoriaDTO.builder()
                .id(guardada.getId())
                .nombre(guardada.getNombre())
                .descripcion(guardada.getDescripcion())
                .padreId(guardada.getPadreId())
                .padreNombre(padreNombre)
                .activo(true)
                .articulosCount(0L)
                .subcategoriasCount(0L)
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional
    public CategoriaDTO actualizarCategoria(Integer id, ActualizarCategoriaRequest req, UUID empresaId, String actor, String ip) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada con ID: " + id));

        String nombre = req.getNombre().trim();

        // 1. Validar unicidad de nombre
        boolean nombreExisteOtro = empresaId != null
                ? categoriaRepository.existsByNombreIgnoreCaseAndEmpresaIdAndIdNot(nombre, empresaId, id)
                : categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);

        if (nombreExisteOtro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra categoría con el nombre '" + nombre + "'.");
        }

        // 2. Validar ciclo recursivo
        String padreNombre = null;
        if (req.getPadreId() != null) {
            if (req.getPadreId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una categoría no puede tenerse a sí misma como categoría superior.");
            }

            Categoria padre = categoriaRepository.findById(req.getPadreId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría superior especificada no existe."));
            padreNombre = padre.getNombre();

            // Verificar que el nuevo padre no sea un descendiente de la categoría actual (anti-ciclos)
            validarNoDescendiente(id, req.getPadreId());
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(req.getDescripcion() != null ? req.getDescripcion().trim() : null);
        categoria.setPadreId(req.getPadreId());

        Categoria guardada = categoriaRepository.save(categoria);
        registrarBitacora("ACTUALIZAR_CATEGORIA", actor, guardada.getId().longValue(), ip,
                "Actualizada categoría: " + guardada.getNombre());

        long subcatCount = categoriaRepository.countByPadreId(id);
        long articulosCount = contarArticulosDeCategoriaSeguro(id);

        return CategoriaDTO.builder()
                .id(guardada.getId())
                .nombre(guardada.getNombre())
                .descripcion(guardada.getDescripcion())
                .padreId(guardada.getPadreId())
                .padreNombre(padreNombre)
                .activo(Boolean.TRUE.equals(guardada.getActivo()))
                .articulosCount(articulosCount)
                .subcategoriasCount(subcatCount)
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional
    public CategoriaDTO cambiarEstado(Integer id, Boolean activo, UUID empresaId, String actor, String ip) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada con ID: " + id));

        categoria.setActivo(Boolean.TRUE.equals(activo));
        Categoria guardada = categoriaRepository.save(categoria);

        String accion = Boolean.TRUE.equals(activo) ? "ACTIVAR_CATEGORIA" : "DESACTIVAR_CATEGORIA";
        registrarBitacora(accion, actor, guardada.getId().longValue(), ip,
                "Cambio de estado a " + (categoria.getActivo() ? "ACTIVO" : "INACTIVO"));

        return obtenerCategoria(id, empresaId);
    }

    @Transactional
    public void eliminarCategoria(Integer id, UUID empresaId, String actor, String ip) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada con ID: " + id));

        // 1. Validar si tiene subcategorías dependientes
        if (categoriaRepository.countByPadreId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La categoría posee subcategorías dependientes en el árbol jerárquico. " +
                    "No puede eliminarse físicamente, proceda a desactivarla.");
        }

        // 2. Validar si tiene productos/artículos asignados
        long articulosCount = contarArticulosDeCategoriaSeguro(id);
        if (articulosCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La categoría posee " + articulosCount + " artículo(s) asociado(s) en el catálogo. " +
                    "No puede eliminarse físicamente, utilice la desactivación (baja lógica).");
        }

        categoriaRepository.delete(categoria);
        registrarBitacora("ELIMINAR_CATEGORIA", actor, id.longValue(), ip,
                "Eliminada categoría: " + categoria.getNombre());
    }

    private void validarNoDescendiente(Integer categoriaId, Integer nuevoPadreId) {
        Integer curr = nuevoPadreId;
        Set<Integer> visitados = new HashSet<>();
        while (curr != null) {
            if (curr.equals(categoriaId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ciclo recursivo inválido: la categoría seleccionada como superior es descendiente de la categoría actual.");
            }
            if (!visitados.add(curr)) {
                break;
            }
            Optional<Categoria> parentOpt = categoriaRepository.findById(curr);
            curr = parentOpt.map(Categoria::getPadreId).orElse(null);
        }
    }

    private Map<Integer, Long> contarArticulosPorCategoriaSeguro() {
        Map<Integer, Long> counts = new HashMap<>();
        try {
            Integer existeArticulos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'articulos' AND column_name = 'categoria_id'",
                    Integer.class);
            if (existeArticulos != null && existeArticulos > 0) {
                jdbc.query("SELECT categoria_id, COUNT(*) as cnt FROM articulos WHERE categoria_id IS NOT NULL GROUP BY categoria_id",
                        (rs, rowNum) -> {
                            counts.put(rs.getInt("categoria_id"), rs.getLong("cnt"));
                            return null;
                        });
            }
        } catch (Exception ignored) {}

        try {
            Integer existeProductos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'categoria_id'",
                    Integer.class);
            if (existeProductos != null && existeProductos > 0) {
                jdbc.query("SELECT categoria_id, COUNT(*) as cnt FROM productos WHERE categoria_id IS NOT NULL GROUP BY categoria_id",
                        (rs, rowNum) -> {
                            int catId = rs.getInt("categoria_id");
                            counts.put(catId, counts.getOrDefault(catId, 0L) + rs.getLong("cnt"));
                            return null;
                        });
            }
        } catch (Exception ignored) {}

        return counts;
    }

    private long contarArticulosDeCategoriaSeguro(Integer categoriaId) {
        long count = 0;
        try {
            Integer existeArticulos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'articulos' AND column_name = 'categoria_id'",
                    Integer.class);
            if (existeArticulos != null && existeArticulos > 0) {
                Long c = jdbc.queryForObject("SELECT COUNT(*) FROM articulos WHERE categoria_id = ?", Long.class, categoriaId);
                if (c != null) count += c;
            }
        } catch (Exception ignored) {}

        try {
            Integer existeProductos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'categoria_id'",
                    Integer.class);
            if (existeProductos != null && existeProductos > 0) {
                Long c = jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE categoria_id = ?", Long.class, categoriaId);
                if (c != null) count += c;
            }
        } catch (Exception ignored) {}

        return count;
    }

    private void registrarBitacora(String accion, String actor, Long id, String ip, String detalle) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion, actor, "categorias", id, ip));
        } catch (Exception e) {
            log.warn("No se pudo registrar bitacora para categoria: {}", e.getMessage());
        }
    }
}
