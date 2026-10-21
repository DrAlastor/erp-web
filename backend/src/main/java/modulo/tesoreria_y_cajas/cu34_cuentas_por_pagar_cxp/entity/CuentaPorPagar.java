package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cuota por pagar a un proveedor originada en una compra a credito (tabla
 * {@code cuentas_por_pagar}).
 *
 * <p>Estados almacenados: PENDIENTE (sin pagos), PARCIAL y PAGADA. "Vencida" no se guarda:
 * se deriva de {@code saldo > 0 && fechaVencimiento < hoy}. Compra, proveedor y creador se
 * guardan como ids simples.
 */
@Entity(name = "CuentaPorPagar")
@Table(name = "cuentas_por_pagar")
@Getter
@Setter
@NoArgsConstructor
public class CuentaPorPagar {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String PARCIAL = "PARCIAL";
    public static final String PAGADA = "PAGADA";
    /** Filtro derivado del listado; nunca se guarda en la columna ESTADO. */
    public static final String VENCIDA = "VENCIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compra_id", nullable = false)
    private Long compraId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (estado == null) estado = PENDIENTE;
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
