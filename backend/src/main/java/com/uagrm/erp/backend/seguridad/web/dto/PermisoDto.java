package com.uagrm.erp.backend.seguridad.web.dto;

import com.uagrm.erp.backend.seguridad.dominio.Permiso;

/**
 * Un permiso del catálogo tal como lo consume la pantalla de Roles y Permisos. Incluye las
 * etiquetas legibles para no tener que traducir los códigos en el frontend.
 */
public record PermisoDto(
        String codigo,
        String modulo,
        String accion,
        String etiquetaModulo,
        String etiquetaAccion,
        String descripcion) {

    public static PermisoDto de(Permiso permiso) {
        return new PermisoDto(
                permiso.getCodigo(),
                permiso.getModulo().name(),
                permiso.getAccion().name(),
                permiso.getModulo().etiqueta(),
                permiso.getAccion().etiqueta(),
                permiso.getDescripcion());
    }
}
