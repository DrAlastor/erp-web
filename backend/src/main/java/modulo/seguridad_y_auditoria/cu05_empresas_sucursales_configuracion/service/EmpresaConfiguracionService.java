package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Sucursal;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.EmpleadoRepository;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository.SucursalRepository;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto.*;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.Ciudad;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.EmpresaPerfil;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.ParametroSistema;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository.CiudadRepository;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository.EmpresaPerfilRepository;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository.ParametroSistemaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CU05: datos de la empresa, sucursales y parametros de configuracion general.
 * Cada cambio deja su rastro en la bitacora (CU08).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaConfiguracionService {

    /** Catalogo de parametros: clave -> {valor por defecto, descripcion}. */
    private static final Map<String, String[]> CATALOGO_PARAMETROS = new LinkedHashMap<>();

    static {
        CATALOGO_PARAMETROS.put("iva_porcentaje", new String[]{"13", "Porcentaje de IVA aplicado a compras y ventas"});
        CATALOGO_PARAMETROS.put("decimales_moneda", new String[]{"2", "Cantidad de decimales en montos"});
        CATALOGO_PARAMETROS.put("zona_horaria", new String[]{"America/La_Paz", "Zona horaria de la empresa"});
        CATALOGO_PARAMETROS.put("formato_fecha", new String[]{"dd/MM/yyyy", "Formato de fecha en pantallas y reportes"});
        CATALOGO_PARAMETROS.put("tipo_cambio_usd", new String[]{"6.96", "Tipo de cambio oficial USD -> moneda principal"});
        CATALOGO_PARAMETROS.put("dias_credito_defecto", new String[]{"30", "Días de crédito por defecto en operaciones a crédito"});
    }

    private final EmpresaPerfilRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final CiudadRepository ciudadRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final EmpleadoRepository empleadoRepository;
    private final BitacoraLogRepository bitacoraLogRepository;

    // ---------------------------------------------------------------- Empresa

    @Transactional
    public EmpresaDTO obtenerEmpresa(UUID empresaId) {
        return aDTO(buscarEmpresa(empresaId));
    }

    @Transactional
    public EmpresaDTO actualizarEmpresa(UUID empresaId, ActualizarEmpresaRequest req, String actor, String ip) {
        EmpresaPerfil empresa = buscarEmpresa(empresaId);
        String nit = req.nit().trim();
        if (empresaRepository.existsByNitAndIdNot(nit, empresa.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra empresa con el NIT " + nit);
        }
        empresa.setNombre(req.nombre().trim());
        empresa.setRazonSocial(vacioANulo(req.razonSocial()));
        empresa.setNit(nit);
        empresa.setDireccion(vacioANulo(req.direccion()));
        empresa.setTelefono(vacioANulo(req.telefono()));
        empresa.setEmail(vacioANulo(req.email()));
        empresa.setMonedaPrincipal(req.monedaPrincipal().trim().toUpperCase());
        String secundaria = vacioANulo(req.monedaSecundaria());
        if (secundaria != null && secundaria.equalsIgnoreCase(empresa.getMonedaPrincipal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La moneda secundaria debe ser distinta de la principal");
        }
        empresa.setMonedaSecundaria(secundaria == null ? null : secundaria.toUpperCase());
        empresa.setLogoUrl(vacioANulo(req.logoUrl()));
        empresaRepository.save(empresa);

        bitacora("ACTUALIZAR_EMPRESA", actor, "empresa", 0L, ip);
        log.info("CU05: empresa {} actualizada por {}", empresa.getId(), actor);
        return aDTO(empresa);
    }

    // ------------------------------------------------------------- Sucursales

    @Transactional(readOnly = true)
    public List<SucursalDTO> listarSucursales(Boolean activo, String search) {
        String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        Map<Integer, String> ciudades = mapaCiudades();
        return sucursalRepository.findAll().stream()
                .filter(s -> activo == null || Objects.equals(s.getActivo(), activo))
                .filter(s -> q == null
                        || (s.getNombre() != null && s.getNombre().toLowerCase().contains(q))
                        || (s.getDireccion() != null && s.getDireccion().toLowerCase().contains(q)))
                .sorted(Comparator.comparing(Sucursal::getId))
                .map(s -> aDTO(s, ciudades))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SucursalDTO obtenerSucursal(Integer id) {
        return aDTO(buscarSucursal(id), mapaCiudades());
    }

    @Transactional
    public SucursalDTO crearSucursal(GuardarSucursalRequest req, String actor, String ip) {
        validarSucursal(req, null);
        Sucursal nueva = Sucursal.builder().activo(true).build();
        aplicar(nueva, req);
        Sucursal guardada = sucursalRepository.save(nueva);
        bitacora("CREAR_SUCURSAL", actor, "sucursal", guardada.getId().longValue(), ip);
        log.info("CU05: sucursal creada id={}", guardada.getId());
        return aDTO(guardada, mapaCiudades());
    }

    @Transactional
    public SucursalDTO actualizarSucursal(Integer id, GuardarSucursalRequest req, String actor, String ip) {
        Sucursal sucursal = buscarSucursal(id);
        validarSucursal(req, id);
        aplicar(sucursal, req);
        Sucursal guardada = sucursalRepository.save(sucursal);
        bitacora("ACTUALIZAR_SUCURSAL", actor, "sucursal", id.longValue(), ip);
        return aDTO(guardada, mapaCiudades());
    }

    @Transactional
    public SucursalDTO cambiarEstadoSucursal(Integer id, boolean activo, String actor, String ip) {
        Sucursal sucursal = buscarSucursal(id);
        if (!activo) {
            boolean conEmpleados = empleadoRepository.findAll().stream()
                    .anyMatch(e -> Objects.equals(e.getSucursalId(), id) && Boolean.TRUE.equals(e.getActivo()));
            if (conEmpleados) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede desactivar la sucursal: tiene empleados activos asignados");
            }
            long activas = sucursalRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActivo()) && !s.getId().equals(id)).count();
            if (activas == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La empresa debe conservar al menos una sucursal activa");
            }
        }
        sucursal.setActivo(activo);
        Sucursal guardada = sucursalRepository.save(sucursal);
        bitacora(activo ? "ACTIVAR_SUCURSAL" : "DESACTIVAR_SUCURSAL", actor, "sucursal", id.longValue(), ip);
        return aDTO(guardada, mapaCiudades());
    }

    @Transactional(readOnly = true)
    public List<CiudadDTO> listarCiudades() {
        return ciudadRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> new CiudadDTO(c.getId(), c.getNombre(), c.getPais()))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------- Configuracion

    /** Devuelve los parametros de la empresa; crea con su valor por defecto los que aun no existen. */
    @Transactional
    public List<ParametroDTO> obtenerConfiguracion(UUID empresaIdSolicitada) {
        UUID empresaId = buscarEmpresa(empresaIdSolicitada).getId();
        Map<String, ParametroSistema> existentes = parametroRepository.findByEmpresaIdOrderByClaveAsc(empresaId)
                .stream().collect(Collectors.toMap(ParametroSistema::getClave, p -> p));
        CATALOGO_PARAMETROS.forEach((clave, def) -> {
            if (!existentes.containsKey(clave)) {
                existentes.put(clave, parametroRepository.save(new ParametroSistema(empresaId, clave, def[0], def[1])));
            }
        });
        return CATALOGO_PARAMETROS.keySet().stream()
                .map(existentes::get)
                .map(p -> new ParametroDTO(p.getClave(), p.getValor(), p.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ParametroDTO> guardarConfiguracion(UUID empresaIdSolicitada, GuardarConfiguracionRequest req, String actor, String ip) {
        UUID empresaId = buscarEmpresa(empresaIdSolicitada).getId();
        obtenerConfiguracion(empresaId); // asegura que existan todas las filas
        Map<String, ParametroSistema> filas = parametroRepository.findByEmpresaIdOrderByClaveAsc(empresaId)
                .stream().collect(Collectors.toMap(ParametroSistema::getClave, p -> p));

        for (Map.Entry<String, String> e : req.parametros().entrySet()) {
            if (!CATALOGO_PARAMETROS.containsKey(e.getKey())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetro desconocido: " + e.getKey());
            }
            String valor = e.getValue() == null ? "" : e.getValue().trim();
            validarParametro(e.getKey(), valor);
            ParametroSistema fila = filas.get(e.getKey());
            fila.setValor(valor);
            fila.setActualizadoEn(LocalDateTime.now());
            parametroRepository.save(fila);
        }
        bitacora("ACTUALIZAR_CONFIGURACION", actor, "parametro_sistema", 0L, ip);
        return obtenerConfiguracion(empresaId);
    }

    // ---------------------------------------------------------------- Privado

    private void validarParametro(String clave, String valor) {
        if (valor.isEmpty() || valor.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor de " + clave + " es obligatorio");
        }
        try {
            switch (clave) {
                case "iva_porcentaje" -> {
                    double v = Double.parseDouble(valor);
                    if (v < 0 || v > 100) throw new IllegalArgumentException();
                }
                case "decimales_moneda" -> {
                    int v = Integer.parseInt(valor);
                    if (v < 0 || v > 6) throw new IllegalArgumentException();
                }
                case "tipo_cambio_usd" -> {
                    if (Double.parseDouble(valor) <= 0) throw new IllegalArgumentException();
                }
                case "dias_credito_defecto" -> {
                    if (Integer.parseInt(valor) < 0) throw new IllegalArgumentException();
                }
                case "zona_horaria" -> java.time.ZoneId.of(valor);
                case "formato_fecha" -> java.time.format.DateTimeFormatter.ofPattern(valor);
                default -> { }
            }
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor inválido para " + clave + ": " + valor);
        }
    }

    private void validarSucursal(GuardarSucursalRequest req, Integer idActual) {
        if (!ciudadRepository.existsById(req.ciudadId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ciudad indicada no existe");
        }
        if (req.horaApertura() != null && req.horaCierre() != null
                && !req.horaCierre().isAfter(req.horaApertura())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de cierre debe ser posterior a la de apertura");
        }
        String nombre = req.nombre().trim();
        boolean duplicada = sucursalRepository.findAll().stream()
                .anyMatch(s -> s.getNombre().equalsIgnoreCase(nombre) && !Objects.equals(s.getId(), idActual));
        if (duplicada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sucursal llamada \"" + nombre + "\"");
        }
    }

    private void aplicar(Sucursal s, GuardarSucursalRequest req) {
        s.setCiudadId(req.ciudadId());
        s.setNombre(req.nombre().trim());
        s.setDireccion(req.direccion().trim());
        s.setTelefono(vacioANulo(req.telefono()));
        s.setHoraApertura(req.horaApertura());
        s.setHoraCierre(req.horaCierre());
    }

    /**
     * Empresa de esta base de datos. Cada empresa tiene su propia base, asi que hay una sola fila;
     * si el login trae un id que existe se usa ese, y si la tabla esta vacia se crea la empresa inicial.
     */
    private EmpresaPerfil buscarEmpresa(UUID empresaId) {
        if (empresaId != null) {
            Optional<EmpresaPerfil> exacta = empresaRepository.findById(empresaId);
            if (exacta.isPresent()) return exacta.get();
        }
        return empresaRepository.findAll().stream().filter(EmpresaPerfil::isActivo).findFirst()
                .or(() -> empresaRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    EmpresaPerfil nueva = new EmpresaPerfil();
                    nueva.setId(UUID.randomUUID());
                    nueva.setNombre("Mi Empresa");
                    nueva.setNit("SIN-NIT");
                    log.info("CU05: no habia empresa registrada, se creo la empresa inicial {}", nueva.getId());
                    return empresaRepository.save(nueva);
                });
    }

    private Sucursal buscarSucursal(Integer id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));
    }

    private Map<Integer, String> mapaCiudades() {
        return ciudadRepository.findAll().stream()
                .collect(Collectors.toMap(Ciudad::getId, Ciudad::getNombre, (a, b) -> a));
    }

    private SucursalDTO aDTO(Sucursal s, Map<Integer, String> ciudades) {
        return new SucursalDTO(s.getId(), s.getCiudadId(), ciudades.get(s.getCiudadId()), s.getNombre(),
                s.getDireccion(), s.getTelefono(), s.getHoraApertura(), s.getHoraCierre(),
                Boolean.TRUE.equals(s.getActivo()));
    }

    private EmpresaDTO aDTO(EmpresaPerfil e) {
        return new EmpresaDTO(e.getId(), e.getNombre(), e.getRazonSocial(), e.getNit(), e.getDireccion(),
                e.getTelefono(), e.getEmail(), e.getMonedaPrincipal(), e.getMonedaSecundaria(),
                e.getLogoUrl(), e.isActivo());
    }

    private static String vacioANulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private void bitacora(String accion, String actor, String entidad, Long entidadId, String ip) {
        try {
            String responsable = (actor != null && !actor.isBlank()) ? actor : "sistema";
            bitacoraLogRepository.save(new BitacoraLog(accion, responsable, entidad, entidadId, ip));
        } catch (Exception e) {
            log.warn("No se pudo persistir registro en bitacora_logs: {}", e.getMessage());
        }
    }
}
