package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service;

import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.catalogo.CatalogoPermisos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de autorización del ERP: la fuente de verdad unificada para resolver
 * y validar los permisos efectivos de cada usuario y rol en el sistema.
 */
@Service
public class ServicioAutorizacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutorizacion.class);

    private final UsuarioRepository usuarioRepository;
    private final ServicioRoles servicioRoles;
    private final JdbcTemplate jdbcTemplate;

    /** Permisos efectivos cacheados por usuario. */
    private final Map<Long, Set<String>> cachePorUsuario = new ConcurrentHashMap<>();

    public ServicioAutorizacion(UsuarioRepository usuarioRepository,
                                ServicioRoles servicioRoles,
                                JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.servicioRoles = servicioRoles;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Permisos efectivos del usuario: la unión de los permisos de sus roles activos.
     */
    @Transactional(readOnly = true)
    public Set<String> permisosEfectivos(Long usuarioId) {
        if (usuarioId == null) {
            return Set.of();
        }
        return cachePorUsuario.computeIfAbsent(usuarioId, this::leerDeLaBase);
    }

    /** Indica si el usuario tiene un permiso concreto, por ejemplo {@code INVENTARIO_CONSULTAR}. */
    @Transactional(readOnly = true)
    public boolean tienePermiso(Long usuarioId, String codigoPermiso) {
        if (usuarioId == null || codigoPermiso == null || codigoPermiso.isBlank()) {
            return false;
        }
        Set<String> permisos = permisosEfectivos(usuarioId);
        if (permisos.contains("ALL")) {
            return true;
        }
        if (permisos.contains(codigoPermiso)) {
            return true;
        }
        for (String p : permisos) {
            if (p.equalsIgnoreCase(codigoPermiso)) {
                return true;
            }
        }
        return false;
    }

    /** Descarta los permisos cacheados de un usuario. */
    public void invalidarCache(Long usuarioId) {
        if (usuarioId != null) {
            cachePorUsuario.remove(usuarioId);
        }
    }

    /** Descarta los permisos cacheados de todos los usuarios que tengan un rol. */
    @Transactional(readOnly = true)
    public void invalidarCacheDeRol(UUID rolId) {
        cachePorUsuario.clear();
    }

    /** Descarta toda la caché. */
    public void invalidarTodo() {
        cachePorUsuario.clear();
    }

    private Set<String> leerDeLaBase(Long usuarioId) {
        Set<String> perms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return Set.of();
        }

        Rol rol = usuario.getRol();
        if (rol == null || Boolean.FALSE.equals(rol.getActivo())) {
            return Set.of();
        }

        String rolNombre = rol.getNombre() != null ? rol.getNombre().toUpperCase() : "";
        String rolPermiso = rol.getPermiso() != null ? rol.getPermiso().trim().toUpperCase() : "";

        // 1. Administrador o permiso global ALL
        if (rolNombre.contains("ADMIN") || rolNombre.contains("SUPER") || "ALL".equalsIgnoreCase(rolPermiso)) {
            perms.add("ALL");
            perms.addAll(CatalogoPermisos.codigos());
            perms.addAll(List.of(
                    "VENTAS_ONLY", "INVENTARIO_ONLY", "CAJA_ONLY",
                    "VENTAS_CONSULTAR", "VENTAS_REGISTRAR",
                    "COMPRAS_CONSULTAR", "COMPRAS_REGISTRAR",
                    "CONTABILIDAD_REGISTRAR", "USUARIOS_CONSULTAR", "ROLES_CONSULTAR"
            ));
            return Collections.unmodifiableSet(perms);
        }

        // 2. Permisos canónicos desde ServicioRoles (incluye matrices personalizadas)
        try {
            perms.addAll(servicioRoles.obtenerPermisosEfectivos(rol));
        } catch (Exception ex) {
            log.debug("No se pudieron obtener permisos de ServicioRoles: {}", ex.getMessage());
        }

        // 3. Permiso explícito de la columna rol.permiso
        if (!rolPermiso.isBlank()) {
            perms.add(rolPermiso);
        }

        // 4. Mapeo específico de la tabla rol_funcion de la base de datos
        try {
            String sql = "SELECT f.nombre, rf.descripcion FROM rol_funcion rf " +
                         "JOIN funcion f ON rf.funcion_id = f.id WHERE rf.rol_id = ?";
            jdbcTemplate.query(sql, (rs) -> {
                String funcion = rs.getString(1);
                String desc = rs.getString(2);
                boolean edicion = "Edicion".equalsIgnoreCase(desc);
                mapearFuncionAPermisos(funcion, edicion, perms);
            }, rol.getId());
        } catch (Exception ex) {
            log.debug("Nota sobre consulta rol_funcion: {}", ex.getMessage());
        }

        // 5. Garantía de acceso operativo esencial:
        // Todo vendedor, cajero o almacenero necesita consultar catálogo y artículos
        perms.add("INVENTARIO_CONSULTAR");
        perms.add("COMERCIAL_CONSULTAR");

        if (rolNombre.contains("VENTA") || "VENTAS_ONLY".equalsIgnoreCase(rolPermiso)) {
            perms.addAll(List.of(
                    "VENTAS_ONLY", "VENTAS_CONSULTAR", "VENTAS_REGISTRAR",
                    "COMERCIAL_CONSULTAR", "COMERCIAL_CREAR", "COMERCIAL_MODIFICAR",
                    "INVENTARIO_CONSULTAR",
                    "FACTURACION_CONSULTAR", "FACTURACION_CREAR",
                    "REPORTES_CONSULTAR"
            ));
        } else if (rolNombre.contains("CAJA") || "CAJA_ONLY".equalsIgnoreCase(rolPermiso)) {
            perms.addAll(List.of(
                    "CAJA_ONLY", "VENTAS_CONSULTAR", "VENTAS_REGISTRAR",
                    "COMERCIAL_CONSULTAR", "COMERCIAL_CREAR",
                    "INVENTARIO_CONSULTAR",
                    "FACTURACION_CONSULTAR", "FACTURACION_CREAR", "FACTURACION_ANULAR",
                    "CONTABILIDAD_CONSULTAR", "CONTABILIDAD_MODIFICAR"
            ));
        } else if (rolNombre.contains("ALMACEN") || "INVENTARIO_ONLY".equalsIgnoreCase(rolPermiso)) {
            perms.addAll(List.of(
                    "INVENTARIO_ONLY",
                    "INVENTARIO_CONSULTAR", "INVENTARIO_CREAR", "INVENTARIO_MODIFICAR", "INVENTARIO_ANULAR", "INVENTARIO_ELIMINAR",
                    "COMERCIAL_CONSULTAR",
                    "REPORTES_CONSULTAR"
            ));
        }

        return Collections.unmodifiableSet(perms);
    }

    private void mapearFuncionAPermisos(String funcion, boolean edicion, Set<String> perms) {
        if (funcion == null) return;
        String f = funcion.toLowerCase();

        if (f.contains("catalogo") || f.contains("articulos")) {
            perms.add("INVENTARIO_CONSULTAR");
            if (edicion) {
                perms.add("INVENTARIO_CREAR");
                perms.add("INVENTARIO_MODIFICAR");
                perms.add("INVENTARIO_ELIMINAR");
            }
        } else if (f.contains("inventario") || f.contains("kardex") || f.contains("almacen")) {
            perms.add("INVENTARIO_CONSULTAR");
            if (edicion) {
                perms.add("INVENTARIO_CREAR");
                perms.add("INVENTARIO_MODIFICAR");
            }
        } else if (f.contains("despacho")) {
            perms.add("INVENTARIO_CONSULTAR");
            if (edicion) {
                perms.add("INVENTARIO_CREAR");
                perms.add("INVENTARIO_MODIFICAR");
            }
        } else if (f.contains("proveedor") || f.contains("compra")) {
            // Va antes que ventas: "Registrar Compra y Devolucion de Proveedores" contiene
            // "devolucion" y no debe dar permisos de facturación.
            perms.add("INVENTARIO_CONSULTAR");
            if (edicion) {
                perms.add("INVENTARIO_CREAR");
                perms.add("INVENTARIO_MODIFICAR");
            }
        } else if (f.contains("cliente") || f.contains("precio") || f.contains("cotizacion") || f.contains("pedido")) {
            perms.add("COMERCIAL_CONSULTAR");
            if (edicion) {
                perms.add("COMERCIAL_CREAR");
                perms.add("COMERCIAL_MODIFICAR");
            }
        } else if (f.contains("venta") || f.contains("facturacion") || f.contains("credito") || f.contains("devolucion")) {
            perms.add("FACTURACION_CONSULTAR");
            perms.add("COMERCIAL_CONSULTAR");
            if (edicion) {
                perms.add("FACTURACION_CREAR");
                perms.add("FACTURACION_MODIFICAR");
                perms.add("FACTURACION_ANULAR");
                perms.add("COMERCIAL_CREAR");
            }
        } else if (f.contains("caja")) {
            perms.add("FACTURACION_CONSULTAR");
            perms.add("COMERCIAL_CONSULTAR");
            perms.add("INVENTARIO_CONSULTAR");
            if (edicion) {
                perms.add("FACTURACION_CREAR");
                perms.add("COMERCIAL_CREAR");
            }
        } else if (f.contains("cuenta") || f.contains("contable") || f.contains("asiento") || f.contains("financiera")) {
            perms.add("CONTABILIDAD_CONSULTAR");
            if (edicion) {
                perms.add("CONTABILIDAD_CREAR");
                perms.add("CONTABILIDAD_MODIFICAR");
            }
        } else if (f.contains("reporte") || f.contains("dashboard")) {
            perms.add("REPORTES_CONSULTAR");
        } else if (f.contains("usuario") || f.contains("rol") || f.contains("empleado") || f.contains("bitacora") || f.contains("configuracion") || f.contains("backup")) {
            perms.add("SEGURIDAD_CONSULTAR");
            if (edicion) {
                perms.add("SEGURIDAD_CREAR");
                perms.add("SEGURIDAD_MODIFICAR");
                perms.add("SEGURIDAD_ELIMINAR");
            }
        }
    }
}
