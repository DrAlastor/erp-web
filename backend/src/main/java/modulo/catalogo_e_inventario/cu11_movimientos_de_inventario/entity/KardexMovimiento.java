package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento inmutable del Kardex valorado mapeado a la tabla kardex_movimientos de ERP_Asus.
 */
@Entity
@Table(name = "kardex_movimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Almacen almacen;

    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "saldo_cantidad", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoCantidad;

    @Column(name = "saldo_valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoValorado;

    @Column(name = "documento_referencia", length = 50)
    private String referenciaDoc;

    @Column(length = 255)
    private String motivo;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id", nullable = false)
    @Builder.Default
    private Integer usuarioId = 1;

    @Column(name = "unidad_medida_id", nullable = false)
    @Builder.Default
    private Integer unidadMedidaId = 1;

    @Transient
    public String getCreadoPor() {
        return usuarioId != null ? "Usuario #" + usuarioId : "sistema";
    }

    @PrePersist
    public void prePersist() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (usuarioId == null) {
            usuarioId = 1;
        }
        if (unidadMedidaId == null) {
            unidadMedidaId = 1;
        }
    }
}
