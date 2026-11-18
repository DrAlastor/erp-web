package modulo.contabilidad_y_finanzas.compartido;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Control por función para las operaciones de escritura de la CU27 y la CU29.
 *
 * <p>El evaluador de la CU03 traduce cualquier función de Contabilidad con «Edición» a
 * CONTABILIDAD_CREAR/MODIFICAR para todo el módulo: un Cajero con Edición en «Cuentas por
 * Cobrar y Cobros» quedaría habilitado también para registrar asientos. Aquí se exige,
 * además del permiso del módulo, Edición en la función concreta (o rol administrador),
 * tal como el administrador la asignó en ROL_FUNCION.
 */
@Component
@RequiredArgsConstructor
public class PermisoFuncionContable {

    public static final String ASIENTOS = "Gestionar Asientos Contables";
    public static final String CUENTAS_POR_COBRAR = "Gestionar Cuentas por Cobrar y Cobros";

    private final NamedParameterJdbcTemplate jdbc;

    public void exigirEdicion(Long usuarioId, String funcion) {
        if (!puedeEditar(usuarioId, funcion)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Su rol no tiene permiso de edición en «" + funcion + "».");
        }
    }

    /** Rol administrador (o permiso global ALL): el único que recibe CONTABILIDAD_ANULAR. */
    public boolean esAdministrador(Long usuarioId) {
        List<Map<String, Object>> rol = rol(usuarioId);
        return !rol.isEmpty() && administrador(rol.get(0));
    }

    public boolean puedeEditar(Long usuarioId, String funcion) {
        List<Map<String, Object>> rol = rol(usuarioId);
        if (rol.isEmpty()) return false;
        if (administrador(rol.get(0))) return true;
        Long concedidas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rol_funcion rf JOIN funcion f ON f.id = rf.funcion_id "
                        + "WHERE rf.rol_id = :rol AND LOWER(f.nombre) = LOWER(:funcion) "
                        + "AND LOWER(rf.descripcion) = 'edicion'",
                new MapSqlParameterSource("rol", rol.get(0).get("id")).addValue("funcion", funcion),
                Long.class);
        return concedidas != null && concedidas > 0;
    }

    private List<Map<String, Object>> rol(Long usuarioId) {
        return jdbc.queryForList(
                "SELECT r.id, r.nombre, r.permiso FROM usuario u JOIN rol r ON r.id = u.rol_id "
                        + "WHERE u.id = :id AND COALESCE(r.activo, TRUE) = TRUE",
                new MapSqlParameterSource("id", usuarioId));
    }

    private static boolean administrador(Map<String, Object> rol) {
        String nombre = texto(rol.get("nombre"));
        return nombre.contains("ADMIN") || nombre.contains("SUPER") || "ALL".equals(texto(rol.get("permiso")));
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim().toUpperCase(Locale.ROOT);
    }
}
