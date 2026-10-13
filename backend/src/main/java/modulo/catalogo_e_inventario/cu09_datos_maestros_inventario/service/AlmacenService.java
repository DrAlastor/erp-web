package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service;

import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarAlmacenRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.AlmacenDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearAlmacenRequest;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Sucursal;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.SucursalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlmacenService {

    private static final Logger log = LoggerFactory.getLogger(AlmacenService.class);

    private final SucursalRepository sucursalRepository;
    private final BitacoraLogRepository bitacoraLogRepository;
    private final JdbcTemplate jdbc;

    @Autowired
    public AlmacenService(
            SucursalRepository sucursalRepository,
            BitacoraLogRepository bitacoraLogRepository,
            JdbcTemplate jdbc) {
        this.sucursalRepository = sucursalRepository;
        this.bitacoraLogRepository = bitacoraLogRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<AlmacenDTO> listarAlmacenes(UUID empresaId) {
        Map<Integer, String> ciudadesMap = obtenerMapaCiudades();

        return sucursalRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(s -> mapSucursalToDTO(s, ciudadesMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlmacenDTO obtenerAlmacen(Integer id, UUID empresaId) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada con ID: " + id));

        Map<Integer, String> ciudadesMap = obtenerMapaCiudades();
        return mapSucursalToDTO(sucursal, ciudadesMap);
    }

    @Transactional
    public AlmacenDTO crearAlmacen(CrearAlmacenRequest req, UUID empresaId, String actor, String ip) {
        String nombre = req.getNombre() != null ? req.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la sucursal es obligatorio.");
        }

        // Validar unicidad de nombre de sucursal
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sucursal WHERE LOWER(nombre) = LOWER(?)",
                Integer.class, nombre);
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sucursal con el nombre '" + nombre + "'.");
        }

        Integer ciudadId = req.getCiudadId() != null ? req.getCiudadId() : 1;

        LocalTime apertura = parseHora(req.getHoraApertura(), LocalTime.of(8, 30));
        LocalTime cierre = parseHora(req.getHoraCierre(), LocalTime.of(19, 0));

        Sucursal nueva = new Sucursal();
        nueva.setNombre(nombre);
        nueva.setCiudadId(ciudadId);
        nueva.setDireccion(req.getDireccion() != null ? req.getDireccion().trim() : null);
        nueva.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);
        nueva.setHoraApertura(apertura);
        nueva.setHoraCierre(cierre);
        nueva.setActivo(true);

        Sucursal guardada = sucursalRepository.save(nueva);
        registrarBitacora("CREAR_SUCURSAL", actor, guardada.getId().longValue(), ip,
                "Creada sucursal: " + guardada.getNombre());

        Map<Integer, String> ciudadesMap = obtenerMapaCiudades();
        return mapSucursalToDTO(guardada, ciudadesMap);
    }

    @Transactional
    public AlmacenDTO actualizarAlmacen(Integer id, ActualizarAlmacenRequest req, UUID empresaId, String actor, String ip) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada con ID: " + id));

        String nombre = req.getNombre() != null ? req.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la sucursal es obligatorio.");
        }

        // Validar unicidad de nombre
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sucursal WHERE LOWER(nombre) = LOWER(?) AND id <> ?",
                Integer.class, nombre, id);
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra sucursal con el nombre '" + nombre + "'.");
        }

        sucursal.setNombre(nombre);
        if (req.getCiudadId() != null) {
            sucursal.setCiudadId(req.getCiudadId());
        }
        sucursal.setDireccion(req.getDireccion() != null ? req.getDireccion().trim() : null);
        sucursal.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);

        if (req.getHoraApertura() != null && !req.getHoraApertura().trim().isEmpty()) {
            sucursal.setHoraApertura(parseHora(req.getHoraApertura(), sucursal.getHoraApertura()));
        }
        if (req.getHoraCierre() != null && !req.getHoraCierre().trim().isEmpty()) {
            sucursal.setHoraCierre(parseHora(req.getHoraCierre(), sucursal.getHoraCierre()));
        }

        Sucursal guardada = sucursalRepository.save(sucursal);
        registrarBitacora("ACTUALIZAR_SUCURSAL", actor, guardada.getId().longValue(), ip,
                "Actualizada sucursal: " + guardada.getNombre());

        Map<Integer, String> ciudadesMap = obtenerMapaCiudades();
        return mapSucursalToDTO(guardada, ciudadesMap);
    }

    @Transactional
    public AlmacenDTO cambiarEstado(Integer id, Boolean activo, UUID empresaId, String actor, String ip) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada con ID: " + id));

        sucursal.setActivo(Boolean.TRUE.equals(activo));
        Sucursal guardada = sucursalRepository.save(sucursal);

        String accion = Boolean.TRUE.equals(activo) ? "ACTIVAR_SUCURSAL" : "DESACTIVAR_SUCURSAL";
        registrarBitacora(accion, actor, guardada.getId().longValue(), ip,
                "Cambio de estado a " + (sucursal.getActivo() ? "ACTIVO" : "INACTIVO"));

        Map<Integer, String> ciudadesMap = obtenerMapaCiudades();
        return mapSucursalToDTO(guardada, ciudadesMap);
    }

    @Transactional
    public void eliminarAlmacen(Integer id, UUID empresaId, String actor, String ip) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada con ID: " + id));

        // Validar si tiene colaboradores asignados
        try {
            Integer empCount = jdbc.queryForObject("SELECT COUNT(*) FROM empleados WHERE sucursal_id = ?", Integer.class, id);
            if (empCount != null && empCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La sucursal tiene " + empCount + " colaborador(es) asignado(s). No puede eliminarse, proceda a desactivarla.");
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ignored) {}

        // Validar si tiene stock o movimientos
        try {
            Integer invCount = jdbc.queryForObject("SELECT COUNT(*) FROM inventario_sucursal WHERE sucursal_id = ?", Integer.class, id);
            if (invCount != null && invCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La sucursal tiene existencias registradas en inventario. Proceda a desactivarla.");
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ignored) {}

        sucursalRepository.delete(sucursal);
        registrarBitacora("ELIMINAR_SUCURSAL", actor, id.longValue(), ip, "Eliminada sucursal: " + sucursal.getNombre());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCiudades() {
        try {
            List<Map<String, Object>> res = jdbc.query("SELECT id, nombre, pais FROM ciudad ORDER BY id ASC", (rs, rowNum) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("nombre", rs.getString("nombre"));
                map.put("pais", rs.getString("pais"));
                return map;
            });
            if (res != null && !res.isEmpty()) {
                return res;
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(crearCiudadMap(1, "Santa Cruz de la Sierra", "Bolivia"));
        fallback.add(crearCiudadMap(2, "La Paz", "Bolivia"));
        fallback.add(crearCiudadMap(3, "Cochabamba", "Bolivia"));
        fallback.add(crearCiudadMap(4, "Sucre", "Bolivia"));
        fallback.add(crearCiudadMap(5, "Tarija", "Bolivia"));
        fallback.add(crearCiudadMap(6, "Oruro", "Bolivia"));
        fallback.add(crearCiudadMap(7, "Potosí", "Bolivia"));
        fallback.add(crearCiudadMap(8, "Trinidad (Beni)", "Bolivia"));
        fallback.add(crearCiudadMap(9, "Cobija (Pando)", "Bolivia"));
        return fallback;
    }

    private Map<String, Object> crearCiudadMap(int id, String nombre, String pais) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("nombre", nombre);
        m.put("pais", pais);
        return m;
    }

    private Map<Integer, String> obtenerMapaCiudades() {
        Map<Integer, String> map = new HashMap<>();
        try {
            jdbc.query("SELECT id, nombre FROM ciudad", (rs, rowNum) -> {
                map.put(rs.getInt("id"), rs.getString("nombre"));
                return null;
            });
        } catch (Exception ignored) {}
        if (map.isEmpty()) {
            map.put(1, "Santa Cruz de la Sierra");
            map.put(2, "La Paz");
            map.put(3, "Cochabamba");
            map.put(4, "Sucre");
            map.put(5, "Tarija");
        }
        return map;
    }

    private AlmacenDTO mapSucursalToDTO(Sucursal s, Map<Integer, String> ciudadesMap) {
        String ciudadNombre = s.getCiudadId() != null
                ? ciudadesMap.getOrDefault(s.getCiudadId(), "Santa Cruz de la Sierra")
                : "Santa Cruz de la Sierra";

        String codigo = "SUC-" + String.format("%02d", s.getId());
        String nombre = s.getNombre();

        String horaAper = s.getHoraApertura() != null 
                ? (s.getHoraApertura().toString().length() >= 5 ? s.getHoraApertura().toString().substring(0, 5) : s.getHoraApertura().toString())
                : "08:30";
        String horaCier = s.getHoraCierre() != null 
                ? (s.getHoraCierre().toString().length() >= 5 ? s.getHoraCierre().toString().substring(0, 5) : s.getHoraCierre().toString())
                : "19:00";
        String horario = horaAper + " - " + horaCier;

        AlmacenDTO dto = new AlmacenDTO();
        dto.setId(s.getId());
        dto.setNombre(nombre);
        dto.setCodigo(codigo);
        dto.setSucursalId(s.getId());
        dto.setSucursalNombre(nombre);
        dto.setCiudadId(s.getCiudadId() != null ? s.getCiudadId() : 1);
        dto.setCiudadNombre(ciudadNombre);
        dto.setDireccion(s.getDireccion() != null ? s.getDireccion() : "Sin dirección física");
        dto.setTelefono(s.getTelefono() != null ? s.getTelefono() : "Sin teléfono");
        dto.setHoraApertura(horaAper);
        dto.setHoraCierre(horaCier);
        dto.setHorario(horario);
        dto.setCapacidadEstimada(BigDecimal.valueOf(1000.00));
        dto.setEsPrincipal(s.getId() != null && s.getId() == 1);
        dto.setActivo(Boolean.TRUE.equals(s.getActivo()));
        return dto;
    }

    private LocalTime parseHora(String horaStr, LocalTime defaultVal) {
        if (horaStr == null || horaStr.trim().isEmpty()) return defaultVal;
        try {
            String trimmed = horaStr.trim();
            if (trimmed.length() == 5) {
                return LocalTime.parse(trimmed);
            } else if (trimmed.length() == 8) {
                return LocalTime.parse(trimmed.substring(0, 5));
            }
            return LocalTime.parse(trimmed);
        } catch (Exception e) {
            return defaultVal != null ? defaultVal : LocalTime.of(8, 30);
        }
    }

    private void registrarBitacora(String accion, String actor, Long id, String ip, String detalle) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion, actor, "sucursales", id, ip));
        } catch (Exception e) {
            log.warn("No se pudo registrar bitacora para sucursal: {}", e.getMessage());
        }
    }
}
