package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Renglón de la cotización, mapeado a la tabla COTIZACION_DETALLES de ERP_Asus. */
@Entity
@Table(name = "cotizacion_detalles")
@Getter
@Setter
@NoArgsConstructor
public class CotizacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Cantidad × precio, antes del descuento del renglón. */
    public BigDecimal importeBruto() {
        return cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularSubtotal() {
        subtotal = importeBruto().subtract(descuento);
        return subtotal;
    }
}
