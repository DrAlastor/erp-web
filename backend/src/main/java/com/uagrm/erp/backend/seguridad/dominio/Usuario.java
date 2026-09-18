package com.uagrm.erp.backend.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Usuario del sistema. Mínimo a propósito: el CRUD de usuarios es la CU-02 y la
 * autenticación definitiva es la CU-01. Acá está lo que la autorización necesita: a qué
 * empresa pertenece, si está activo y con qué credencial entra.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public Usuario() {
    }

    public Usuario(UUID empresaId, String email, String passwordHash, String nombre) {
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = true;
        this.creadoEn = OffsetDateTime.now();
    }

    public UUID getId() {
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

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
