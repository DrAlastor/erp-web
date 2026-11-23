package modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU41 - Herramientas que leen los resultados del Machine Learning (CU40, tablas ml_*). El agente
 * no entrena modelos: solo lee lo que dejo el ultimo entrenamiento.
 */
@Component
public class ModelosHerramientas implements ProveedorHerramientas {

    static final String SIN_ENTRENAR = "Los modelos de Machine Learning todavia no se entrenaron para esta empresa:"
            + " el dato no existe. Sugiere entrenarlos en Inteligencia de Negocio > Panel de Modelos.";
    static final String SIN_RESULTADOS = "Los modelos estan entrenados, pero no hay resultados para estos filtros.";
    private static final int HORIZONTE_MENSUAL = 6;
    private static final int HORIZONTE_SEMANAL = 12;

    @Override
    public List<Herramienta> herramientas() {
        return List.of(pronosticarVentas(), sugerenciaDeCompra(), clientesEnRiesgo(), segmentos(), canasta(), anomalias());
    }

    /** Si no hay filas, distingue "nunca se entrenaron" de "no hay resultados para estos filtros". */
    static ResultadoHerramienta conAvisoDeModelos(ResultadoHerramienta r, ContextoHerramienta c) {
        if (!r.filas().isEmpty()) return r.conResumen(Map.of("fuente", "Ultimo entrenamiento de los modelos de ML"));
        return entrenados(c) ? r.conNota(SIN_RESULTADOS) : r.noDisponible(SIN_ENTRENAR);
    }

    static boolean entrenados(ContextoHerramienta c) {
        Map<String, Object> f = c.unaFila(new Consulta().sql(
                "SELECT to_regclass('public.ml_ejecucion') IS NOT NULL AS existe"));
        if (!Boolean.TRUE.equals(f.get("existe"))) return false;
        return !c.consultar(new Consulta().sql(
                "SELECT 1 FROM ml_ejecucion WHERE estado IN ('OK', 'PARCIAL') LIMIT 1")).isEmpty();
    }

    Herramienta pronosticarVentas() {
        return Herramienta.de("pronosticar_ventas",
                        "Pronostico de ventas futuras (Bs) con su rango del 80 % (bajo-alto), mensual o semanal, para toda"
                                + " la empresa, una sucursal o una categoria. Precalculado: 6 meses o 12 semanas; para un"
                                + " horizonte mayor (hasta 24) se calcula al momento.")
                .parametro("frecuencia", Parametro.opciones("Mensual o semanal.", "MENSUAL", "SEMANAL"))
                .parametro("horizonte", Parametro.entero("Cuantos periodos pronosticar (1 a 24).", 1, 24))
                .parametro("sucursal", Parametro.SUCURSAL).parametro("categoria", Parametro.CATEGORIA)
                .visual(a -> Visual.pronostico("Pronostico de ventas (Bs)", "periodo", "valor", "bajo", "alto"))
                .ejecutar((a, c) -> {
                    String f = a.texto("frecuencia", "MENSUAL");
                    int precalculado = "MENSUAL".equals(f) ? HORIZONTE_MENSUAL : HORIZONTE_SEMANAL;
                    int h = a.entero("horizonte", precalculado);
                    if (a.texto("sucursal") != null && a.texto("categoria") != null) {
                        throw new ErrorDeParametros("Usa sucursal o categoria, no ambas.");
                    }
                    String ambito = "TOTAL";
                    String clave = "TOTAL";
                    String etiqueta = "Toda la empresa";
                    if (a.texto("sucursal") != null) {
                        Map<String, Object> s = unico(c, "SELECT id, nombre FROM sucursal WHERE activo = TRUE AND ", "nombre", a.texto("sucursal"), "sucursal");
                        ambito = "SUCURSAL";
                        clave = String.valueOf(s.get("id"));
                        etiqueta = String.valueOf(s.get("nombre"));
                    } else if (a.texto("categoria") != null) {
                        Map<String, Object> s = unico(c, "SELECT DISTINCT COALESCE(NULLIF(TRIM(c.nombre), ''), 'Sin categoria') AS nombre"
                                + " FROM productos p LEFT JOIN categorias c ON c.id = p.categoria_id WHERE ", "c.nombre", a.texto("categoria"), "categoria");
                        ambito = "CATEGORIA";
                        clave = String.valueOf(s.get("nombre"));
                        etiqueta = clave;
                    }
                    Map<String, Object> resumen = new LinkedHashMap<>();
                    resumen.put("serie", etiqueta);
                    resumen.put("frecuencia", f);

                    if (h > precalculado) {
                        JsonNode r = c.ml().pronostico(c.empresa(), ambito, clave, f, h);
                        if (!r.path("disponible").asBoolean(false)) {
                            return ResultadoHerramienta.de(List.of()).conResumen(resumen)
                                    .noDisponible("No se pudo calcular el pronostico al momento: " + r.path("motivo").asText("servicio no disponible"));
                        }
                        List<Map<String, Object>> filas = new ArrayList<>();
                        for (JsonNode p : r.path("puntos")) {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("periodo", p.path("periodo").asText());
                            m.put("valor", p.path("valor").decimalValue());
                            m.put("bajo", p.path("bajo").decimalValue());
                            m.put("alto", p.path("alto").decimalValue());
                            filas.add(m);
                        }
                        resumen.put("modelo", r.path("modelo").asText());
                        resumen.put("origen", "calculado al momento por el servicio de ML");
                        return ResultadoHerramienta.de(filas).conResumen(resumen);
                    }
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).conResumen(resumen).noDisponible(SIN_ENTRENAR);
                    Consulta q = new Consulta();
                    String etiquetaPeriodo = "MENSUAL".equals(f)
                            ? "to_char(periodo, 'YYYY-MM')"
                            : "'semana del ' || to_char(periodo, 'YYYY-MM-DD')";
                    String inicioActual = "MENSUAL".equals(f)
                            ? "date_trunc('month', " + q.fecha(c.hoy()) + ")"
                            : "date_trunc('week', " + q.fecha(c.hoy()) + ")";
                    q.sql("SELECT to_char(periodo, 'YYYY-MM-DD') AS periodo, " + etiquetaPeriodo + " AS mes_o_semana,"
                            + " (periodo = CAST(" + inicioActual + " AS DATE)) AS en_curso, valor, bajo, alto, modelo FROM ml_pronostico"
                            + " WHERE ambito = " + q.valor(ambito) + " AND clave = " + q.valor(clave)
                            + " AND frecuencia = " + q.valor(f) + " ORDER BY periodo LIMIT " + q.entero(h));
                    resumen.put("hoy", c.hoy().toString());
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)).conResumen(resumen), c)
                            .conNota("Cada fila es el pronostico del periodo COMPLETO indicado en mes_o_semana (usa esa etiqueta"
                                    + " tal cual, no la cambies). en_curso=true es el periodo actual, que ya empezo: aclaralo."
                                    + " Da siempre el rango bajo-alto (80 %), no solo el valor central.");
                });
    }

    /** Resuelve un nombre parcial a exactamente UNA fila; si hay 0 o varias, error para que Gemini corrija. */
    private static Map<String, Object> unico(ContextoHerramienta c, String select, String columna, String texto, String que) {
        Consulta q = new Consulta();
        q.sql(select + q.contiene(columna, texto));
        List<Map<String, Object>> filas = c.consultar(q);
        if (filas.size() == 1) return filas.getFirst();
        if (filas.isEmpty()) throw new ErrorDeParametros("No hay ninguna " + que + " que coincida con '" + texto + "'. Usa catalogos.");
        throw new ErrorDeParametros("'" + texto + "' coincide con varias: "
                + filas.stream().map(f -> String.valueOf(f.get("nombre"))).toList() + ". Se mas especifico.");
    }

    Herramienta sugerenciaDeCompra() {
        return Herramienta.de("sugerencia_de_compra",
                        "Que productos reponer, en que sucursal y cuantas unidades (demanda esperada de 2 meses + stock de"
                                + " seguridad - stock actual), con el costo estimado de la compra.")
                .parametro("sucursal", Parametro.SUCURSAL).parametro("categoria", Parametro.CATEGORIA)
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Sugerencia de compra"))
                .ejecutar((a, c) -> {
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).noDisponible(SIN_ENTRENAR);
                    Consulta q = new Consulta();
                    q.sql("SELECT p.nombre AS producto, COALESCE(c.nombre, 'Sin categoria') AS categoria, s.nombre AS sucursal,"
                            + " g.demanda_esperada, g.stock_actual, g.cantidad_sugerida,"
                            + " ROUND(g.cantidad_sugerida * COALESCE(p.costo_compra, 0), 2) AS costo_estimado_bs"
                            + " FROM ml_sugerencia_compra g LEFT JOIN productos p ON p.id = g.producto_id"
                            + " LEFT JOIN categorias c ON c.id = p.categoria_id LEFT JOIN sucursal s ON s.id = g.sucursal_id WHERE 1 = 1");
                    q.filtroContiene("s.nombre", a.texto("sucursal"));
                    q.filtroContiene("c.nombre", a.texto("categoria"));
                    q.sql(" ORDER BY costo_estimado_bs DESC LIMIT " + q.entero(a.entero("limite", 20)));
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)), c);
                });
    }

    Herramienta clientesEnRiesgo() {
        return Herramienta.de("clientes_en_riesgo",
                        "Clientes con probabilidad de NO volver a comprar en 90 dias (riesgo de abandono ALTO, MEDIO o BAJO)"
                                + " con los factores que mas pesan, mas el conteo por nivel. Los nombres se muestran al usuario"
                                + " pero no se te envian: refierete a ellos de forma general.")
                .parametro("nivel", Parametro.opciones("Nivel de riesgo.", "ALTO", "MEDIO", "BAJO"))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Clientes en riesgo de abandono"))
                .ejecutar((a, c) -> {
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).noDisponible(SIN_ENTRENAR);
                    Consulta q = new Consulta();
                    q.sql("SELECT cl.razon_social AS cliente, r.probabilidad, r.nivel, CAST(r.factores AS TEXT) AS factores"
                            + " FROM ml_riesgo_abandono r LEFT JOIN clientes cl ON cl.id = r.cliente_id WHERE 1 = 1");
                    if (a.texto("nivel") != null) q.sql(" AND r.nivel = " + q.valor(a.texto("nivel")));
                    q.sql(" ORDER BY r.probabilidad DESC LIMIT " + q.entero(a.entero("limite", 20)));
                    Consulta n = new Consulta().sql("SELECT"
                            + " COUNT(*) FILTER (WHERE nivel = 'ALTO') AS alto, COUNT(*) FILTER (WHERE nivel = 'MEDIO') AS medio,"
                            + " COUNT(*) FILTER (WHERE nivel = 'BAJO') AS bajo FROM ml_riesgo_abandono");
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)).conResumen(c.unaFila(n)), c).privadas("cliente");
                });
    }

    Herramienta segmentos() {
        return Herramienta.de("segmentos_de_clientes",
                        "Grupos de clientes encontrados por clustering (RFM): cuantos clientes tiene cada segmento, dias desde"
                                + " su ultima compra, compras y gasto promedio y total.")
                .visual(a -> Visual.torta("Clientes por segmento", "segmento", "clientes"))
                .ejecutar((a, c) -> {
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).noDisponible(SIN_ENTRENAR);
                    Consulta q = new Consulta().sql("SELECT segmento, COUNT(*) AS clientes,"
                            + " ROUND(AVG(recencia_dias), 0) AS dias_desde_ultima_compra, ROUND(AVG(frecuencia), 1) AS compras_promedio,"
                            + " ROUND(AVG(monto), 2) AS gasto_promedio_bs, ROUND(SUM(monto), 2) AS gasto_total_bs"
                            + " FROM ml_segmento_cliente GROUP BY segmento ORDER BY gasto_total_bs DESC");
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)), c);
                });
    }

    Herramienta canasta() {
        return Herramienta.de("productos_que_se_compran_juntos",
                        "Reglas de canasta de mercado: si un cliente compra A, que tan probable es que tambien compre B"
                                + " (confianza) y cuanto mas que por azar (lift). Sirve para combos, promociones y ubicacion.")
                .parametro("producto", Parametro.texto("Producto A (o parte del nombre). Omitir para las reglas mas fuertes."))
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Productos que se compran juntos"))
                .ejecutar((a, c) -> {
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).noDisponible(SIN_ENTRENAR);
                    Consulta q = new Consulta();
                    q.sql("SELECT producto_a, producto_b, confianza, lift, ventas AS ventas_juntos FROM ml_asociacion WHERE 1 = 1");
                    q.filtroContiene("producto_a", a.texto("producto"));
                    q.sql(" ORDER BY lift DESC, confianza DESC LIMIT " + q.entero(a.entero("limite", 15)));
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)), c)
                            .conNota("confianza = P(B | A); lift > 1 = se compran juntos mas que por azar.");
                });
    }

    Herramienta anomalias() {
        return Herramienta.de("anomalias_recientes",
                        "Dias en que una sucursal tuvo una cantidad de ventas muy distinta de lo esperado (deteccion de"
                                + " anomalias con STL + Isolation Forest). Desviacion positiva = mas ventas de lo normal.")
                .parametro("desde", Parametro.fecha("Desde que fecha. Si se omite: ultimos 180 dias."))
                .parametro("hasta", Parametro.HASTA).parametro("sucursal", Parametro.SUCURSAL)
                .parametro("limite", Parametro.LIMITE)
                .visual(a -> Visual.tabla("Dias atipicos"))
                .ejecutar((a, c) -> {
                    if (!entrenados(c)) return ResultadoHerramienta.de(List.of()).noDisponible(SIN_ENTRENAR);
                    var desde = a.fecha("desde", c.hoy().minusDays(180));
                    var hasta = a.fecha("hasta", c.hoy());
                    Consulta q = new Consulta();
                    q.sql("SELECT to_char(x.fecha, 'YYYY-MM-DD') AS fecha, s.nombre AS sucursal, x.valor_real AS ventas_reales,"
                            + " x.valor_esperado AS ventas_esperadas, x.desviacion, x.gravedad"
                            + " FROM ml_anomalia x LEFT JOIN sucursal s ON s.id = x.sucursal_id"
                            + " WHERE x.fecha BETWEEN " + q.fecha(desde) + " AND " + q.fecha(hasta));
                    q.filtroContiene("s.nombre", a.texto("sucursal"));
                    q.sql(" ORDER BY x.fecha DESC LIMIT " + q.entero(a.entero("limite", 30)));
                    return conAvisoDeModelos(ResultadoHerramienta.de(c.consultar(q)), c)
                            .conResumen(Map.of("periodo_desde", desde.toString(), "periodo_hasta", hasta.toString()));
                });
    }
}
