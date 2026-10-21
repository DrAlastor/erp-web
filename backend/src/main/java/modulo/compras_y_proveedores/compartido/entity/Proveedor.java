package modulo.compras_y_proveedores.compartido.entity;

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

import java.time.LocalDateTime;

/**
 * Proveedor de mercaderia (tabla {@code proveedores}).
 *
 * <p>Entidad minima compartida del modulo de compras: la usa la CU34 (cuentas por pagar)
 * y la reutilizaran la CU21 (gestionar proveedores) y la CU23 (ordenes de compra).
 * {@code diasCredito} da el vencimiento por defecto de las cuotas por pagar.
 */
@Entity(name = "Proveedor")
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "condicion_pago", length = 50)
    private String condicionPago;

    @Column(name = "dias_credito")
    private Integer diasCredito;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = Boolean.TRUE;
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
