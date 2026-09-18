package com.uagrm.erp.backend.seguridad.dominio;

import com.uagrm.erp.backend.seguridad.catalogo.AccionErp;
import com.uagrm.erp.backend.seguridad.catalogo.DefinicionPermiso;
import com.uagrm.erp.backend.seguridad.catalogo.ModuloErp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Permiso del catálogo, persistido. Su definición canónica vive en
 * {@link com.uagrm.erp.backend.seguridad.catalogo.CatalogoPermisos}; esta entidad es el
 * reflejo de esa definición en la base de datos.
 */
@Entity
@Table(name = "permiso")
public class Permiso {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuloErp modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionErp accion;

    @Column(nullable = false, length = 200)
    private String descripcion;

    public Permiso() {
    }

    public Permiso(DefinicionPermiso definicion) {
        this.id = UUID.randomUUID();
        this.codigo = definicion.codigo();
        this.modulo = definicion.modulo();
        this.accion = definicion.accion();
        this.descripcion = definicion.descripcion();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public ModuloErp getModulo() {
        return modulo;
    }

    public AccionErp getAccion() {
        return accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Permiso permiso && id != null && id.equals(permiso.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
