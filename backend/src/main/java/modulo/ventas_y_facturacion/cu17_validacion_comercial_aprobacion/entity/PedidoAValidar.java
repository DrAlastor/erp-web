package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
 * Pedido de venta mapeado a la tabla {@code pedidos} de ERP_Asus.
 *
 * <p>Es el modelo mínimo que necesita la validación comercial del CU17. El CU16 (gestionar
 * pedidos) es el dueño de esta entidad y puede ampliarla; las referencias a cliente,
 * vendedor y sucursal se guardan como identificadores para no acoplar este paquete a las
 * entidades de otros casos de uso.
 */
@Entity(name = "PedidoAValidar")
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class PedidoAValidar {

    /** Pedido registrado que espera la decisión del validador comercial. */
    public static final String PENDIENTE = "PENDIENTE";
    /** Pedido aprobado: reserva existencias hasta que se despache. */
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "vendedor_id", nullable = false)
    private Integer vendedorId;

    @Column(name = "sucursal_id", nullable = false)
    private Integer sucursalId;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 50)
    private String numeroPedido;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String estado = PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    private List<PedidoAValidarDetalle> detalles = new ArrayList<>();

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
