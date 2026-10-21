package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Compra registrada a un proveedor. Mapea la tabla {@code compras} del esquema base. */
@Entity
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    public static final String REGISTRADA = "REGISTRADA";
    /** Estado con que ya vienen las compras cargadas antes de la CU23; se trata igual que REGISTRADA. */
    public static final String COMPLETADA = "COMPLETADA";
    public static final String DEVUELTA_PARCIAL = "DEVUELTA_PARCIAL";
    public static final String DEVUELTA = "DEVUELTA";
    public static final String ANULADA = "ANULADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "proveedor_id", nullable = false)
    private Integer proveedorId;

    @Column(name = "sucursal_id", nullable = false)
    private Integer sucursalId;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "orden_compra_id")
    private Integer ordenCompraId;

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
    private String estado = REGISTRADA;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompraDetalle> detalles = new ArrayList<>();

    /** La compra esta vigente: se puede devolver y, si no tiene devoluciones, anular. */
    public boolean estaVigente() {
        return REGISTRADA.equals(estado) || COMPLETADA.equals(estado);
    }

    public void agregarDetalle(CompraDetalle detalle) {
        detalle.setCompra(this);
        detalles.add(detalle);
    }
}
