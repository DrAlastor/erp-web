package com.uagrm.erp.backend.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Empresa (inquilino del SaaS). Mínima a propósito: el registro de empresas es de la
 * CU-02; acá solo existe lo necesario para que un usuario pertenezca a una empresa y la
 * autorización quede aislada por inquilino.
 */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String nit;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public Empresa() {
    }

    public Empresa(String nombre, String nit) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.nit = nit;
        this.activo = true;
        this.creadoEn = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNit() {
        return nit;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
