package modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proveedor de la empresa, mapeado a la tabla PROVEEDORES de ERP_Asus.
 *
 * <p>Nunca se borra: ORDENES_COMPRA, COMPRAS y CUENTAS_POR_PAGAR lo referencian con
 * ON DELETE CASCADE, así que un DELETE se llevaría todo su historial. Se desactiva.
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nit_ci", nullable = false, unique = true, length = 30)
    private String nitCi;

    @Column(length = 100)
    private String contacto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Convert(converter = CondicionPagoConverter.class)
    @Column(name = "condicion_pago", length = 50)
    private CondicionPago condicionPago = CondicionPago.CONTADO;

    @Column(name = "dias_credito")
    private Integer diasCredito = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public void desactivar() {
        activo = false;
    }

    public void reactivar() {
        activo = true;
    }

    /** Fecha en que vence una compra hecha hoy, según los días de crédito pactados. */
    public LocalDate fechaVencimiento(LocalDate fechaCompra) {
        return condicionPago == CondicionPago.CREDITO && diasCredito != null
                ? fechaCompra.plusDays(diasCredito)
                : fechaCompra;
    }

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (activo == null) activo = true;
    }
}
