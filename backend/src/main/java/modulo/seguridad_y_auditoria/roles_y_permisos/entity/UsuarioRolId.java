package modulo.seguridad_y_auditoria.roles_y_permisos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de la asignación: es la que impide asignar dos veces el mismo rol. */
@Embeddable
public class UsuarioRolId implements Serializable {

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    public UsuarioRolId() {
    }

    public UsuarioRolId(Long usuarioId, UUID rolId) {
        this.usuarioId = usuarioId;
        this.rolId = rolId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public UUID getRolId() {
        return rolId;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof UsuarioRolId clave
                && Objects.equals(usuarioId, clave.usuarioId)
                && Objects.equals(rolId, clave.rolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rolId);
    }
}
