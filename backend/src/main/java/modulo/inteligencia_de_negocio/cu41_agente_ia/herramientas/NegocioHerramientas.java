package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.Comunes.*;
import static modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.VentasHerramientas.num;

/** CU41 - Herramientas de negocio (6): clientes, cartera, compras, resultado y catalogos. */
@Component
public class NegocioHerramientas implements ProveedorHerramientas {

    /** Mismo criterio de "consumidor final" que el servicio de ML. */
    static final String CONSUMIDOR_FINAL = "(TRIM(cl.nit_ci) IN ('0', '00', '000', '0000', '99001', '99002', '99003')"
            + " OR cl.razon_social ILIKE '%sin nombre%' OR cl.razon_social ILIKE '%consumidor final%')";

    @Override
    public List<Herramienta> herramientas() {
        return List.of(clientesNuevos(), cuentasPorCobrar(), cuentasPorPagar(), compras(), resultadoDelPeriodo(), catalogos());
    }

    Herramienta clientesNuevos() {
        return Herramienta.de("clientes_nuevos_y_recurrencia",
                        "Clientes identificados que compraron en el periodo: cuantos, cuantos eran nuevos (primera compra en"
                                + " el periodo), cuantos recurrentes y el % de recurrencia, en total o por mes. Excluye ventas"
                                + " al consumidor final sin nombre.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "total", "mes"))
                .visual(a -> "mes".equals(a.texto("agrupar")) ? Visual.barras("Clientes por mes", "grupo", "nuevos", "recurrentes") : null)
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String g = "mes".equals(a.texto("agrupar")) ? "to_char(v.fecha, 'YYYY-MM')" : "'Total'";
                    Consulta q = new Consulta();
                    q.sql("WITH primera AS (SELECT v.cliente_id, MIN(v.fecha) AS primera FROM ventas v"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' GROUP BY 1)"
                            + " SELECT " + g + " AS grupo, COUNT(DISTINCT v.cliente_id) AS clientes,"
                            + " COUNT(DISTINCT v.cliente_id) FILTER (WHERE pr.primera >= " + q.fecha(p.desde()) + ") AS nuevos,"
                            + " COUNT(DISTINCT v.cliente_id) FILTER (WHERE pr.primera < " + q.fecha(p.desde()) + ") AS recurrentes,"
                            + " ROUND(100.0 * COUNT(DISTINCT v.cliente_id) FILTER (WHERE pr.primera < " + q.fecha(p.desde())
                            + ") / NULLIF(COUNT(DISTINCT v.cliente_id), 0), 1) AS recurrencia_pct"
                            + " FROM ventas v JOIN clientes cl ON cl.id = v.cliente_id JOIN primera pr ON pr.cliente_id = v.cliente_id"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND NOT " + CONSUMIDOR_FINAL
                            + " AND v.fecha >= " + q.fecha(p.desde()) + " AND v.fecha < " + q.fecha(p.hasta()) + " + 1"
                            + " GROUP BY 1 ORDER BY 1");
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa())
                            .conNota("Recurrente = ya habia comprado antes del inicio del periodo.");
                });
    }

    Herramienta cuentasPorCobrar() {
        return Herramienta.de("cuentas_por_cobrar",
                        "Saldos que los clientes deben (cuentas por cobrar con saldo), cuanto esta vencido y por cliente."
                                + " Los nombres de clientes se muestran al usuario pero no se te envian.")
                .parametro("solo_vencidas", Parametro.logico("Solo cuotas vencidas."))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Cuentas por cobrar"))
                .ejecutar((a, c) -> {
                    Consulta q = new Consulta();
                    q.sql("SELECT cl.razon_social AS cliente, COUNT(*) AS cuotas, ROUND(SUM(x.saldo), 2) AS saldo_bs,"
                            + " ROUND(SUM(x.saldo) FILTER (WHERE x.fecha_vencimiento < " + q.fecha(c.hoy()) + "), 2) AS vencido_bs,"
                            + " MIN(x.fecha_vencimiento) AS vencimiento_mas_antiguo"
                            + " FROM cuentas_por_cobrar x LEFT JOIN clientes cl ON cl.id = x.cliente_id"
                            + " WHERE x.saldo > 0 AND UPPER(x.estado) <> 'ANULADA'");
                    if (a.logico("solo_vencidas", false)) q.sql(" AND x.fecha_vencimiento < " + q.fecha(c.hoy()));
                    q.sql(" GROUP BY cl.razon_social ORDER BY saldo_bs DESC LIMIT " + q.entero(a.entero("limite", 20)));
                    Consulta t = new Consulta();
                    t.sql("SELECT ROUND(COALESCE(SUM(saldo), 0), 2) AS saldo_total_bs,"
                            + " ROUND(COALESCE(SUM(saldo) FILTER (WHERE fecha_vencimiento < " + t.fecha(c.hoy()) + "), 0), 2) AS vencido_total_bs,"
                            + " COUNT(DISTINCT cliente_id) AS clientes_con_deuda"
                            + " FROM cuentas_por_cobrar WHERE saldo > 0 AND UPPER(estado) <> 'ANULADA'");
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(c.unaFila(t)).privadas("cliente");
                });
    }

    Herramienta cuentasPorPagar() {
        return Herramienta.de("cuentas_por_pagar",
                        "Lo que la empresa debe a sus proveedores: saldo pendiente, vencido y lo que vence en los proximos"
                                + " 'dias' dias, por proveedor.")
                .parametro("dias", Parametro.entero("Ventana de proximos vencimientos (por defecto 30).", 1, 365))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.barras("Deuda con proveedores (Bs)", "proveedor", "saldo_bs"))
                .ejecutar((a, c) -> {
                    int dias = a.entero("dias", 30);
                    Consulta q = new Consulta();
                    q.sql("SELECT pv.razon_social AS proveedor, ROUND(SUM(x.saldo), 2) AS saldo_bs,"
                            + " ROUND(COALESCE(SUM(x.saldo) FILTER (WHERE x.fecha_vencimiento < " + q.fecha(c.hoy()) + "), 0), 2) AS vencido_bs,"
                            + " ROUND(COALESCE(SUM(x.saldo) FILTER (WHERE x.fecha_vencimiento BETWEEN " + q.fecha(c.hoy())
                            + " AND " + q.fecha(c.hoy().plusDays(dias)) + "), 0), 2) AS por_vencer_bs"
                            + " FROM cuentas_por_pagar x LEFT JOIN proveedores pv ON pv.id = x.proveedor_id"
                            + " WHERE x.saldo > 0 AND UPPER(x.estado) <> 'ANULADA'"
                            + " GROUP BY pv.razon_social ORDER BY saldo_bs DESC LIMIT " + q.entero(a.entero("limite", 20)));
                    return ResultadoHerramienta.de(c.consultar(q))
                            .conResumen(Map.of("hoy", c.hoy().toString(), "ventana_dias", dias));
                });
    }

    Herramienta compras() {
        return Herramienta.de("compras_a_proveedores",
                        "Compras registradas a proveedores (Bs) en un periodo, en total o por mes, proveedor o sucursal.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("agrupar", Parametro.opciones("Agrupacion.", "total", "mes", "proveedor", "sucursal"))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> {
                    String g = a.texto("agrupar", "total");
                    return "total".equals(g) ? null : "mes".equals(g)
                            ? Visual.linea("Compras (Bs)", "grupo", "compras_bs") : Visual.barras("Compras (Bs)", "grupo", "compras_bs");
                })
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    String g = switch (a.texto("agrupar", "total")) {
                        case "mes" -> "to_char(co.fecha, 'YYYY-MM')";
                        case "proveedor" -> "pv.razon_social";
                        case "sucursal" -> "s.nombre";
                        default -> "'Total'";
                    };
                    Consulta q = new Consulta();
                    q.sql("SELECT " + g + " AS grupo, COUNT(*) AS compras, ROUND(SUM(co.total), 2) AS compras_bs"
                            + " FROM compras co LEFT JOIN proveedores pv ON pv.id = co.proveedor_id LEFT JOIN sucursal s ON s.id = co.sucursal_id"
                            + " WHERE UPPER(co.estado) NOT IN ('ANULADA', 'ANULADO') AND co.fecha BETWEEN " + q.fecha(p.desde())
                            + " AND " + q.fecha(p.hasta()) + " GROUP BY 1 ORDER BY 1 LIMIT " + q.entero(a.entero("limite", 50)));
                    return ResultadoHerramienta.de(c.consultar(q)).conResumen(p.comoMapa());
                });
    }

    Herramienta resultadoDelPeriodo() {
        return Herramienta.de("resultado_del_periodo",
                        "Estado de resultados SIMPLIFICADO y estimado de un periodo: ventas, impuestos (IVA 13 % e IT 3 %),"
                                + " devoluciones, costo de ventas (al costo de compra actual), egresos de caja y utilidad"
                                + " estimada. Responde 'cuanto gane'. No reemplaza los estados financieros contables.")
                .parametro("desde", Parametro.DESDE).parametro("hasta", Parametro.HASTA)
                .parametro("sucursal", Parametro.SUCURSAL)
                .visual(a -> Visual.tabla("Resultado del periodo"))
                .ejecutar((a, c) -> {
                    Periodo p = periodo(a, c);
                    Consulta v = new Consulta();
                    v.sql("SELECT ROUND(COALESCE(SUM(v.total), 0), 2) AS ventas, ROUND(COALESCE(SUM(v.iva), 0), 2) AS iva,"
                            + " ROUND(COALESCE(SUM(v.it), 0), 2) AS it FROM ventas v LEFT JOIN sucursal s ON s.id = v.sucursal_id"
                            + " WHERE UPPER(v.estado) <> 'ANULADA' AND v.fecha >= " + v.fecha(p.desde())
                            + " AND v.fecha < " + v.fecha(p.hasta()) + " + 1");
                    v.filtroContiene("s.nombre", a.texto("sucursal"));
                    Map<String, Object> fv = c.unaFila(v);
                    Consulta k = new Consulta();
                    k.sql("SELECT ROUND(COALESCE(SUM(" + COSTO + "), 0), 2) AS costo" + FROM_VENTAS);
                    whereVentas(k, p, new Argumentos(a.texto("sucursal") == null ? Map.of() : Map.of("sucursal", a.texto("sucursal"))));
                    Consulta d = new Consulta();
                    d.sql("SELECT ROUND(COALESCE(SUM(n.total), 0), 2) AS devoluciones FROM notas_credito n LEFT JOIN sucursal s ON s.id = n.sucursal_id"
                            + " WHERE UPPER(n.estado) <> 'ANULADA' AND n.fecha BETWEEN " + d.fecha(p.desde()) + " AND " + d.fecha(p.hasta()));
                    d.filtroContiene("s.nombre", a.texto("sucursal"));
                    Consulta g = new Consulta();
                    g.sql("SELECT ROUND(COALESCE(SUM(m.monto), 0), 2) AS egresos FROM movimientos_caja m"
                            + " JOIN turnos_caja t ON t.id = m.turno_caja_id LEFT JOIN sucursal s ON s.id = t.sucursal_id"
                            + " WHERE UPPER(m.tipo) = 'EGRESO' AND m.fecha_hora >= " + g.fecha(p.desde())
                            + " AND m.fecha_hora < " + g.fecha(p.hasta()) + " + 1");
                    g.filtroContiene("s.nombre", a.texto("sucursal"));

                    BigDecimal ventas = num(fv.get("ventas"));
                    BigDecimal iva = num(fv.get("iva"));
                    BigDecimal it = num(fv.get("it"));
                    BigDecimal devoluciones = num(c.unaFila(d).get("devoluciones"));
                    BigDecimal costo = num(c.unaFila(k).get("costo"));
                    BigDecimal egresos = num(c.unaFila(g).get("egresos"));
                    BigDecimal netas = ventas.subtract(iva).subtract(it).subtract(devoluciones);
                    BigDecimal bruta = netas.subtract(costo);
                    BigDecimal utilidad = bruta.subtract(egresos);
                    List<Map<String, Object>> filas = List.of(
                            linea("Ventas (con impuestos)", ventas), linea("(-) IVA debito", iva), linea("(-) IT", it),
                            linea("(-) Devoluciones", devoluciones), linea("= Ventas netas", netas),
                            linea("(-) Costo de ventas estimado", costo), linea("= Utilidad bruta", bruta),
                            linea("(-) Egresos de caja", egresos), linea("= Utilidad estimada", utilidad));
                    return ResultadoHerramienta.de(filas).conResumen(p.comoMapa())
                            .conNota("Estimacion: costo con el costo de compra actual y gastos = egresos registrados en caja."
                                    + " Para cifras contables oficiales ver Informacion Financiera.");
                });
    }

    private static Map<String, Object> linea(String concepto, BigDecimal monto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("concepto", concepto);
        m.put("monto_bs", monto);
        return m;
    }

    Herramienta catalogos() {
        return Herramienta.de("catalogos",
                        "Nombres validos de sucursales y categorias, y el rango de fechas con ventas registradas. Usala"
                                + " cuando no sepas como se llama una sucursal o categoria, o que periodo tiene datos.")
                .ejecutar((a, c) -> {
                    Consulta s = new Consulta().sql("SELECT 'sucursal' AS tipo, nombre FROM sucursal WHERE activo = TRUE"
                            + " UNION ALL SELECT DISTINCT 'categoria', COALESCE(c.nombre, 'Sin categoria') FROM productos p"
                            + " LEFT JOIN categorias c ON c.id = p.categoria_id WHERE p.activo = TRUE ORDER BY 1, 2");
                    Consulta r = new Consulta().sql("SELECT CAST(MIN(fecha) AS DATE) AS primera_venta, CAST(MAX(fecha) AS DATE) AS ultima_venta,"
                            + " COUNT(*) AS ventas FROM ventas WHERE UPPER(estado) <> 'ANULADA'");
                    return ResultadoHerramienta.de(c.consultar(s)).conResumen(c.unaFila(r));
                });
    }
}
