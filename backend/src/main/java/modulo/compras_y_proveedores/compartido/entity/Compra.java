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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factura de compra a un proveedor (tabla {@code compras}).
 *
 * <p>Entidad minima compartida: la CU34 la lee para generar las cuotas por pagar y la
 * CU23/CU24 la registrara. Proveedor, sucursal y usuario se guardan como ids simples.
 * Valores de los datos semilla de ERP_Asus: {@code tipoPago} CREDITO / CONTADO y
 * {@code estado} COMPLETADA; la anulacion se representa con ANULADA.
 */
@Entity(name = "Compra")
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    public static final String TIPO_CREDITO = "CREDITO";
    public static final String TIPO_CONTADO = "CONTADO";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_ANULADA = "ANULADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @Column(name = "numero_autorizacion", length = 60)
    private String numeroAutorizacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_pago", nullable = false, length = 30)
    private String tipoPago;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public boolean esCredito() {
        return TIPO_CREDITO.equalsIgnoreCase(tipoPago);
    }

    /** Acepta ANULADA y ANULADO, por si otra rama usa el masculino. */
    public boolean esAnulada() {
        return estado != null && estado.toUpperCase().startsWith("ANULAD");
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
