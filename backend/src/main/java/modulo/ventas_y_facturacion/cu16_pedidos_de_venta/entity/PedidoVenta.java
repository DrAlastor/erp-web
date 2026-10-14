package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de un pedido de venta (tabla {@code pedidos}).
 *
 * <p>Cliente, sucursal y vendedor se guardan como identificadores simples: sus entidades
 * viven en otros modulos y este CU solo necesita sus nombres para mostrarlos, que se leen
 * con consultas propias del repositorio de consultas.
 *
 * <p>El nombre de entidad es explicito porque el nombre JPA debe ser unico en todo el
 * proyecto.
 */
@Entity(name = "PedidoVenta")
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class PedidoVenta {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADO = "APROBADO";
    public static final String ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 50)
    private String numeroPedido;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PedidoVentaDetalle> detalles = new ArrayList<>();

    /** Reemplaza las lineas del pedido; las anteriores se borran por {@code orphanRemoval}. */
    public void reemplazarDetalles(List<PedidoVentaDetalle> nuevos) {
        detalles.clear();
        for (PedidoVentaDetalle detalle : nuevos) {
            detalle.setPedido(this);
            detalles.add(detalle);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) estado = PENDIENTE;
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
