package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto.*;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Empleado;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Sucursal;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.EmpleadoRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.SucursalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraLogRepository bitacoraLogRepository;

    /**
     * Lista empleados con filtros opcionales de sucursal, cargo, estado activo y búsqueda.
     */
    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listarEmpleados(Integer sucursalId, String cargo, Boolean activo, String search) {
        List<Empleado> todos = empleadoRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));

        String searchLower = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String cargoLower = (cargo != null && !cargo.isBlank()) ? cargo.trim().toLowerCase() : null;

        List<Empleado> empleados = todos.stream()
                .filter(e -> sucursalId == null || Objects.equals(e.getSucursalId(), sucursalId))
                .filter(e -> cargoLower == null || (e.getCargo() != null && e.getCargo().toLowerCase().contains(cargoLower)))
                .filter(e -> activo == null || Objects.equals(e.getActivo(), activo))
                .filter(e -> {
                    if (searchLower == null) return true;
                    boolean matchCi = e.getCi() != null && e.getCi().toLowerCase().contains(searchLower);
                    boolean matchNombre = e.getNombre() != null && e.getNombre().toLowerCase().contains(searchLower);
                    boolean matchApellido = e.getApellido() != null && e.getApellido().toLowerCase().contains(searchLower);
                    boolean matchEmail = e.getEmail() != null && e.getEmail().toLowerCase().contains(searchLower);
                    return matchCi || matchNombre || matchApellido || matchEmail;
                })
                .collect(Collectors.toList());

        // Mapa de sucursales para mapeo eficiente
        Map<Integer, String> nombresSucursales = sucursalRepository.findAll().stream()
                .collect(Collectors.toMap(Sucursal::getId, Sucursal::getNombre, (existing, replacement) -> existing));

        return empleados.stream().map(emp -> {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmpleadoId(emp.getId());
            return EmpleadoDTO.builder()
                    .id(emp.getId())
                    .sucursalId(emp.getSucursalId())
                    .sucursalNombre(nombresSucursales.getOrDefault(emp.getSucursalId(), "Sucursal #" + emp.getSucursalId()))
                    .ci(emp.getCi())
                    .nombre(emp.getNombre())
                    .apellido(emp.getApellido())
                    .nombreCompleto(emp.getNombreCompleto())
                    .cargo(emp.getCargo())
                    .telefono(emp.getTelefono())
                    .email(emp.getEmail())
                    .direccion(emp.getDireccion())
                    .fechaIngreso(emp.getFechaIngreso())
                    .activo(emp.getActivo())
                    .tieneUsuario(usuarioOpt.isPresent())
                    .usuarioId(usuarioOpt.map(Usuario::getId).orElse(null))
                    .usuarioEmail(usuarioOpt.map(Usuario::getEmail).orElse(null))
                    .usuarioActivo(usuarioOpt.map(Usuario::getActivo).orElse(null))
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listarDisponiblesParaUsuario() {
        return listarEmpleados(null, null, true, null).stream()
                .filter(e -> !Boolean.TRUE.equals(e.getTieneUsuario()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle de un empleado por ID.
     */
    @Transactional(readOnly = true)
    public EmpleadoDTO obtenerPorId(Long id) {
        Empleado emp = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));

        String sucursalNombre = sucursalRepository.findById(emp.getSucursalId())
                .map(Sucursal::getNombre)
                .orElse("Sucursal #" + emp.getSucursalId());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmpleadoId(emp.getId());

        return EmpleadoDTO.builder()
                .id(emp.getId())
                .sucursalId(emp.getSucursalId())
                .sucursalNombre(sucursalNombre)
                .ci(emp.getCi())
                .nombre(emp.getNombre())
                .apellido(emp.getApellido())
                .nombreCompleto(emp.getNombreCompleto())
                .cargo(emp.getCargo())
                .telefono(emp.getTelefono())
                .email(emp.getEmail())
                .direccion(emp.getDireccion())
                .fechaIngreso(emp.getFechaIngreso())
                .activo(emp.getActivo())
                .tieneUsuario(usuarioOpt.isPresent())
                .usuarioId(usuarioOpt.map(Usuario::getId).orElse(null))
                .usuarioEmail(usuarioOpt.map(Usuario::getEmail).orElse(null))
                .usuarioActivo(usuarioOpt.map(Usuario::getActivo).orElse(null))
                .build();
    }

    /**
     * Registra un nuevo empleado validando unicidad de CI y existencia de sucursal.
     */
    @Transactional
    public EmpleadoDTO crearEmpleado(CrearEmpleadoRequest request, String actor, String ip) {
        String ciLimpio = request.getCi().trim();

        // 1. Validar duplicidad de CI (UQ_EMPLEADO_EMPRESA_CI) -> 409 Conflict
        if (empleadoRepository.existsByCi(ciLimpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El documento de identidad (CI: " + ciLimpio + ") ya se encuentra registrado en la empresa.");
        }

        // 2. Validar que la sucursal exista y esté activa -> 400 Bad Request
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La sucursal seleccionada (ID: " + request.getSucursalId() + ") no existe."));

        if (!Boolean.TRUE.equals(sucursal.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La sucursal '" + sucursal.getNombre() + "' se encuentra inactiva.");
        }

        // 3. Persistir empleado
        Empleado nuevoEmpleado = Empleado.builder()
                .ci(ciLimpio)
                .sucursalId(sucursal.getId())
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .cargo(request.getCargo().trim())
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .direccion(request.getDireccion() != null ? request.getDireccion().trim() : null)
                .fechaIngreso(request.getFechaIngreso())
                .activo(true)
                .build();

        Empleado guardado = empleadoRepository.save(nuevoEmpleado);

        // 4. Registrar auditoría en bitácora
        registrarBitacora("CREAR_EMPLEADO", actor, guardado.getId(), ip,
                "Registrado empleado: CI=" + guardado.getCi() + ", Nombre=" + guardado.getNombreCompleto());

        log.info("CU04: Empleado creado exitosamente id={}, CI={}", guardado.getId(), guardado.getCi());

        return obtenerPorId(guardado.getId());
    }

    /**
     * Actualiza información de contacto, cargo y sucursal de un empleado.
     */
    @Transactional
    public EmpleadoDTO actualizarEmpleado(Long id, ActualizarEmpleadoRequest request, String actor, String ip) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));

        String ciLimpio = request.getCi().trim();

        // 1. Validar colisión de CI con otros empleados -> 409 Conflict
        if (empleadoRepository.existsByCiAndIdNot(ciLimpio, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El documento de identidad (CI: " + ciLimpio + ") ya pertenece a otro colaborador.");
        }

        // 2. Validar sucursal asignada -> 400 Bad Request
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La sucursal asignada (ID: " + request.getSucursalId() + ") no existe."));

        if (!Boolean.TRUE.equals(sucursal.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La sucursal '" + sucursal.getNombre() + "' se encuentra inactiva.");
        }

        // 3. Actualizar campos
        empleado.setCi(ciLimpio);
        empleado.setSucursalId(sucursal.getId());
        empleado.setNombre(request.getNombre().trim());
        empleado.setApellido(request.getApellido().trim());
        empleado.setCargo(request.getCargo().trim());
        empleado.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        empleado.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        empleado.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        empleado.setFechaIngreso(request.getFechaIngreso());
        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        }

        Empleado guardado = empleadoRepository.save(empleado);

        // 4. Auditoría
        registrarBitacora("ACTUALIZAR_EMPLEADO", actor, guardado.getId(), ip,
                "Actualizado empleado: CI=" + guardado.getCi() + ", Cargo=" + guardado.getCargo());

        log.info("CU04: Empleado actualizado id={}", guardado.getId());

        return obtenerPorId(guardado.getId());
    }

    /**
     * Aplica baja lógica o reactivación del colaborador.
     * Si se desactiva, inhabilita de forma automática su cuenta de usuario vinculada (cascada lógica).
     */
    @Transactional
    public EmpleadoDTO cambiarEstado(Long id, CambiarEstadoEmpleadoRequest request, String actor, String ip) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));

        boolean nuevoEstado = Boolean.TRUE.equals(request.getActivo());
        empleado.setActivo(nuevoEstado);
        empleadoRepository.save(empleado);

        // Desactivación en cascada de la cuenta de usuario asociada
        if (!nuevoEstado) {
            usuarioRepository.findByEmpleadoId(id).ifPresent(usuario -> {
                usuario.setActivo(false);
                usuarioRepository.save(usuario);
                log.info("CU04: Cuenta de usuario '{}' desactivada en cascada por baja del empleado id={}",
                        usuario.getEmail(), id);
            });
        }

        String accion = nuevoEstado ? "ACTIVAR_EMPLEADO" : "DESACTIVAR_EMPLEADO";
        registrarBitacora(accion, actor, id, ip,
                "Cambio de estado empleado a: " + (nuevoEstado ? "ACTIVO" : "INACTIVO") + " (CI: " + empleado.getCi() + ")");

        log.info("CU04: Estado de empleado id={} cambiado a {}", id, nuevoEstado);

        return obtenerPorId(id);
    }

    /**
     * Intenta eliminar o aplicar baja lógica garantizando que no se rompan cuentas o históricos.
     */
    @Transactional
    public void eliminarEmpleado(Long id, String actor, String ip) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + id));

        boolean tieneUsuario = usuarioRepository.existsByEmpleadoId(id);

        if (tieneUsuario) {
            // El empleado tiene cuenta asociada: impedir borrado físico y aplicar baja lógica
            empleado.setActivo(false);
            empleadoRepository.save(empleado);

            usuarioRepository.findByEmpleadoId(id).ifPresent(u -> {
                u.setActivo(false);
                usuarioRepository.save(u);
            });

            registrarBitacora("DESACTIVAR_EMPLEADO", actor, id, ip,
                    "Baja lógica aplicada por existencia de usuario asociado: CI=" + empleado.getCi());

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El empleado tiene una cuenta de usuario asociada; no se permite la eliminación física, se ha aplicado baja lógica.");
        }

        // Si no tiene usuario asociado, intentar eliminación física
        try {
            empleadoRepository.delete(empleado);
            registrarBitacora("ELIMINAR_EMPLEADO", actor, id, ip,
                    "Eliminado físicamente empleado: CI=" + empleado.getCi() + ", Nombre=" + empleado.getNombreCompleto());
        } catch (Exception ex) {
            // Si existen referencias en ventas, compras o kardex
            empleado.setActivo(false);
            empleadoRepository.save(empleado);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El empleado posee registros históricos vinculados; se aplicó baja lógica preventiva.");
        }
    }

    /**
     * Lista todas las sucursales activas para poblar los selectores del frontend.
     */
    @Transactional(readOnly = true)
    public List<SucursalItemDTO> listarSucursalesActivas() {
        return sucursalRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(s -> SucursalItemDTO.builder()
                        .id(s.getId())
                        .nombre(s.getNombre())
                        .direccion(s.getDireccion())
                        .telefono(s.getTelefono())
                        .activo(s.getActivo())
                        .build())
                .collect(Collectors.toList());
    }

    private void registrarBitacora(String accion, String actor, Long empleadoId, String ip, String detalle) {
        try {
            String responsable = (actor != null && !actor.isBlank()) ? actor : "admin@asustech.bo";
            bitacoraLogRepository.save(new BitacoraLog(accion, responsable, "empleados", empleadoId, ip));
        } catch (Exception e) {
            log.warn("No se pudo persistir registro en bitacora_logs: {}", e.getMessage());
        }
    }
}
