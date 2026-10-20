package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity;

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

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.ventas_y_facturacion.compartido.entity.Pedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nota de despacho de un pedido de venta, mapeada a la tabla NOTAS_DESPACHO de ERP_Asus.
 *
 * <p>Un pedido puede tener varias notas: cada una despacha una parte de lo pendiente.
 */
@Entity
@Table(name = "notas_despacho")
@Getter
@Setter
@NoArgsConstructor
public class NotaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Almacen sucursal;

    @Column(name = "despachado_por", nullable = false)
    private Long despachadoPor;

    @Column(name = "numero_despacho", nullable = false, unique = true, length = 50)
    private String numeroDespacho;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 100)
    private String conductor;

    @Column(length = 20)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDespacho estado = EstadoDespacho.PREPARACION;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @OneToMany(mappedBy = "despacho", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DespachoDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(DespachoDetalle detalle) {
        detalle.setDespacho(this);
        detalles.add(detalle);
    }

    /** Agrega una línea a las observaciones sin perder lo que ya había. */
    public void anotar(String linea) {
        observaciones = observaciones == null || observaciones.isBlank()
                ? linea
                : observaciones + "\n" + linea;
    }

    @PrePersist
    protected void alCrear() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
