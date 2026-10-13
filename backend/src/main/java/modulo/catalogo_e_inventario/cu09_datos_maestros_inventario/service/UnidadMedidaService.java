package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarUnidadMedidaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearUnidadMedidaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.UnidadMedidaDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.entity.UnidadMedida;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.repository.UnidadMedidaRepository;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final BitacoraLogRepository bitacoraLogRepository;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<UnidadMedidaDTO> listarUnidades(UUID empresaId) {
        List<UnidadMedida> unidades;
        if (empresaId != null) {
            unidades = unidadMedidaRepository.findByEmpresaIdOrEmpresaIdIsNullOrderByNombreAsc(empresaId);
        } else {
            unidades = unidadMedidaRepository.findAllByOrderByNombreAsc();
        }

        Map<String, Long> usoMap = contarUsoPorCodigoSeguro();

        return unidades.stream()
                .map(u -> UnidadMedidaDTO.builder()
                        .id(u.getId())
                        .codigo(u.getCodigo())
                        .nombre(u.getNombre())
                        .abreviatura(u.getAbreviatura())
                        .activo(Boolean.TRUE.equals(u.getActivo()))
                        .productosCount(usoMap.getOrDefault(u.getCodigo().toUpperCase(), 0L))
                        .fechaCreacion(u.getFechaCreacion())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadMedidaDTO obtenerUnidad(Integer id, UUID empresaId) {
        UnidadMedida u = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad de medida no encontrada con ID: " + id));

        long count = contarUsoDeUnidadSeguro(u.getCodigo(), id);

        return UnidadMedidaDTO.builder()
                .id(u.getId())
                .codigo(u.getCodigo())
                .nombre(u.getNombre())
                .abreviatura(u.getAbreviatura())
                .activo(Boolean.TRUE.equals(u.getActivo()))
                .productosCount(count)
                .fechaCreacion(u.getFechaCreacion())
                .build();
    }

    @Transactional
    public UnidadMedidaDTO crearUnidad(CrearUnidadMedidaRequest req, UUID empresaId, String actor, String ip) {
        String codigo = req.getCodigo().trim().toUpperCase();
        String abreviatura = req.getAbreviatura().trim();

        // 1. Validar unicidad de código
        boolean codigoExiste = empresaId != null
                ? unidadMedidaRepository.existsByCodigoIgnoreCaseAndEmpresaId(codigo, empresaId)
                : unidadMedidaRepository.existsByCodigoIgnoreCase(codigo);

        if (codigoExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una unidad de medida con el código '" + codigo + "'.");
        }

        // 2. Validar unicidad de abreviatura
        boolean abrevExiste = empresaId != null
                ? unidadMedidaRepository.existsByAbreviaturaIgnoreCaseAndEmpresaId(abreviatura, empresaId)
                : unidadMedidaRepository.existsByAbreviaturaIgnoreCase(abreviatura);

        if (abrevExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una unidad de medida con la abreviatura '" + abreviatura + "'.");
        }

        UnidadMedida unidad = UnidadMedida.builder()
                .codigo(codigo)
                .nombre(req.getNombre().trim())
                .abreviatura(abreviatura)
                .activo(true)
                .empresaId(empresaId)
                .build();

        UnidadMedida guardada = unidadMedidaRepository.save(unidad);
        registrarBitacora("CREAR_UNIDAD_MEDIDA", actor, guardada.getId().longValue(), ip,
                "Creada unidad de medida: " + guardada.getNombre() + " (" + guardada.getCodigo() + ")");

        return UnidadMedidaDTO.builder()
                .id(guardada.getId())
                .codigo(guardada.getCodigo())
                .nombre(guardada.getNombre())
                .abreviatura(guardada.getAbreviatura())
                .activo(true)
                .productosCount(0L)
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional
    public UnidadMedidaDTO actualizarUnidad(Integer id, ActualizarUnidadMedidaRequest req, UUID empresaId, String actor, String ip) {
        UnidadMedida u = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad de medida no encontrada con ID: " + id));

        String codigo = req.getCodigo().trim().toUpperCase();
        String abreviatura = req.getAbreviatura().trim();

        // 1. Validar unicidad de código
        boolean codigoExisteOtro = empresaId != null
                ? unidadMedidaRepository.existsByCodigoIgnoreCaseAndEmpresaIdAndIdNot(codigo, empresaId, id)
                : unidadMedidaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id);

        if (codigoExisteOtro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra unidad de medida con el código '" + codigo + "'.");
        }

        // 2. Validar unicidad de abreviatura
        boolean abrevExisteOtro = empresaId != null
                ? unidadMedidaRepository.existsByAbreviaturaIgnoreCaseAndEmpresaIdAndIdNot(abreviatura, empresaId, id)
                : unidadMedidaRepository.existsByAbreviaturaIgnoreCaseAndIdNot(abreviatura, id);

        if (abrevExisteOtro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra unidad de medida con la abreviatura '" + abreviatura + "'.");
        }

        u.setCodigo(codigo);
        u.setNombre(req.getNombre().trim());
        u.setAbreviatura(abreviatura);

        UnidadMedida guardada = unidadMedidaRepository.save(u);
        registrarBitacora("ACTUALIZAR_UNIDAD_MEDIDA", actor, guardada.getId().longValue(), ip,
                "Actualizada unidad de medida: " + guardada.getNombre() + " (" + guardada.getCodigo() + ")");

        long count = contarUsoDeUnidadSeguro(guardada.getCodigo(), guardada.getId());

        return UnidadMedidaDTO.builder()
                .id(guardada.getId())
                .codigo(guardada.getCodigo())
                .nombre(guardada.getNombre())
                .abreviatura(guardada.getAbreviatura())
                .activo(Boolean.TRUE.equals(guardada.getActivo()))
                .productosCount(count)
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional
    public UnidadMedidaDTO cambiarEstado(Integer id, Boolean activo, UUID empresaId, String actor, String ip) {
        UnidadMedida u = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad de medida no encontrada con ID: " + id));

        u.setActivo(Boolean.TRUE.equals(activo));
        UnidadMedida guardada = unidadMedidaRepository.save(u);

        String accion = Boolean.TRUE.equals(activo) ? "ACTIVAR_UNIDAD_MEDIDA" : "DESACTIVAR_UNIDAD_MEDIDA";
        registrarBitacora(accion, actor, guardada.getId().longValue(), ip,
                "Cambio de estado a " + (u.getActivo() ? "ACTIVO" : "INACTIVO"));

        long count = contarUsoDeUnidadSeguro(guardada.getCodigo(), guardada.getId());

        return UnidadMedidaDTO.builder()
                .id(guardada.getId())
                .codigo(guardada.getCodigo())
                .nombre(guardada.getNombre())
                .abreviatura(guardada.getAbreviatura())
                .activo(guardada.getActivo())
                .productosCount(count)
                .fechaCreacion(guardada.getFechaCreacion())
                .build();
    }

    @Transactional
    public void eliminarUnidad(Integer id, UUID empresaId, String actor, String ip) {
        UnidadMedida u = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad de medida no encontrada con ID: " + id));

        long count = contarUsoDeUnidadSeguro(u.getCodigo(), id);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La unidad de medida está asignada a " + count + " artículo(s) en el catálogo. " +
                    "No puede eliminarse físicamente, utilice la desactivación (baja lógica).");
        }

        unidadMedidaRepository.delete(u);
        registrarBitacora("ELIMINAR_UNIDAD_MEDIDA", actor, id.longValue(), ip,
                "Eliminada unidad de medida: " + u.getNombre() + " (" + u.getCodigo() + ")");
    }

    private Map<String, Long> contarUsoPorCodigoSeguro() {
        Map<String, Long> map = new HashMap<>();
        try {
            // Verificar primero si existe la columna en PostgreSQL antes de ejecutar la consulta
            Integer existeColTexto = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'unidad_medida'",
                    Integer.class);
            if (existeColTexto != null && existeColTexto > 0) {
                jdbc.query("SELECT UPPER(unidad_medida) as u_code, COUNT(*) as cnt FROM productos WHERE unidad_medida IS NOT NULL GROUP BY UPPER(unidad_medida)",
                        (rs, rowNum) -> {
                            map.put(rs.getString("u_code"), rs.getLong("cnt"));
                            return null;
                        });
            }
        } catch (Exception ignored) {}
        return map;
    }

    private long contarUsoDeUnidadSeguro(String codigo, Integer id) {
        long total = 0;
        try {
            Integer existeColTexto = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'unidad_medida'",
                    Integer.class);
            if (existeColTexto != null && existeColTexto > 0) {
                Long c = jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE UPPER(unidad_medida) = ?", Long.class, codigo.toUpperCase());
                if (c != null) total += c;
            }
        } catch (Exception ignored) {}

        try {
            Integer existeColId = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'unidad_medida_id'",
                    Integer.class);
            if (existeColId != null && existeColId > 0) {
                Long c = jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE unidad_medida_id = ?", Long.class, id);
                if (c != null) total += c;
            }
        } catch (Exception ignored) {}

        return total;
    }

    private void registrarBitacora(String accion, String actor, Long id, String ip, String detalle) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion, actor, "unidades_medida", id, ip));
        } catch (Exception e) {
            log.warn("No se pudo registrar bitacora para unidad de medida: {}", e.getMessage());
        }
    }
}
