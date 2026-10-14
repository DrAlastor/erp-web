package modulo.seguridad_y_auditoria.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "empleado_id")
    private Long empleadoId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "username", length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "nivel_usuario", length = 30)
    private String nivelUsuario = "GERENCIA";

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Campos transitorios para el servicio de seguridad (bloqueo / reintentos / compatibilidad)
    @Transient
    private Integer intentosFallidos = 0;

    @Transient
    private LocalDateTime bloqueadoHasta;

    @Transient
    private java.util.UUID empresaId;

    @Transient
    private String createdBy;

    @Transient
    private String updatedBy;

    @Transient
    private Boolean rbacInicializado = true;

    @Column(name = "fullname", length = 150)
    private String fullname;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", insertable = false, updatable = false)
    private modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Empleado empleado;

    // Métodos de compatibilidad requeridos por el servicio de autenticación y JWT
    public String getUsername() {
        if (this.username != null && !this.username.isBlank()) {
            return this.username;
        }
        return this.email != null ? this.email : "";
    }

    public void setUsername(String username) {
        this.username = username;
        if (this.email == null || this.email.isBlank()) {
            this.email = username;
        }
    }

    public String getFullname() {
        if (this.fullname != null && !this.fullname.trim().isBlank()) {
            return this.fullname.trim();
        }
        if (this.empleado != null && this.empleado.getNombreCompleto() != null && !this.empleado.getNombreCompleto().trim().isBlank()) {
            return this.empleado.getNombreCompleto().trim();
        }
        if (this.email != null && this.email.contains("@")) {
            String prefix = this.email.split("@")[0];
            return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
        }
        return "Usuario del Sistema";
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Boolean getEnable() {
        return Boolean.TRUE.equals(this.activo);
    }

    public void setEnable(Boolean enable) {
        this.activo = enable;
    }

    public Set<Rol> getRoles() {
        return this.rol != null ? Set.of(this.rol) : Set.of();
    }

    public java.util.UUID getEmpresaId() {
        return this.empresaId;
    }

    public void setEmpresaId(java.util.UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Boolean getRbacInicializado() {
        return this.rbacInicializado;
    }

    public void setRbacInicializado(Boolean rbacInicializado) {
        this.rbacInicializado = rbacInicializado;
    }

    public LocalDateTime getCreatedAt() {
        return this.fechaCreacion != null ? this.fechaCreacion : LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return this.fechaCreacion != null ? this.fechaCreacion : LocalDateTime.now();
    }
}
