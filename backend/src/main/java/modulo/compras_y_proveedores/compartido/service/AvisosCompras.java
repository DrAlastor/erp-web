package modulo.compras_y_proveedores.compartido.service;

import modulo.notificaciones.service.NotificacionService;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * Rastro y avisos de Compras y Proveedores: la bitácora (CU08) y las notificaciones.
 *
 * <ul>
 *   <li><b>Bitácora:</b> es obligatoria y va en la misma transacción que la operación. Si no
 *       se puede escribir, la operación entera se revierte: no queda un cambio sin rastro.</li>
 *   <li><b>Notificaciones:</b> son un aviso, no parte del negocio. Se mandan después del
 *       commit y en su propia transacción, así un fallo al notificar no deshace la operación
 *       (y no se avisa de algo que al final se revirtió).</li>
 * </ul>
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
    private final TransactionTemplate aparte;

    public AvisosCompras(BitacoraLogRepository bitacora, NotificacionService notificaciones, JdbcTemplate jdbc,
                         PlatformTransactionManager transacciones) {
        this.bitacora = bitacora;
        this.notificaciones = notificaciones;
        this.jdbc = jdbc;
        this.aparte = new TransactionTemplate(transacciones);
        this.aparte.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Deja la operación en bitacora_logs: acción, responsable, tabla, id e IP. */
    public void registrar(String accion, Solicitante quien, String entidad, Integer entidadId) {
        bitacora.save(new BitacoraLog(accion, quien.actor(), entidad, entidadId.longValue(), quien.ip()));
    }

    /**
     * Avisa a los administradores activos (por ejemplo, que hay una orden por aprobar).
     *
     * @param excepto usuario que no necesita el aviso (quien hizo la operación), o {@code null}
     */
    public void avisarAdministradores(String titulo, String mensaje, Long excepto) {
        alConfirmar(() -> jdbc.queryForList(ADMINISTRADORES, Long.class).stream()
                .filter(id -> !Objects.equals(id, excepto))
                .forEach(id -> notificaciones.notificar(id, titulo, mensaje, "INFO")), titulo);
    }

    /** Avisa a un usuario puntual (por ejemplo, a quien registró la orden). */
    public void avisarUsuario(Long usuarioId, String titulo, String mensaje) {
        if (usuarioId == null) return;
        alConfirmar(() -> notificaciones.notificar(usuarioId, titulo, mensaje, "INFO"), titulo);
    }

    /** Ejecuta el aviso cuando la transacción en curso se confirma; si no hay una, enseguida. */
    private void alConfirmar(Runnable aviso, String titulo) {
        Runnable seguro = () -> {
            try {
                aparte.executeWithoutResult(estado -> aviso.run());
            } catch (Exception e) {
                log.warn("No se pudo enviar la notificación '{}': {}", titulo, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    seguro.run();
                }
            });
        } else {
            seguro.run();
        }
    }
}
