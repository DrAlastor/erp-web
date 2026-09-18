package modulo.seguridad_y_auditoria.roles_y_permisos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Asignación de un rol a un usuario, con el rastro de quién la hizo y cuándo.
 * Es la entidad intermedia {@code UsuarioRol} del diagrama de clases de la HU-03.
 */
@Entity(name = "RbacUsuarioRol")
@Table(name = "usuario_rol")
public class UsuarioRol {

    @EmbeddedId
    private UsuarioRolId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", insertable = false, updatable = false)
    private Rol rol;

    @Column(name = "asignado_por")
    private Long asignadoPor;

    @Column(name = "asignado_en", nullable = false)
    private OffsetDateTime asignadoEn = OffsetDateTime.now();

    public UsuarioRol() {
    }

    public UsuarioRol(Long usuarioId, UUID rolId, Long asignadoPor) {
        this.id = new UsuarioRolId(usuarioId, rolId);
        this.asignadoPor = asignadoPor;
        this.asignadoEn = OffsetDateTime.now();
    }

    public UsuarioRolId getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public Long getAsignadoPor() {
        return asignadoPor;
    }

    public OffsetDateTime getAsignadoEn() {
        return asignadoEn;
    }
}
