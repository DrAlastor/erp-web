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

@Entity
@Table(name = "devoluciones_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class DevolucionProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "compra_id", nullable = false)
    private Integer compraId;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "numero_devolucion", nullable = false, length = 50)
    private String numeroDevolucion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DevolucionDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(DevolucionDetalle detalle) {
        detalle.setDevolucion(this);
        detalles.add(detalle);
    }
}
