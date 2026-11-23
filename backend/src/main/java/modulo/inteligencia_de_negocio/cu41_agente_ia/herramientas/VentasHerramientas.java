package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.Comunes.*;

/** CU41 - Herramientas de ventas (8). Leen ventas, venta_detalles, productos, categorias y notas_credito. */
@Component
public class VentasHerramientas implements ProveedorHerramientas {

    private static final String METRICAS = "ROUND(SUM(" + IMPORTE + "), 2) AS ventas_bs,"
            + " ROUND(SUM(d.cantidad), 2) AS unidades, COUNT(DISTINCT v.id) AS tickets";

    @Override
    public List<Herramienta> herramientas() {
        return List.of(ventasPorPeriodo(), compararPeriodos(), topProductos(), ticketPromedio(), margenPor(),
                descuentosOtorgados(), descomponerVariacion(), tasaDevolucion());
    }

    Herramienta ventasPorPeriodo() {
        return Herramienta.de("ventas_por_periodo",
                        "Ventas netas (Bs, despues de descuentos), unidades y cantidad de ventas (tickets) de un periodo,"
                                + " en total o agrupadas por tiempo, sucursal, categoria, producto o tipo de venta."
                                + " Es la herramienta general para 'cuanto vendi'. No da margen ni ganancia"
                                + " (para eso margen_por o resultado_del_periodo).")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", AGRUPAR).parametro("sucursal", Parametro.SUCURSAL)
                .parametro("categoria", Parametro.CATEGORIA).parametro("producto", Parametro.PRODUCTO)
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> visualAgrupado(a.texto("agrupar", "total"), "Ventas (Bs)", "ventas_bs"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String agrupar = a.texto("agrupar", "total");
                    Consulta q = new Consulta();
                    q.sql("SELECT " + Consulta.expresion(AGRUPACIONES, agrupar) + " AS grupo, " + METRICAS + FROM_VENTAS);
                    whereVentas(q, p, a);
                    q.sql(" GROUP BY 1" + orden(agrupar, "ventas_bs") + " LIMIT " + q.entero(a.entero("limite", 100)));
                    List<Map<String, Object>> filas = c.consultar(q);
                    return ResultadoHerramienta.de(filas).conResumen(p.comoMapa()).conResumen(totales(filas))
                            .conNota("ventas_bs ya descuenta los descuentos; tickets = cantidad de ventas.");
                });
    }

    Herramienta compararPeriodos() {
        return Herramienta.de("comparar_periodos",
                        "Compara dos periodos A y B: ventas netas, unidades, tickets, ticket promedio, margen estimado,"
                                + " devoluciones y variacion %. Usala para 'como me fue este mes frente al anterior' y como"
                                + " primer paso de las preguntas de 'por que'. Para saber QUE explico la diferencia usa"
                                + " descomponer_variacion.")
                .parametro("desde_a", Parametro.fecha("Inicio del periodo A (base).")).parametro("hasta_a", Parametro.fecha("Fin del periodo A."))
                .parametro("desde_b", Parametro.fecha("Inicio del periodo B (el que se compara).")).parametro("hasta_b", Parametro.fecha("Fin del periodo B."))
                .parametro("sucursal", Parametro.SUCURSAL).parametro("categoria", Parametro.CATEGORIA)
                .requeridos("desde_a", "hasta_a", "desde_b", "hasta_b")
                .visual(a -> Visual.barras("Periodo A vs. B", "periodo", "ventas_bs"))
                .ejecutar((a, c) -> {
                    Periodo pa = periodo(a, c, "desde_a", "hasta_a");
                    Periodo pb = periodo(a, c, "desde_b", "hasta_b");
                    Map<String, Object> fa = indicadores(pa, a, c, "A");
                    Map<String, Object> fb = indicadores(pb, a, c, "B");
                    Map<String, Object> variacion = new LinkedHashMap<>();
                    for (String k : List.of("ventas_bs", "unidades", "tickets", "ticket_promedio", "margen_bs", "devoluciones_bs")) {
                        variacion.put("variacion_pct_" + k, variacionPct(fa.get(k), fb.get(k)));
                    }
                    return ResultadoHerramienta.de(List.of(fa, fb)).conResumen(variacion)
                            .conNota("variacion_pct = (B - A) / A * 100. El margen usa el costo de compra actual de cada producto.");
                });
    }

    private Map<String, Object> indicadores(Periodo p, Argumentos a, ContextoHerramienta c, String etiqueta) {
        Consulta q = new Consulta();
        q.sql("SELECT " + METRICAS + ", ROUND(SUM(" + IMPORTE + " - " + COSTO + "), 2) AS margen_bs" + FROM_VENTAS);
        whereVentas(q, p, a);
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("periodo", etiqueta + ": " + p.desde() + " a " + p.hasta());
        f.putAll(c.unaFila(q));
        BigDecimal ventas = num(f.get("ventas_bs"));
        long tickets = f.get("tickets") instanceof Number n ? n.longValue() : 0;
        f.put("ticket_promedio", tickets == 0 ? null : ventas.divide(BigDecimal.valueOf(tickets), 2, RoundingMode.HALF_UP));
        Consulta d = new Consulta();
        d.sql("SELECT COALESCE(SUM(n.total), 0) AS devoluciones_bs FROM notas_credito n LEFT JOIN sucursal s ON s.id = n.sucursal_id"
                + " WHERE UPPER(n.estado) <> 'ANULADA' AND n.fecha BETWEEN " + d.fecha(p.desde()) + " AND " + d.fecha(p.hasta()));
        d.filtroContiene("s.nombre", a.texto("sucursal"));
        f.put("devoluciones_bs", c.unaFila(d).get("devoluciones_bs"));
        return f;
    }

    Herramienta topProductos() {
        return Herramienta.de("top_productos",
                        "Ranking de productos que MAS o MENOS venden en un periodo, ordenado por ventas (Bs), unidades o"
                                + " margen estimado. Para productos sin ventas o con mucho stock usa candidatos_a_liquidar.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("orden", Parametro.opciones("Metrica del ranking.", "ventas", "unidades", "margen"))
                .parametro("direccion", Parametro.opciones("mas = los que mas venden; menos = los que menos.", "mas", "menos"))
                .parametro("sucursal", Parametro.SUCURSAL).parametro("categoria", Parametro.CATEGORIA)
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.barras("Productos por " + a.texto("orden", "ventas"), "producto", columnaOrden(a.texto("orden", "ventas"))))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String col = columnaOrden(a.texto("orden", "ventas"));
                    String dir = "menos".equals(a.texto("direccion")) ? "ASC" : "DESC";
                    Consulta q = new Consulta();
                    q.sql("SELECT COALESCE(p.nombre, 'Producto ' || d.producto_id) AS producto,"
                            + " COALESCE(c.nombre, 'Sin categoria') AS categoria, " + METRICAS + ","
                            + " ROUND(SUM(" + IMPORTE + " - " + COSTO + "), 2) AS margen_bs" + FROM_VENTAS);
                    whereVentas(q, p, a);
                    q.sql(" GROUP BY 1, 2 ORDER BY " + col + " " + dir + " LIMIT " + q.entero(a.entero("limite", 10)));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa())
                            .conNota("Solo aparecen productos con al menos una venta en el periodo.");
                });
    }

    private static String columnaOrden(String orden) {
        return switch (orden) {
            case "unidades" -> "unidades";
            case "margen" -> "margen_bs";
            default -> "ventas_bs";
        };
    }

    Herramienta ticketPromedio() {
        return Herramienta.de("ticket_promedio",
                        "Ticket promedio (ventas netas / cantidad de ventas) de un periodo, en total o por mes, sucursal o"
                                + " tipo de venta.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "total", "mes", "sucursal", "tipo_venta"))
                .parametro("sucursal", Parametro.SUCURSAL)
                .visual(a -> visualAgrupado(a.texto("agrupar", "total"), "Ticket promedio (Bs)", "ticket_promedio"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String agrupar = a.texto("agrupar", "total");
                    Consulta q = new Consulta();
                    q.sql("SELECT " + Consulta.expresion(AGRUPACIONES, agrupar) + " AS grupo, " + METRICAS
                            + ", ROUND(SUM(" + IMPORTE + ") / NULLIF(COUNT(DISTINCT v.id), 0), 2) AS ticket_promedio" + FROM_VENTAS);
                    whereVentas(q, p, a);
                    q.sql(" GROUP BY 1" + orden(agrupar, "ticket_promedio"));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa());
                });
    }

    Herramienta margenPor() {
        return Herramienta.de("margen_por",
                        "Margen bruto estimado (ventas netas - costo) y su % sobre ventas, en total o agrupado. El costo es"
                                + " el costo de compra ACTUAL del producto. Informa cuantas lineas no tienen costo cargado."
                                + " Para la ganancia del periodo con gastos e impuestos usa resultado_del_periodo.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", AGRUPAR).parametro("sucursal", Parametro.SUCURSAL)
                .parametro("categoria", Parametro.CATEGORIA).parametro("limite", Parametro.LIMITE)
                .visual(a -> visualAgrupado(a.texto("agrupar", "total"), "Margen bruto (Bs)", "margen_bs"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String agrupar = a.texto("agrupar", "total");
                    Consulta q = new Consulta();
                    q.sql("SELECT " + Consulta.expresion(AGRUPACIONES, agrupar) + " AS grupo,"
                            + " ROUND(SUM(" + IMPORTE + "), 2) AS ventas_bs, ROUND(SUM(" + COSTO + "), 2) AS costo_bs,"
                            + " ROUND(SUM(" + IMPORTE + " - " + COSTO + "), 2) AS margen_bs,"
                            + " ROUND(100 * SUM(" + IMPORTE + " - " + COSTO + ") / NULLIF(SUM(" + IMPORTE + "), 0), 1) AS margen_pct,"
                            + " COUNT(*) FILTER (WHERE COALESCE(p.costo_compra, 0) = 0) AS lineas_sin_costo" + FROM_VENTAS);
                    whereVentas(q, p, a);
                    q.sql(" GROUP BY 1" + orden(agrupar, "margen_bs") + " LIMIT " + q.entero(a.entero("limite", 100)));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa())
                            .conNota("Margen estimado con el costo de compra actual; lineas_sin_costo cuentan como costo 0.");
                });
    }

    Herramienta descuentosOtorgados() {
        return Herramienta.de("descuentos_otorgados",
                        "Descuentos dados en las ventas (Bs) y su % sobre el precio de lista, en total o por mes o sucursal.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "total", "mes", "sucursal"))
                .parametro("sucursal", Parametro.SUCURSAL)
                .visual(a -> visualAgrupado(a.texto("agrupar", "total"), "Descuentos (Bs)", "descuento_bs"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String agrupar = a.texto("agrupar", "total");
                    Consulta q = new Consulta();
                    q.sql("SELECT " + Consulta.expresion(AGRUPACIONES, agrupar) + " AS grupo,"
                            + " ROUND(SUM(v.subtotal), 2) AS precio_lista_bs, ROUND(SUM(v.descuento), 2) AS descuento_bs,"
                            + " ROUND(100 * SUM(v.descuento) / NULLIF(SUM(v.subtotal), 0), 2) AS descuento_pct,"
                            + " COUNT(*) FILTER (WHERE v.descuento > 0) AS ventas_con_descuento, COUNT(*) AS ventas"
                            + " FROM ventas v LEFT JOIN sucursal s ON s.id = v.sucursal_id"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + q.fecha(p.desde())
                            + " AND v.fecha < " + q.fecha(p.hasta()) + " + 1");
                    q.filtroContiene("s.nombre", a.texto("sucursal"));
                    q.sql(" GROUP BY 1" + orden(agrupar, "descuento_bs"));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa());
                });
    }

    Herramienta descomponerVariacion() {
        return Herramienta.de("descomponer_variacion",
                        "Cuanto aporto cada categoria, sucursal o producto a la diferencia de ventas entre el periodo A y"
                                + " el B (diferencia = B - A). Usala para responder 'por que subieron/bajaron las ventas'.")
                .parametro("desde_a", Parametro.fecha("Inicio del periodo A (base).")).parametro("hasta_a", Parametro.fecha("Fin del periodo A."))
                .parametro("desde_b", Parametro.fecha("Inicio del periodo B.")).parametro("hasta_b", Parametro.fecha("Fin del periodo B."))
                .parametro("dimension", Parametro.opciones("Por que dimension descomponer.", "categoria", "sucursal", "producto"))
                .parametro("limite", Parametro.LIMITE)
                .requeridos("desde_a", "hasta_a", "desde_b", "hasta_b", "dimension")
                .visual(a -> Visual.barras("Aporte a la variacion (Bs)", "grupo", "diferencia_bs"))
                .ejecutar((a, c) -> {
                    Periodo pa = periodo(a, c, "desde_a", "hasta_a");
                    Periodo pb = periodo(a, c, "desde_b", "hasta_b");
                    String expr = Consulta.expresion(AGRUPACIONES, a.texto("dimension"));
                    // Cada condicion de fecha se escribe dos veces (FILTER y WHERE): cada aparicion agrega
                    // sus propios valores, en el mismo orden en que quedan los "?" en el SQL.
                    Consulta final_ = new Consulta();
                    String fa = "v.fecha >= " + final_.fecha(pa.desde()) + " AND v.fecha < " + final_.fecha(pa.hasta()) + " + 1";
                    String fb = "v.fecha >= " + final_.fecha(pb.desde()) + " AND v.fecha < " + final_.fecha(pb.hasta()) + " + 1";
                    String fa2 = "v.fecha >= " + final_.fecha(pa.desde()) + " AND v.fecha < " + final_.fecha(pa.hasta()) + " + 1";
                    String fb2 = "v.fecha >= " + final_.fecha(pb.desde()) + " AND v.fecha < " + final_.fecha(pb.hasta()) + " + 1";
                    final_.sql("SELECT grupo, ventas_a_bs, ventas_b_bs, ROUND(ventas_b_bs - ventas_a_bs, 2) AS diferencia_bs FROM ("
                            + "SELECT " + expr + " AS grupo,"
                            + " ROUND(COALESCE(SUM(" + IMPORTE + ") FILTER (WHERE " + fa + "), 0), 2) AS ventas_a_bs,"
                            + " ROUND(COALESCE(SUM(" + IMPORTE + ") FILTER (WHERE " + fb + "), 0), 2) AS ventas_b_bs" + FROM_VENTAS
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND ((" + fa2 + ") OR (" + fb2 + "))"
                            + " GROUP BY 1) t ORDER BY ABS(ventas_b_bs - ventas_a_bs) DESC LIMIT " + final_.entero(a.entero("limite", 15)));
                    List<Map<String, Object>> filas = c.consultar(final_);
                    BigDecimal totalA = filas.stream().map(f -> num(f.get("ventas_a_bs"))).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalB = filas.stream().map(f -> num(f.get("ventas_b_bs"))).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("periodo_a", pa.desde() + " a " + pa.hasta());
                    r.put("periodo_b", pb.desde() + " a " + pb.hasta());
                    r.put("diferencia_de_las_filas_bs", totalB.subtract(totalA));
                    return ResultadoHerramienta.de(filas).conResumen(r)
                            .conNota("Filas ordenadas por el tamano del aporte (positivo = subio, negativo = bajo).");
                });
    }

    Herramienta tasaDevolucion() {
        return Herramienta.de("tasa_devolucion",
                        "Devoluciones de clientes (notas de credito): unidades devueltas sobre vendidas, monto devuelto y"
                                + " cantidad por motivo, en total o por mes, producto o motivo.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "total", "mes", "producto", "motivo"))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> {
                    String g = a.texto("agrupar", "total");
                    return "total".equals(g) ? null : "mes".equals(g)
                            ? Visual.linea("Devoluciones (Bs)", "grupo", "devuelto_bs")
                            : Visual.barras("Devoluciones (Bs)", "grupo", "devuelto_bs");
                })
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String agrupar = a.texto("agrupar", "total");
                    String expr = switch (agrupar) {
                        case "mes" -> "to_char(n.fecha, 'YYYY-MM')";
                        case "producto" -> "COALESCE(p.nombre, 'Producto ' || nd.producto_id)";
                        case "motivo" -> "UPPER(n.motivo)";
                        default -> "'Total'";
                    };
                    Consulta q = new Consulta();
                    q.sql("SELECT " + expr + " AS grupo, COUNT(DISTINCT n.id) AS devoluciones,"
                            + " ROUND(SUM(nd.cantidad), 2) AS unidades_devueltas, ROUND(SUM(nd.subtotal), 2) AS devuelto_bs"
                            + " FROM notas_credito n JOIN notas_credito_detalles nd ON nd.nota_credito_id = n.id"
                            + " LEFT JOIN productos p ON p.id = nd.producto_id"
                            + " WHERE UPPER(n.estado) <> 'ANULADA' AND n.fecha BETWEEN " + q.fecha(p.desde()) + " AND " + q.fecha(p.hasta())
                            + " GROUP BY 1" + orden(agrupar, "devuelto_bs") + " LIMIT " + q.entero(a.entero("limite", 50)));
                    List<Map<String, Object>> filas = c.consultar(q);
                    Consulta v = new Consulta();
                    v.sql("SELECT ROUND(SUM(d.cantidad), 2) AS unidades_vendidas" + FROM_VENTAS
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + v.fecha(p.desde())
                            + " AND v.fecha < " + v.fecha(p.hasta()) + " + 1");
                    BigDecimal vendidas = num(c.unaFila(v).get("unidades_vendidas"));
                    BigDecimal devueltas = filas.stream().map(f -> num(f.get("unidades_devueltas"))).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> r = new LinkedHashMap<>(p.comoMapa());
                    r.put("unidades_vendidas", vendidas);
                    r.put("tasa_devolucion_pct", vendidas.signum() == 0 ? null
                            : devueltas.multiply(BigDecimal.valueOf(100)).divide(vendidas, 2, RoundingMode.HALF_UP));
                    return ResultadoHerramienta.de(filas).conResumen(r);
                });
    }

    // ------------------------------------------------------------------ util

    static BigDecimal num(Object o) {
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    static Object variacionPct(Object a, Object b) {
        BigDecimal x = num(a);
        BigDecimal y = num(b);
        if (x.signum() == 0) return null;
        return y.subtract(x).multiply(BigDecimal.valueOf(100)).divide(x, 1, RoundingMode.HALF_UP);
    }

    static Map<String, Object> totales(List<Map<String, Object>> filas) {
        Map<String, Object> t = new LinkedHashMap<>();
        if (filas.size() <= 1) return t;
        BigDecimal ventas = BigDecimal.ZERO;
        BigDecimal unidades = BigDecimal.ZERO;
        for (Map<String, Object> f : filas) {
            ventas = ventas.add(num(f.get("ventas_bs")));
            unidades = unidades.add(num(f.get("unidades")));
        }
        t.put("total_ventas_bs_de_las_filas", ventas);
        t.put("total_unidades_de_las_filas", unidades);
        return t;
    }
}
