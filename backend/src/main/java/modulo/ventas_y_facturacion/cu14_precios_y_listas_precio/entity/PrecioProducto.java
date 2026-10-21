package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "precios_producto", uniqueConstraints = {
        @UniqueConstraint(name = "uq_precios_lista_producto", columnNames = {"lista_precio_id", "producto_id"})
})
public class PrecioProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecio listaPrecio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public PrecioProducto() {}

    public PrecioProducto(Integer id, ListaPrecio listaPrecio, Producto producto, BigDecimal precio, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.listaPrecio = listaPrecio;
        this.producto = producto;
        this.precio = precio;
        this.fechaActualizacion = fechaActualizacion != null ? fechaActualizacion : LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public ListaPrecio getListaPrecio() { return listaPrecio; }
    public void setListaPrecio(ListaPrecio listaPrecio) { this.listaPrecio = listaPrecio; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
