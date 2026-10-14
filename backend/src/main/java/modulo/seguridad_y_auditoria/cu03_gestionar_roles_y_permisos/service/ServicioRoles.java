package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service;

import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.catalogo.AccionErp;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.catalogo.CatalogoPermisos;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.catalogo.ModuloErp;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.PermisoDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.RolDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.exception.ReglaDeNegocio;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Administración de los roles de una empresa: consultarlos, editar su matriz de permisos y
 * activarlos o desactivarlos, sincronizado directamente con la base de datos ERP_Asus.
 */
@Service
public class ServicioRoles {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private static final Set<String> PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR = Set.of(
            CatalogoPermisos.codigo(ModuloErp.SEGURIDAD, AccionErp.CONSULTAR),
            CatalogoPermisos.codigo(ModuloErp.SEGURIDAD, AccionErp.MODIFICAR));

    private final RolRepository rolRepository;
    private final Map<Integer, Set<String>> matrizPermisosPorRol = new ConcurrentHashMap<>();

    public ServicioRoles(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    /** Roles de la empresa con su matriz de permisos consultados desde la base de datos. */
    @Transactional(readOnly = true)
    public List<RolDto> listar(UUID empresaId) {
        List<Rol> roles = rolRepository.findAll();
        List<RolDto> resultado = new ArrayList<>();
        for (Rol r : roles) {
            Set<String> permisos = obtenerPermisosEfectivos(r);
            resultado.add(new RolDto(
                    String.valueOf(r.getId()),
                    r.getNombre().toUpperCase(),
                    formatearNombre(r.getNombre()),
                    obtenerDescripcion(r.getNombre()),
                    Boolean.TRUE.equals(r.getActivo()),
                    true,
                    permisos));
        }
        return resultado;
    }

    /** Catálogo completo de permisos canónicos, para dibujar las columnas de la matriz. */
    public List<PermisoDto> catalogo() {
        return CatalogoPermisos.TODOS.stream()
                .map(d -> new PermisoDto(
                        d.codigo(),
                        d.modulo().name(),
                        d.accion().name(),
                        d.modulo().etiqueta(),
                        d.accion().etiqueta(),
                        d.descripcion()))
                .toList();
    }

    /** Reemplaza por completo la matriz de permisos de un rol. */
    @Transactional
    public RolDto reemplazarMatriz(UUID empresaId, String rolId, Collection<String> codigos, UsuarioPrincipal actor) {
        Rol rol = buscarRol(rolId);
        validarQueLosPermisosExistan(codigos);
        validarQueElAdministradorConserveLaSeguridad(rol, codigos);

        matrizPermisosPorRol.put(rol.getId(), new TreeSet<>(codigos));

        return new RolDto(
                String.valueOf(rol.getId()),
                rol.getNombre().toUpperCase(),
                formatearNombre(rol.getNombre()),
                obtenerDescripcion(rol.getNombre()),
                Boolean.TRUE.equals(rol.getActivo()),
                true,
                matrizPermisosPorRol.get(rol.getId()));
    }

    /** Activa o desactiva un rol. */
    @Transactional
    public RolDto cambiarEstado(UUID empresaId, String rolId, boolean activo, UsuarioPrincipal actor) {
        Rol rol = buscarRol(rolId);

        if (!activo && rol.getNombre().toUpperCase().contains(ROL_ADMINISTRADOR)) {
            throw new ReglaDeNegocio("ADMINISTRADOR_NO_SE_DESACTIVA",
                    "El rol Administrador no se puede desactivar: dejaría al sistema sin quien administre "
                            + "los roles y permisos");
        }

        rol.setActivo(activo);
        rolRepository.save(rol);

        return new RolDto(
                String.valueOf(rol.getId()),
                rol.getNombre().toUpperCase(),
                formatearNombre(rol.getNombre()),
                obtenerDescripcion(rol.getNombre()),
                Boolean.TRUE.equals(rol.getActivo()),
                true,
                obtenerPermisosEfectivos(rol));
    }

    private Rol buscarRol(String rolId) {
        try {
            Integer id = Integer.parseInt(rolId);
            return rolRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO", "No existe ese rol"));
        } catch (NumberFormatException e) {
            return rolRepository.findByNombre(rolId)
                    .orElseThrow(() -> new RecursoNoEncontrado("ROL_NO_ENCONTRADO", "No existe ese rol"));
        }
    }

    public Set<String> obtenerPermisosEfectivos(Rol rol) {
        if (matrizPermisosPorRol.containsKey(rol.getId())) {
            return matrizPermisosPorRol.get(rol.getId());
        }
        Set<String> perms = new TreeSet<>();
        String n = rol.getNombre().toUpperCase();
        if (n.contains("ADMIN") || n.contains("SUPER")) {
            perms.addAll(CatalogoPermisos.codigos());
        } else if (n.contains("VENTA")) {
            perms.addAll(List.of(
                    "COMERCIAL_CREAR", "COMERCIAL_CONSULTAR", "COMERCIAL_MODIFICAR",
                    "INVENTARIO_CONSULTAR",
                    "FACTURACION_CREAR", "FACTURACION_CONSULTAR",
                    "REPORTES_CONSULTAR"
            ));
        } else if (n.contains("CAJA")) {
            perms.addAll(List.of(
                    "COMERCIAL_CREAR", "COMERCIAL_CONSULTAR",
                    "INVENTARIO_CONSULTAR",
                    "FACTURACION_CREAR", "FACTURACION_CONSULTAR", "FACTURACION_ANULAR"
            ));
        } else if (n.contains("ALMACEN")) {
            perms.addAll(List.of(
                    "COMERCIAL_CONSULTAR",
                    "INVENTARIO_CREAR", "INVENTARIO_CONSULTAR", "INVENTARIO_MODIFICAR",
                    "REPORTES_CONSULTAR"
            ));
        } else {
            perms.addAll(List.of("COMERCIAL_CONSULTAR", "INVENTARIO_CONSULTAR"));
        }
        matrizPermisosPorRol.put(rol.getId(), perms);
        return perms;
    }

    private String formatearNombre(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    private String obtenerDescripcion(String raw) {
        String n = raw != null ? raw.toUpperCase() : "";
        if (n.contains("ADMIN")) return "Control total del sistema, incluida la gestión de roles y permisos";
        if (n.contains("VENTA")) return "Gestión comercial, cotizaciones, pedidos y ventas de tienda";
        if (n.contains("CAJA")) return "Cobros en caja POS, emisión de facturas y control de turnos";
        if (n.contains("ALMACEN")) return "Control de existencias, recepción y movimientos de inventario";
        return "Rol operativo del sistema ERP";
    }

    private void validarQueLosPermisosExistan(Collection<String> codigos) {
        for (String codigo : codigos) {
            if (!CatalogoPermisos.existe(codigo)) {
                throw new ReglaDeNegocio("PERMISO_DESCONOCIDO",
                        "El permiso " + codigo + " no existe en el catálogo del ERP");
            }
        }
    }

    private void validarQueElAdministradorConserveLaSeguridad(Rol rol, Collection<String> codigos) {
        if (!rol.getNombre().toUpperCase().contains(ROL_ADMINISTRADOR)) {
            return;
        }
        if (!codigos.containsAll(PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR)) {
            throw new ReglaDeNegocio("ADMINISTRADOR_SIN_SEGURIDAD",
                    "El rol Administrador debe conservar los permisos "
                            + String.join(" y ", PERMISOS_IRRENUNCIABLES_DEL_ADMINISTRADOR)
                            + ": sin ellos nadie podría volver a administrar los roles");
        }
    }
}
