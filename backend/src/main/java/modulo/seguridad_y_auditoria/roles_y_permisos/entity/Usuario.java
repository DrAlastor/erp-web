package modulo.seguridad_y_auditoria.roles_y_permisos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usuario del sistema. Mínimo a propósito: el CRUD de usuarios es la CU-02 y la
 * autenticación definitiva es la CU-01. Acá está lo que la autorización necesita: a qué
 * empresa pertenece, si está activo y con qué credencial entra.
 */
@org.hibernate.annotations.Immutable
@Entity(name = "RbacUsuario")
@Table(name = "usuarios")
public class Usuario {

    @Id
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "fullname", nullable = false, length = 150)
    private String nombre;

    @Column(name = "enable", nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Usuario() {
    }

    public Usuario(Long id, UUID empresaId, String email, String passwordHash, String nombre) {
        this.id = id;
        this.empresaId = empresaId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = true;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
