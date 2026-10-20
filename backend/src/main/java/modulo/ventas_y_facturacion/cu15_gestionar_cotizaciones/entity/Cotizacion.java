package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity;

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

import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cotización (proforma) a un cliente, mapeada a la tabla COTIZACIONES de ERP_Asus.
 *
 * <p>Es sólo una oferta: no reserva ni descuenta stock. El inventario se compromete
 * cuando se convierte en pedido y se despacha.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
@NoArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(name = "numero_cotizacion", nullable = false, unique = true, length = 50)
    private String numeroCotizacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "validez_dias", nullable = false)
    private Integer validezDias;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCotizacion estado = EstadoCotizacion.EMITIDA;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CotizacionDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(CotizacionDetalle detalle) {
        detalle.setCotizacion(this);
        detalles.add(detalle);
    }

    /**
     * Recalcula los importes a partir de los renglones.
     *
     * <p>{@code subtotal} es la suma bruta (cantidad × precio), {@code descuento} la suma de
     * los descuentos de cada renglón más el descuento global, y {@code total} la diferencia.
     */
    public void calcularTotales(BigDecimal descuentoGlobal) {
        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descuentos = descuentoGlobal == null ? BigDecimal.ZERO : descuentoGlobal;
        for (CotizacionDetalle d : detalles) {
            d.calcularSubtotal();
            bruto = bruto.add(d.importeBruto());
            descuentos = descuentos.add(d.getDescuento());
        }
        subtotal = bruto.setScale(2, RoundingMode.HALF_UP);
        descuento = descuentos.setScale(2, RoundingMode.HALF_UP);
        total = subtotal.subtract(descuento);
    }

    /** Descuento aplicado sobre el total, aparte de los descuentos de cada renglón. */
    public BigDecimal descuentoGlobal() {
        BigDecimal porRenglon = detalles.stream()
                .map(CotizacionDetalle::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return descuento.subtract(porRenglon);
    }

    /** Último día en que la cotización es válida. */
    public LocalDate fechaVencimiento() {
        return fecha.plusDays(validezDias);
    }

    /** Si corre su plazo de validez a la fecha indicada. */
    public boolean estaVigente(LocalDate hoy) {
        return EstadoCotizacion.VIGENTES.contains(estado) && !hoy.isAfter(fechaVencimiento());
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
