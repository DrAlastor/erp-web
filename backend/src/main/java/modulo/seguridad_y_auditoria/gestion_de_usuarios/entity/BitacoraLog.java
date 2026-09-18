package modulo.seguridad_y_auditoria.gestion_de_usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de auditoría de la CU-02; mapea la tabla {@code bitacora_logs} de la migración V3.
 *
 * <p>Cada cambio efectivo sobre una cuenta deja aquí la acción, el responsable, la entidad
 * afectada, su identificador y la IP de origen. Se escribe dentro de la misma transacción
 * que el cambio: si la operación se revierte, el rastro tampoco queda.
 */
@Entity
@Table(name = "bitacora_logs")
@Getter
@NoArgsConstructor
public class BitacoraLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario_responsable", nullable = false, length = 50)
    private String usuarioResponsable;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    public BitacoraLog(String accion, String actor, Long usuarioId, String ip) {
        this.accion = accion;
        this.usuarioResponsable = actor;
        this.entidadId = usuarioId;
        this.ipOrigen = ip;
        this.entidad = "usuarios";
        this.fechaHora = LocalDateTime.now();
    }
}
