package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import org.springframework.stereotype.Component;

import java.util.List;

import static modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.Comunes.*;

/** CU41 - Herramientas de inventario (4). Leen inventario_sucursal, productos y kardex_movimientos. */
@Component
public class InventarioHerramientas implements ProveedorHerramientas {

    private static final String FROM_STOCK = " FROM inventario_sucursal i"
            + " JOIN productos p ON p.id = i.producto_id AND p.activo = TRUE"
            + " LEFT JOIN categorias c ON c.id = p.categoria_id"
            + " JOIN sucursal s ON s.id = i.sucursal_id AND s.activo = TRUE";

    @Override
    public List<Herramienta> herramientas() {
        return List.of(stockActual(), rotacion(), candidatosALiquidar(), movimientosDeInventario());
    }

    Herramienta stockActual() {
        return Herramienta.de("stock_actual",
                        "Stock disponible HOY y su valor a costo, por producto, categoria o sucursal. Con solo_criticos=true"
                                + " devuelve los productos sin stock o por debajo de su minimo. No hay historial de stock por"
                                + " fecha: solo el stock actual.")
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "producto", "categoria", "sucursal"))
                .parametro("solo_criticos", Parametro.logico("Solo productos sin stock o bajo el minimo."))
                .parametro("sucursal", Parametro.SUCURSAL).parametro("categoria", Parametro.CATEGORIA)
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> "producto".equals(a.texto("agrupar", "producto"))
                        ? Visual.tabla("Stock actual")
                        : Visual.barras("Valor del inventario (Bs)", "grupo", "valor_costo_bs"))
                .ejecutar((a, c) -> {
                    String agrupar = a.texto("agrupar", "producto");
                    Consulta q = new Consulta();
                    if ("producto".equals(agrupar)) {
                        q.sql("SELECT p.nombre AS producto, COALESCE(c.nombre, 'Sin categoria') AS categoria, s.nombre AS sucursal,"
                                + " i.stock_disponible AS stock, i.stock_minimo AS minimo,"
                                + " ROUND(i.stock_disponible * p.costo_compra, 2) AS valor_costo_bs" + FROM_STOCK + " WHERE 1 = 1");
                    } else {
                        String g = "categoria".equals(agrupar) ? "COALESCE(c.nombre, 'Sin categoria')" : "s.nombre";
                        q.sql("SELECT " + g + " AS grupo, ROUND(SUM(i.stock_disponible), 2) AS stock,"
                                + " ROUND(SUM(GREATEST(i.stock_disponible, 0) * p.costo_compra), 2) AS valor_costo_bs,"
                                + " COUNT(*) FILTER (WHERE i.stock_disponible <= 0) AS sin_stock,"
                                + " COUNT(*) FILTER (WHERE i.stock_disponible > 0 AND i.stock_disponible <= i.stock_minimo) AS bajo_minimo"
                                + FROM_STOCK + " WHERE 1 = 1");
                    }
                    if (a.logico("solo_criticos", false)) q.sql(" AND i.stock_disponible <= i.stock_minimo");
                    q.filtroContiene("s.nombre", a.texto("sucursal"));
                    q.filtroContiene("c.nombre", a.texto("categoria"));
                    q.sql("producto".equals(agrupar)
                            ? " ORDER BY (i.stock_disponible - i.stock_minimo), p.nombre"
                            : " GROUP BY 1 ORDER BY valor_costo_bs DESC");
                    q.sql(" LIMIT " + q.entero(a.entero("limite", 50)));
                    Consulta t = new Consulta();
                    t.sql("SELECT ROUND(SUM(GREATEST(i.stock_disponible, 0) * p.costo_compra), 2) AS valor_total_inventario_bs,"
                            + " COUNT(*) FILTER (WHERE i.stock_disponible <= 0) AS filas_sin_stock" + FROM_STOCK + " WHERE 1 = 1");
                    t.filtroContiene("s.nombre", a.texto("sucursal"));
                    t.filtroContiene("c.nombre", a.texto("categoria"));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(c.unaFila(t))
                            .conResumen(java.util.Map.of("fecha_stock", c.hoy().toString()));
                });
    }

    Herramienta rotacion() {
        return Herramienta.de("rotacion_y_dias_inventario",
                        "Rotacion del inventario (costo de lo vendido en el periodo / valor del inventario actual) y dias de"
                                + " inventario (cuantos dias alcanza el stock al ritmo de venta), por categoria o producto.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "categoria", "producto"))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.barras("Dias de inventario", "grupo", "dias_inventario"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(p.desde(), p.hasta()) + 1;
                    boolean porProducto = "producto".equals(a.texto("agrupar"));
                    String g = porProducto ? "p.nombre" : "COALESCE(c.nombre, 'Sin categoria')";
                    Consulta q = new Consulta();
                    q.sql("WITH vend AS (SELECT d.producto_id, SUM(d.cantidad * p.costo_compra) AS costo_vendido"
                            + " FROM ventas v JOIN venta_detalles d ON d.venta_id = v.id JOIN productos p ON p.id = d.producto_id"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + q.fecha(p.desde())
                            + " AND v.fecha < " + q.fecha(p.hasta()) + " + 1 GROUP BY 1),"
                            + " inv AS (SELECT producto_id, SUM(GREATEST(stock_disponible, 0)) AS stock FROM inventario_sucursal GROUP BY 1)"
                            + " SELECT " + g + " AS grupo,"
                            + " ROUND(SUM(COALESCE(inv.stock, 0) * p.costo_compra), 2) AS inventario_bs,"
                            + " ROUND(SUM(COALESCE(vend.costo_vendido, 0)), 2) AS costo_vendido_bs,"
                            + " ROUND(SUM(COALESCE(vend.costo_vendido, 0)) / NULLIF(SUM(COALESCE(inv.stock, 0) * p.costo_compra), 0), 2) AS rotacion,"
                            + " ROUND(SUM(COALESCE(inv.stock, 0) * p.costo_compra) / NULLIF(SUM(COALESCE(vend.costo_vendido, 0)) / "
                            + q.entero((int) dias) + ", 0), 0) AS dias_inventario"
                            + " FROM productos p LEFT JOIN categorias c ON c.id = p.categoria_id"
                            + " LEFT JOIN vend ON vend.producto_id = p.id LEFT JOIN inv ON inv.producto_id = p.id"
                            + " WHERE p.activo = TRUE GROUP BY 1 ORDER BY dias_inventario DESC NULLS FIRST LIMIT "
                            + q.entero(a.entero("limite", 30)));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa())
                            .conNota("dias_inventario nulo = no se vendio nada en el periodo (stock inmovilizado).");
                });
    }

    Herramienta candidatosALiquidar() {
        return Herramienta.de("candidatos_a_liquidar",
                        "Productos con mucho stock para lo poco que venden (regla: mas de 'meses_cobertura' meses de stock"
                                + " al ritmo de los ultimos 90 dias, o sin ventas en 90 dias con stock). Son reglas, no ML.")
                .parametro("meses_cobertura", Parametro.entero("Meses de cobertura a partir de los cuales liquidar (por defecto 6).", 1, 36))
                .parametro("sucursal", Parametro.SUCURSAL).parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Candidatos a liquidar"))
                .ejecutar((a, c) -> {
                    Consulta q = new Consulta();
                    q.sql("WITH vend AS (SELECT v.sucursal_id, d.producto_id, SUM(d.cantidad) AS unidades_90d"
                            + " FROM ventas v JOIN venta_detalles d ON d.venta_id = v.id"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + q.fecha(c.hoy().minusDays(90))
                            + " GROUP BY 1, 2)"
                            + " SELECT p.nombre AS producto, s.nombre AS sucursal, i.stock_disponible AS stock,"
                            + " COALESCE(vend.unidades_90d, 0) AS unidades_90d,"
                            + " ROUND(i.stock_disponible / NULLIF(COALESCE(vend.unidades_90d, 0) / 3.0, 0), 1) AS meses_cobertura,"
                            + " ROUND(i.stock_disponible * p.costo_compra, 2) AS capital_inmovilizado_bs"
                            + FROM_STOCK
                            + " LEFT JOIN vend ON vend.producto_id = i.producto_id AND vend.sucursal_id = i.sucursal_id"
                            + " WHERE i.stock_disponible > 0 AND (COALESCE(vend.unidades_90d, 0) = 0"
                            + " OR i.stock_disponible / (vend.unidades_90d / 3.0) > " + q.entero(a.entero("meses_cobertura", 6)) + ")");
                    q.filtroContiene("s.nombre", a.texto("sucursal"));
                    q.sql(" ORDER BY capital_inmovilizado_bs DESC LIMIT " + q.entero(a.entero("limite", 20)));
                    return ResultadoHerramienta.de(c.consultar(q))
                            .conNota("meses_cobertura nulo = sin ventas en 90 dias.");
                });
    }

    Herramienta movimientosDeInventario() {
        return Herramienta.de("movimientos_de_inventario",
                        "Entradas y salidas de inventario registradas en el kardex (compras, ajustes, transferencias, etc.)"
                                + " por tipo de movimiento o por mes, en unidades y valor.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "tipo", "mes", "producto"))
                .parametro("producto", Parametro.PRODUCTO).parametro("limite", Parametro.LIMITE)
                .visual(a -> "mes".equals(a.texto("agrupar"))
                        ? Visual.linea("Movimientos de inventario", "grupo", "unidades")
                        : Visual.barras("Movimientos de inventario", "grupo", "unidades"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String g = switch (a.texto("agrupar", "tipo")) {
                        case "mes" -> "to_char(k.fecha_movimiento, 'YYYY-MM')";
                        case "producto" -> "p.nombre";
                        default -> "UPPER(k.tipo_movimiento)";
                    };
                    Consulta q = new Consulta();
                    q.sql("SELECT " + g + " AS grupo, COUNT(*) AS movimientos, ROUND(SUM(k.cantidad), 2) AS unidades,"
                            + " ROUND(SUM(k.cantidad * k.costo_unitario), 2) AS valor_bs"
                            + " FROM kardex_movimientos k LEFT JOIN productos p ON p.id = k.producto_id"
                            + " WHERE k.fecha_movimiento >= " + q.fecha(p.desde()) + " AND k.fecha_movimiento < " + q.fecha(p.hasta()) + " + 1");
                    q.filtroContiene("p.nombre", a.texto("producto"));
                    q.sql(" GROUP BY 1 ORDER BY 1 LIMIT " + q.entero(a.entero("limite", 50)));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa())
                            .conNota("Solo incluye movimientos registrados en el kardex.");
                });
    }
}
