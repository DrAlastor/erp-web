package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo.*;

/**
 * Catálogo predefinido de entidades del negocio que se pueden consultar con el constructor
 * QBE (HU38.1). Es la única fuente de SQL del CU38: el usuario elige entidades y campos por
 * su clave, y la tabla, los JOIN y la expresión de cada campo salen de aquí.
 *
 * <p>El código de cada entidad es el nombre de su tabla base, que es lo que se guarda en
 * {@code REPORTES_QBE.TABLA_BASE}.
 */
public final class CatalogoEntidadesQbe {

    public record Campo(String clave, String etiqueta, TipoCampo tipo, String sql) {}

    public record Entidad(String codigo, String etiqueta, String modulo, String from, List<Campo> campos) {

        public Optional<Campo> campo(String clave) {
            if (clave == null) return Optional.empty();
            String buscada = clave.trim().toLowerCase(Locale.ROOT);
            return campos.stream().filter(c -> c.clave().equals(buscada)).findFirst();
        }
    }

    private static final String VENDEDOR =
            "COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email)";

    private static final Map<String, Entidad> ENTIDADES = new LinkedHashMap<>();

    static {
        registrar(new Entidad("VENTAS", "Ventas (facturas)", "Ventas", """
                ventas v
                LEFT JOIN sucursal s ON s.id = v.sucursal_id
                LEFT JOIN clientes c ON c.id = v.cliente_id
                LEFT JOIN usuario u ON u.id = v.cajero_id
                LEFT JOIN empleados e ON e.id = u.empleado_id
                """, List.of(
                new Campo("numero_factura", "N° factura", TEXTO, "CAST(v.numero_factura AS TEXT)"),
                new Campo("fecha", "Fecha", FECHA, "v.fecha"),
                new Campo("cliente", "Cliente", TEXTO, "COALESCE(c.razon_social, v.razon_social_cliente)"),
                new Campo("nit_cliente", "NIT cliente", TEXTO, "v.nit_cliente"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("cajero", "Vendedor / cajero", TEXTO, VENDEDOR),
                new Campo("tipo_venta", "Tipo de venta", TEXTO, "v.tipo_venta"),
                new Campo("estado", "Estado", TEXTO, "v.estado"),
                new Campo("subtotal", "Subtotal", MONEDA, "v.subtotal"),
                new Campo("descuento", "Descuento", MONEDA, "v.descuento"),
                new Campo("iva", "IVA", MONEDA, "v.iva"),
                new Campo("it", "IT", MONEDA, "v.it"),
                new Campo("total", "Total", MONEDA, "v.total"))));

        registrar(new Entidad("VENTA_DETALLES", "Ventas por producto", "Ventas", """
                venta_detalles d
                JOIN ventas v ON v.id = d.venta_id
                LEFT JOIN productos p ON p.id = d.producto_id
                LEFT JOIN categorias cat ON cat.id = p.categoria_id
                LEFT JOIN sucursal s ON s.id = v.sucursal_id
                """, List.of(
                new Campo("numero_factura", "N° factura", TEXTO, "CAST(v.numero_factura AS TEXT)"),
                new Campo("fecha", "Fecha", FECHA, "v.fecha"),
                new Campo("producto", "Producto", TEXTO, "p.nombre"),
                new Campo("codigo", "Código", TEXTO, "p.codigo"),
                new Campo("categoria", "Categoría", TEXTO, "cat.nombre"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("estado_venta", "Estado de la venta", TEXTO, "v.estado"),
                new Campo("cantidad", "Cantidad", NUMERO, "d.cantidad"),
                new Campo("precio_unitario", "Precio unitario", MONEDA, "d.precio_unitario"),
                new Campo("descuento", "Descuento", MONEDA, "d.descuento"),
                new Campo("subtotal", "Subtotal", MONEDA, "d.subtotal"))));

        registrar(new Entidad("PEDIDOS", "Pedidos de venta", "Ventas", """
                pedidos pe
                LEFT JOIN clientes c ON c.id = pe.cliente_id
                LEFT JOIN sucursal s ON s.id = pe.sucursal_id
                LEFT JOIN usuario u ON u.id = pe.vendedor_id
                LEFT JOIN empleados e ON e.id = u.empleado_id
                """, List.of(
                new Campo("numero_pedido", "N° pedido", TEXTO, "pe.numero_pedido"),
                new Campo("fecha", "Fecha", FECHA, "pe.fecha"),
                new Campo("cliente", "Cliente", TEXTO, "c.razon_social"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("vendedor", "Vendedor", TEXTO, VENDEDOR),
                new Campo("estado", "Estado", TEXTO, "pe.estado"),
                new Campo("subtotal", "Subtotal", MONEDA, "pe.subtotal"),
                new Campo("descuento", "Descuento", MONEDA, "pe.descuento"),
                new Campo("total", "Total", MONEDA, "pe.total"))));

        registrar(new Entidad("CLIENTES", "Clientes", "Ventas", "clientes c", List.of(
                new Campo("razon_social", "Razón social", TEXTO, "c.razon_social"),
                new Campo("nit_ci", "NIT / CI", TEXTO, "c.nit_ci"),
                new Campo("telefono", "Teléfono", TEXTO, "c.telefono"),
                new Campo("email", "Correo", TEXTO, "c.email"),
                new Campo("ciudad", "Ciudad", TEXTO, "c.ciudad"),
                new Campo("condicion_pago", "Condición de pago", TEXTO, "c.condicion_pago"),
                new Campo("limite_credito", "Límite de crédito", MONEDA, "c.limite_credito"),
                new Campo("dias_credito", "Días de crédito", NUMERO, "c.dias_credito"),
                new Campo("activo", "Activo", BOOLEANO, "c.activo"),
                new Campo("fecha_creacion", "Fecha de registro", FECHA, "c.fecha_creacion"))));

        registrar(new Entidad("PRODUCTOS", "Artículos", "Inventarios", """
                productos p
                LEFT JOIN categorias cat ON cat.id = p.categoria_id
                LEFT JOIN unidades_medida um ON um.id = p.unidad_medida_id
                """, List.of(
                new Campo("codigo", "Código", TEXTO, "p.codigo"),
                new Campo("nombre", "Nombre", TEXTO, "p.nombre"),
                new Campo("categoria", "Categoría", TEXTO, "cat.nombre"),
                new Campo("unidad", "Unidad de medida", TEXTO, "um.nombre"),
                new Campo("precio_venta", "Precio de venta", MONEDA, "p.precio_venta"),
                new Campo("costo_compra", "Costo de compra", MONEDA, "p.costo_compra"),
                new Campo("stock_minimo", "Stock mínimo", NUMERO, "p.stock_minimo"),
                new Campo("stock_maximo", "Stock máximo", NUMERO, "p.stock_maximo"),
                new Campo("activo", "Activo", BOOLEANO, "p.activo"),
                new Campo("fecha_creacion", "Fecha de registro", FECHA, "p.fecha_creacion"))));

        registrar(new Entidad("INVENTARIO_SUCURSAL", "Inventario por sucursal", "Inventarios", """
                inventario_sucursal i
                JOIN productos p ON p.id = i.producto_id
                LEFT JOIN categorias cat ON cat.id = p.categoria_id
                LEFT JOIN sucursal s ON s.id = i.sucursal_id
                """, List.of(
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("producto", "Producto", TEXTO, "p.nombre"),
                new Campo("codigo", "Código", TEXTO, "p.codigo"),
                new Campo("categoria", "Categoría", TEXTO, "cat.nombre"),
                new Campo("stock_disponible", "Stock disponible", NUMERO, "i.stock_disponible"),
                new Campo("stock_minimo", "Stock mínimo", NUMERO, "i.stock_minimo"),
                new Campo("valor_costo", "Valor a costo", MONEDA, "i.stock_disponible * p.costo_compra"),
                new Campo("ultima_actualizacion", "Última actualización", FECHA, "i.ultima_actualizacion"))));

        registrar(new Entidad("KARDEX_MOVIMIENTOS", "Kardex de movimientos", "Inventarios", """
                kardex_movimientos k
                LEFT JOIN productos p ON p.id = k.producto_id
                LEFT JOIN sucursal s ON s.id = k.sucursal_id
                """, List.of(
                new Campo("fecha", "Fecha", FECHA, "k.fecha_movimiento"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("producto", "Producto", TEXTO, "p.nombre"),
                new Campo("tipo_movimiento", "Tipo de movimiento", TEXTO, "k.tipo_movimiento"),
                new Campo("motivo", "Motivo", TEXTO, "k.motivo"),
                new Campo("documento", "Documento", TEXTO, "k.documento_referencia"),
                new Campo("cantidad", "Cantidad", NUMERO, "k.cantidad"),
                new Campo("costo_unitario", "Costo unitario", MONEDA, "k.costo_unitario"),
                new Campo("saldo_cantidad", "Saldo (cantidad)", NUMERO, "k.saldo_cantidad"),
                new Campo("saldo_valor", "Saldo (valor)", MONEDA, "k.saldo_valor"))));

        registrar(new Entidad("COMPRAS", "Compras", "Compras", """
                compras co
                LEFT JOIN proveedores pr ON pr.id = co.proveedor_id
                LEFT JOIN sucursal s ON s.id = co.sucursal_id
                """, List.of(
                new Campo("numero_factura", "N° factura", TEXTO, "co.numero_factura"),
                new Campo("fecha", "Fecha", FECHA, "co.fecha"),
                new Campo("proveedor", "Proveedor", TEXTO, "pr.razon_social"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("tipo_pago", "Tipo de pago", TEXTO, "co.tipo_pago"),
                new Campo("estado", "Estado", TEXTO, "co.estado"),
                new Campo("subtotal", "Subtotal", MONEDA, "co.subtotal"),
                new Campo("iva", "IVA", MONEDA, "co.iva"),
                new Campo("total", "Total", MONEDA, "co.total"))));

        registrar(new Entidad("PROVEEDORES", "Proveedores", "Compras", "proveedores pr", List.of(
                new Campo("razon_social", "Razón social", TEXTO, "pr.razon_social"),
                new Campo("nit_ci", "NIT / CI", TEXTO, "pr.nit_ci"),
                new Campo("contacto", "Contacto", TEXTO, "pr.contacto"),
                new Campo("telefono", "Teléfono", TEXTO, "pr.telefono"),
                new Campo("email", "Correo", TEXTO, "pr.email"),
                new Campo("ciudad", "Ciudad", TEXTO, "pr.ciudad"),
                new Campo("condicion_pago", "Condición de pago", TEXTO, "pr.condicion_pago"),
                new Campo("dias_credito", "Días de crédito", NUMERO, "pr.dias_credito"),
                new Campo("activo", "Activo", BOOLEANO, "pr.activo"))));

        registrar(new Entidad("CUENTAS_POR_COBRAR", "Cuentas por cobrar", "Finanzas", """
                cuentas_por_cobrar x
                LEFT JOIN clientes c ON c.id = x.cliente_id
                LEFT JOIN sucursal s ON s.id = x.sucursal_id
                """, List.of(
                new Campo("cliente", "Cliente", TEXTO, "c.razon_social"),
                new Campo("sucursal", "Sucursal", TEXTO, "s.nombre"),
                new Campo("numero_cuota", "N° cuota", NUMERO, "x.numero_cuota"),
                new Campo("monto_total", "Monto total", MONEDA, "x.monto_total"),
                new Campo("monto_cobrado", "Monto cobrado", MONEDA, "x.monto_cobrado"),
                new Campo("saldo", "Saldo", MONEDA, "x.saldo"),
                new Campo("fecha_vencimiento", "Vencimiento", FECHA, "x.fecha_vencimiento"),
                new Campo("estado", "Estado", TEXTO, "x.estado"))));

        registrar(new Entidad("CUENTAS_POR_PAGAR", "Cuentas por pagar", "Finanzas", """
                cuentas_por_pagar x
                LEFT JOIN proveedores pr ON pr.id = x.proveedor_id
                """, List.of(
                new Campo("proveedor", "Proveedor", TEXTO, "pr.razon_social"),
                new Campo("numero_cuota", "N° cuota", NUMERO, "x.numero_cuota"),
                new Campo("monto_total", "Monto total", MONEDA, "x.monto_total"),
                new Campo("monto_pagado", "Monto pagado", MONEDA, "x.monto_pagado"),
                new Campo("saldo", "Saldo", MONEDA, "x.saldo"),
                new Campo("fecha_vencimiento", "Vencimiento", FECHA, "x.fecha_vencimiento"),
                new Campo("estado", "Estado", TEXTO, "x.estado"))));

        registrar(new Entidad("ASIENTO_DETALLES", "Libro diario (asientos)", "Contabilidad", """
                asiento_detalles ad
                JOIN asientos_contables a ON a.id = ad.asiento_id
                LEFT JOIN cuentas_contables cc ON cc.id = ad.cuenta_contable_id
                """, List.of(
                new Campo("numero_asiento", "N° asiento", TEXTO, "a.numero_asiento"),
                new Campo("fecha", "Fecha", FECHA, "a.fecha"),
                new Campo("tipo_asiento", "Tipo de asiento", TEXTO, "a.tipo_asiento"),
                new Campo("glosa", "Glosa", TEXTO, "a.glosa"),
                new Campo("cuenta_codigo", "Código de cuenta", TEXTO, "cc.codigo"),
                new Campo("cuenta", "Cuenta", TEXTO, "cc.nombre"),
                new Campo("debe", "Debe", MONEDA, "ad.debe"),
                new Campo("haber", "Haber", MONEDA, "ad.haber"),
                new Campo("estado", "Estado", TEXTO, "a.estado"))));
    }

    private CatalogoEntidadesQbe() {
    }

    public static List<Entidad> todas() {
        return List.copyOf(ENTIDADES.values());
    }

    public static Optional<Entidad> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(ENTIDADES.get(codigo.trim().toUpperCase(Locale.ROOT)));
    }

    private static void registrar(Entidad entidad) {
        ENTIDADES.put(entidad.codigo(), entidad);
    }
}
