package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encabezado de la venta y su factura, mapeado a la tabla ventas de ERP_Asus.
 *
 * <p>Vive en {@code compartido} porque la registra la HU-19 y la lee la HU-20 para
 * emitir notas de crédito sobre ella.
 */
@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    public static final String ESTADO_EMITIDA = "EMITIDA";
    public static final String ESTADO_ANULADA = "ANULADA";
    public static final String TIPO_CONTADO = "CONTADO";
    public static final String TIPO_CREDITO = "CREDITO";
    /** Comprobante fiscal con datos del SIN (autorización, código de control) e impuestos. */
    public static final String COMPROBANTE_FACTURA = "FACTURA";
    /** Comprobante no fiscal: sin datos del SIN ni débito fiscal, con numeración propia. */
    public static final String COMPROBANTE_NOTA_VENTA = "NOTA_VENTA";
    /** Valor de numero_autorizacion (NOT NULL) para las notas de venta, que no tienen autorización. */
    public static final String AUTORIZACION_NOTA_VENTA = "NOTA DE VENTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Almacen sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "cajero_id", nullable = false)
    private Long cajeroId;

    @Column(name = "turno_caja_id")
    private Long turnoCajaId;

    @Column(name = "secuencia_factura_id")
    private Long secuenciaFacturaId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "numero_factura", nullable = false)
    private Integer numeroFactura;

    @Column(name = "numero_autorizacion", nullable = false, length = 60)
    private String numeroAutorizacion;

    @Column(name = "codigo_control", length = 50)
    private String codigoControl;

    @Column(name = "nit_cliente", nullable = false, length = 30)
    private String nitCliente;

    @Column(name = "razon_social_cliente", nullable = false, length = 150)
    private String razonSocialCliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(name = "it", nullable = false, precision = 12, scale = 2)
    private BigDecimal it;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "tipo_venta", nullable = false, length = 30)
    private String tipoVenta;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Columnas agregadas por la CU19 (ver InicializadorVentas): tipo de comprobante y
    // registro de la anulación.
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    @Builder.Default
    private String tipoComprobante = COMPROBANTE_FACTURA;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "anulado_por")
    private Long anuladoPor;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<VentaDetalle> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<PagoVenta> pagos = new ArrayList<>();

    public boolean esFactura() {
        return !COMPROBANTE_NOTA_VENTA.equals(tipoComprobante);
    }

    /** "Factura N° 1003" o "Nota de venta N° 12", para glosas y mensajes. */
    public String descripcionComprobante() {
        return (esFactura() ? "Factura" : "Nota de venta") + " N° " + numeroFactura;
    }

    public void agregarDetalle(VentaDetalle detalle) {
        detalle.setVenta(this);
        detalles.add(detalle);
    }

    public void agregarPago(PagoVenta pago) {
        pago.setVenta(this);
        pagos.add(pago);
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (fecha == null) fecha = fechaCreacion;
    }
}
