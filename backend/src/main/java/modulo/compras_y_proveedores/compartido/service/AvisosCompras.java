package modulo.compras_y_proveedores.compartido.service;

import modulo.notificaciones.service.NotificacionService;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rastro y avisos de Compras y Proveedores: la bitácora (CU08) y las notificaciones.
 *
 * <p>Ninguno de los dos debe tumbar la operación de negocio: si la bitácora o la
 * notificación fallan, se deja constancia en el log y la operación sigue.
 */
@Component
public class AvisosCompras {

    private static final Logger log = LoggerFactory.getLogger(AvisosCompras.class);

    private static final String ADMINISTRADORES =
            "select u.id from usuario u join rol r on r.id = u.rol_id "
            + "where u.activo = true and upper(r.nombre) in ('ADMIN', 'ADMINISTRADOR', 'SUPERADMIN')";

    private final BitacoraLogRepository bitacora;
    private final NotificacionService notificaciones;
    private final JdbcTemplate jdbc;

    public AvisosCompras(BitacoraLogRepository bitacora, NotificacionService notificaciones, JdbcTemplate jdbc) {
        this.bitacora = bitacora;
        this.notificaciones = notificaciones;
        this.jdbc = jdbc;
    }

    /** Deja la operación en bitacora_logs: acción, responsable, tabla, id e IP. */
    public void registrar(String accion, Solicitante quien, String entidad, Integer entidadId) {
        try {
            bitacora.save(new BitacoraLog(accion, quien.actor(), entidad, entidadId.longValue(), quien.ip()));
        } catch (Exception e) {
            log.warn("No se pudo registrar {} de {} #{} en la bitácora: {}", accion, entidad, entidadId, e.getMessage());
        }
    }

    /** Avisa a todos los administradores activos (por ejemplo, que hay una orden por aprobar). */
    public void avisarAdministradores(String titulo, String mensaje) {
        List<Long> ids;
        try {
            ids = jdbc.queryForList(ADMINISTRADORES, Long.class);
        } catch (Exception e) {
            log.warn("No se pudo obtener los administradores para avisar '{}': {}", titulo, e.getMessage());
            return;
        }
        ids.forEach(id -> notificaciones.notificar(id, titulo, mensaje, "INFO"));
    }

    /** Avisa a un usuario puntual (por ejemplo, a quien registró la orden). */
    public void avisarUsuario(Long usuarioId, String titulo, String mensaje) {
        if (usuarioId != null) notificaciones.notificar(usuarioId, titulo, mensaje, "INFO");
    }
}
