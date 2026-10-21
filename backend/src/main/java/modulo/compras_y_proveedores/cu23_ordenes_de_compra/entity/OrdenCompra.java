package modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra a un proveedor, mapeada a la tabla ORDENES_COMPRA de ERP_Asus.
 *
 * <p>Es un pedido formal, no una compra: no mueve stock ni genera deuda. Eso lo hace la
 * compra que la cumple (CU23/CU24), que la deja RECIBIDA.
 */
@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "numero_orden", nullable = false, unique = true, length = 50)
    private String numeroOrden;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoOrdenCompra estado = EstadoOrdenCompra.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrdenCompraDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(OrdenCompraDetalle detalle) {
        detalle.setOrden(this);
        detalles.add(detalle);
    }

    /** subtotal = Σ subtotales de los renglones; total = subtotal − descuento. */
    public void calcularTotales(BigDecimal descuentoGlobal) {
        BigDecimal suma = BigDecimal.ZERO;
        for (OrdenCompraDetalle d : detalles) {
            d.calcularSubtotal();
            suma = suma.add(d.getSubtotal());
        }
        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        descuento = (descuentoGlobal == null ? BigDecimal.ZERO : descuentoGlobal).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.subtract(descuento);
    }

    /** Agrega una línea a las observaciones sin perder lo que ya había. */
    public void anotar(String linea) {
        observaciones = observaciones == null || observaciones.isBlank()
                ? linea
                : observaciones + "\n" + linea;
    }

    @PrePersist
    protected void alCrear() {
        if (fecha == null) fecha = LocalDate.now();
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
