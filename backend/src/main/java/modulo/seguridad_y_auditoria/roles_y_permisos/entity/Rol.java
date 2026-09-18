package modulo.seguridad_y_auditoria.roles_y_permisos.entity;

import modulo.seguridad_y_auditoria.roles_y_permisos.catalogo.RolDeSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Rol de una empresa con su matriz de permisos.
 *
 * <p>Los siete roles del documento de la HU-03 se siembran con {@code esSistema = true}:
 * no se borran, pero el administrador sí puede editar su matriz y activarlos o
 * desactivarlos. Un rol inactivo no otorga ningún permiso, aunque siga asignado.
 */
@Entity(name = "RbacRol")
@Table(name = "rol")
public class Rol {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new LinkedHashSet<>();

    public Rol() {
    }

    /** Siembra un rol de sistema en una empresa a partir de la matriz del documento. */
    public Rol(UUID empresaId, RolDeSistema definicion, Set<Permiso> permisos) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.codigo = definicion.codigo();
        this.nombre = definicion.nombre();
        this.descripcion = definicion.descripcion();
        this.activo = true;
        this.esSistema = true;
        this.permisos = new LinkedHashSet<>(permisos);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isEsSistema() {
        return esSistema;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }

    /** Reemplaza por completo la matriz de permisos del rol. */
    public void reemplazarPermisos(Set<Permiso> nuevos) {
        this.permisos.clear();
        this.permisos.addAll(nuevos);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Rol rol && id != null && id.equals(rol.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
