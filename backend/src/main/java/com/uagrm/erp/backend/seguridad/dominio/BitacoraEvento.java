package com.uagrm.erp.backend.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento de auditoría. El diagrama de actividad de la HU-03 exige dejar registrado el
 * cambio al asignar un rol; la CU-04 del Sprint 3 extiende esta bitácora y le agrega la
 * consulta.
 */
@Entity
@Table(name = "bitacora_auditoria")
public class BitacoraEvento {

    /** Acciones que registra el módulo de seguridad. */
    public static final String ROL_ASIGNADO = "ROL_ASIGNADO";
    public static final String ROL_QUITADO = "ROL_QUITADO";
    public static final String MATRIZ_MODIFICADA = "MATRIZ_MODIFICADA";
    public static final String ROL_ESTADO_CAMBIADO = "ROL_ESTADO_CAMBIADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 500)
    private String detalle;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn = OffsetDateTime.now();

    public BitacoraEvento() {
    }

    public BitacoraEvento(UUID empresaId, UUID usuarioId, String accion, String detalle) {
        this.empresaId = empresaId;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.detalle = detalle;
        this.registradoEn = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }
}
