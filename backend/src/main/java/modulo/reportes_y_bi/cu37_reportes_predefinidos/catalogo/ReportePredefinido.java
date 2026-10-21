package modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo;

import java.util.List;
import java.util.Set;

import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna.cantidad;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna.conteo;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna.fecha;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna.moneda;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna.texto;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte.CATEGORIA;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte.DESDE;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte.HASTA;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte.LIMITE;
import static modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte.SUCURSAL;

/**
 * Los reportes predefinidos del CU37. Cada uno declara sus filtros y sus columnas; la
 * consulta que lo llena vive en {@code ReportesRepositorio}.
 */
public enum ReportePredefinido {

    VENTAS_POR_PERIODO(CategoriaReporte.VENTAS,
            "Ventas por período",
            "Ventas emitidas por día, con subtotal, descuento, IVA y total. No incluye las anuladas.",
            Set.of(DESDE, HASTA, SUCURSAL),
            List.of(fecha("dia", "Día"),
                    conteo("ventas", "N.º de ventas"),
                    moneda("subtotal", "Subtotal", true),
                    moneda("descuento", "Descuento", true),
                    moneda("iva", "IVA", true),
                    moneda("total", "Total", true))),

    VENTAS_POR_CAJERO(CategoriaReporte.VENTAS,
            "Ventas por cajero",
            "Cantidad de ventas, monto vendido y ticket promedio de cada cajero en el período.",
            Set.of(DESDE, HASTA, SUCURSAL),
            List.of(texto("cajero", "Cajero"),
                    conteo("ventas", "N.º de ventas"),
                    moneda("total", "Total vendido", true),
                    moneda("ticket_promedio", "Ticket promedio", false))),

    PRODUCTOS_MAS_VENDIDOS(CategoriaReporte.VENTAS,
            "Productos más vendidos",
            "Ranking de productos por unidades vendidas en el período.",
            Set.of(DESDE, HASTA, SUCURSAL, LIMITE),
            List.of(texto("codigo", "Código"),
                    texto("producto", "Producto"),
                    cantidad("cantidad", "Unidades", true),
                    moneda("importe", "Importe", true))),

    EXISTENCIAS_POR_SUCURSAL(CategoriaReporte.INVENTARIO,
            "Existencias por sucursal",
            "Stock disponible de cada producto activo frente a su stock mínimo.",
            Set.of(SUCURSAL, CATEGORIA),
            List.of(texto("sucursal", "Sucursal"),
                    texto("codigo", "Código"),
                    texto("producto", "Producto"),
                    texto("categoria", "Categoría"),
                    cantidad("stock", "Stock", true),
                    cantidad("minimo", "Mínimo", false),
                    texto("estado", "Estado"))),

    PRODUCTOS_BAJO_MINIMO(CategoriaReporte.INVENTARIO,
            "Productos bajo stock mínimo",
            "Productos cuyo stock llegó o bajó del mínimo, con las unidades que faltan para reponer.",
            Set.of(SUCURSAL),
            List.of(texto("sucursal", "Sucursal"),
                    texto("codigo", "Código"),
                    texto("producto", "Producto"),
                    cantidad("stock", "Stock", false),
                    cantidad("minimo", "Mínimo", false),
                    cantidad("faltante", "Faltante", true))),

    VALORIZACION_INVENTARIO(CategoriaReporte.INVENTARIO,
            "Valorización del inventario",
            "Valor del stock a costo de compra y a precio de venta.",
            Set.of(SUCURSAL),
            List.of(texto("sucursal", "Sucursal"),
                    texto("codigo", "Código"),
                    texto("producto", "Producto"),
                    cantidad("stock", "Stock", true),
                    moneda("costo_unitario", "Costo unitario", false),
                    moneda("valor_costo", "Valor a costo", true),
                    moneda("precio_venta", "Precio de venta", false),
                    moneda("valor_venta", "Valor a precio de venta", true)));

    private final CategoriaReporte categoria;
    private final String titulo;
    private final String descripcion;
    private final Set<FiltroReporte> filtros;
    private final List<Columna> columnas;

    ReportePredefinido(CategoriaReporte categoria, String titulo, String descripcion,
                       Set<FiltroReporte> filtros, List<Columna> columnas) {
        this.categoria = categoria;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.filtros = filtros;
        this.columnas = columnas;
    }

    public CategoriaReporte categoria() {
        return categoria;
    }

    public String titulo() {
        return titulo;
    }

    public String descripcion() {
        return descripcion;
    }

    public Set<FiltroReporte> filtros() {
        return filtros;
    }

    public List<Columna> columnas() {
        return columnas;
    }

    public boolean usaFechas() {
        return filtros.contains(DESDE);
    }
}
