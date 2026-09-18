package modulo.seguridad_y_auditoria.roles_y_permisos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Cuerpos de las peticiones de la pantalla de Roles y Permisos. */
public final class Peticiones {

    private Peticiones() {
    }

    /**
     * Matriz completa de un rol. Se envía la lista entera y reemplaza a la anterior: no hay
     * "agregar" ni "quitar" permiso suelto, para que dos administradores editando a la vez
     * no dejen una matriz a medias.
     */
    public record Matriz(@NotNull(message = "La lista de permisos es obligatoria") List<String> permisos) {
    }

    /** Activación o desactivación de un rol. */
    public record Estado(@NotNull(message = "El estado es obligatorio") Boolean activo) {
    }

    /** Asignación de un rol a un usuario. */
    public record Asignacion(@NotNull(message = "El rol es obligatorio") UUID rolId) {
    }
}
