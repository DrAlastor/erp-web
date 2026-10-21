package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.TipoDato;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.Visualizacion;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ColumnaReporte;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.OpcionesReporteVozResponse;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ReporteVozResponse;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Entidad;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Filtros;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Resultado;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU39 - Reportes por comando de voz. Recibe la frase ya transcrita, la hace interpretar por
 * N8N (o por el intérprete local si N8N no está disponible), valida la consulta resultante
 * contra el catálogo y la ejecuta en modo lectura.
 *
 * <p>No tiene tabla propia: el registro de auditoría de cada comando (HU39 / bitácora) se
 * escribe en el log de la aplicación con el usuario, la frase, la consulta y las filas.
 */
@Service
@RequiredArgsConstructor
public class ReporteVozService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA.CU39");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMITE_POR_DEFECTO = 10;
    private static final int LIMITE_SERIES = 366;
    private static final int MAXIMO_BARRAS = 15;
    private static final int PRODUCTOS_CONTEXTO = 200;

    private static final List<String> EJEMPLOS = List.of(
            "Muéstrame las ventas de la sucursal Central del mes pasado",
            "Ventas por día de esta semana",
            "Top 5 productos más vendidos este año",
            "Ventas por vendedor del último mes",
            "Productos con stock bajo el mínimo",
            "Cuentas por cobrar vencidas por cliente",
            "Compras por proveedor de este año",
            "Movimientos de kardex por tipo de los últimos 30 días");

    private final ReporteVozRepository repository;
    private final InterpreteLocalVoz interpreteLocal;
    private final OrquestadorN8nCliente n8n;

    @Transactional(readOnly = true)
    public OpcionesReporteVozResponse opciones() {
        List<OpcionesReporteVozResponse.MetricaOpcion> metricas = Arrays.stream(MetricaVoz.values())
                .map(m -> new OpcionesReporteVozResponse.MetricaOpcion(
                        m.name(), m.etiqueta(), m.usaFechas(), m.admiteProducto(), m.admiteCriticos(),
                        m.agrupaciones().stream()
                                .map(a -> new OpcionesReporteVozResponse.Opcion(a.name(), a.etiqueta()))
                                .toList()))
                .toList();
        return new OpcionesReporteVozResponse(
                metricas,
                referencias(repository.sucursalesActivas()),
                referencias(repository.productosActivos(PRODUCTOS_CONTEXTO)),
                EJEMPLOS,
                n8n.habilitado());
    }

    /** HU39.2 + HU39.3: interpreta la frase y devuelve el reporte listo para mostrar. */
    @Transactional(readOnly = true)
    public ReporteVozResponse interpretar(String texto) {
        String frase = texto.trim();
        LocalDate hoy = LocalDate.now();
        List<Entidad> sucursales = repository.sucursalesActivas();
        List<Entidad> productos = repository.productosActivos(PRODUCTOS_CONTEXTO);
        List<String> advertencias = new ArrayList<>();

        String origen = "N8N";
        ConsultaReporteVoz consulta = n8n.interpretar(frase, sucursales, productos, hoy).orElse(null);
        if (consulta == null) {
            origen = "LOCAL";
            if (n8n.habilitado()) {
                advertencias.add("El orquestador N8N no respondió; la frase se interpretó con el intérprete local.");
            }
            consulta = interpreteLocal.interpretar(frase, sucursales, productos, hoy)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "No se entendió qué reporte deseas. Prueba con frases como: \""
                                    + EJEMPLOS.get(0) + "\" o \"" + EJEMPLOS.get(2) + "\"."));
        }
        return generar(frase, origen, consulta, advertencias);
    }

    /** Ejecuta una consulta estructurada: la usa la pantalla al refinar el reporte. */
    @Transactional(readOnly = true)
    public ReporteVozResponse ejecutar(ConsultaReporteVoz consulta) {
        return generar(null, "MANUAL", consulta, new ArrayList<>());
    }

    /** El mismo reporte en CSV para Excel (punto y coma, coma decimal y BOM). */
    @Transactional(readOnly = true)
    public byte[] exportarCsv(ConsultaReporteVoz consulta) {
        ReporteVozResponse r = ejecutar(consulta);
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(celda(r.getTitulo())).append('\n');
        csv.append(celda(r.getInterpretacion())).append("\n\n");
        csv.append(String.join(";", r.getColumnas().stream().map(c -> celda(c.etiqueta())).toList())).append('\n');
        for (Map<String, Object> fila : r.getFilas()) {
            List<String> celdas = new ArrayList<>();
            for (ColumnaReporte c : r.getColumnas()) {
                celdas.add(valorCsv(fila.get(c.clave()), c.tipo()));
            }
            csv.append(String.join(";", celdas)).append('\n');
        }
        if (!r.getTotales().isEmpty()) {
            List<String> celdas = new ArrayList<>();
            for (ColumnaReporte c : r.getColumnas()) {
                BigDecimal total = r.getTotales().get(c.clave());
                celdas.add(c.clave().equals("grupo") ? "Total" : total == null ? "" : valorCsv(total, c.tipo()));
            }
            csv.append(String.join(";", celdas)).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ReporteVozResponse generar(
            String frase, String origen, ConsultaReporteVoz pedida, List<String> advertencias) {
        ConsultaReporteVoz consulta = normalizar(pedida, advertencias);
        MetricaVoz metrica = consulta.getMetrica();
        AgrupacionVoz agrupacion = consulta.getAgrupacion();

        Resultado resultado = repository.ejecutar(metrica, agrupacion, new Filtros(
                consulta.getFechaInicio(),
                consulta.getFechaFin(),
                consulta.getSucursalId(),
                consulta.getProductoId(),
                Boolean.TRUE.equals(consulta.getSoloCriticos()),
                consulta.getLimite() != null ? consulta.getLimite() : LIMITE_SERIES));

        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        if (agrupacion != AgrupacionVoz.NINGUNA) {
            for (String clave : resultado.acumulables()) {
                BigDecimal suma = BigDecimal.ZERO;
                for (Map<String, Object> fila : resultado.filas()) {
                    if (fila.get(clave) instanceof Number n) {
                        suma = suma.add(new BigDecimal(n.toString()));
                    }
                }
                totales.put(clave, suma);
            }
        }
        if (resultado.filas().isEmpty()) {
            advertencias.add("No hay datos que coincidan con la consulta. Prueba con otro rango de fechas o sin filtros.");
        }

        String sucursal = consulta.getSucursalId() == null ? null
                : repository.nombreSucursal(consulta.getSucursalId()).orElse(null);
        String producto = consulta.getProductoId() == null ? null
                : repository.nombreProducto(consulta.getProductoId()).orElse(null);

        ReporteVozResponse respuesta = ReporteVozResponse.builder()
                .textoOriginal(frase)
                .origen(origen)
                .consulta(consulta)
                .titulo(titulo(consulta))
                .interpretacion(interpretacion(consulta, sucursal, producto))
                .visualizacion(visualizacion(agrupacion, resultado.filas().size()))
                .columnas(resultado.columnas())
                .filas(resultado.filas())
                .totales(totales)
                .advertencias(advertencias)
                .generadoEn(LocalDateTime.now())
                .build();

        auditar(respuesta);
        return respuesta;
    }

    /**
     * Completa los valores por defecto y descarta, con una advertencia, lo que la métrica no
     * admite. Así una interpretación imperfecta de N8N degrada el reporte en vez de romperlo.
     */
    private ConsultaReporteVoz normalizar(ConsultaReporteVoz c, List<String> advertencias) {
        if (c == null || c.getMetrica() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica qué reporte deseas consultar.");
        }
        MetricaVoz metrica = c.getMetrica();
        ConsultaReporteVoz.ConsultaReporteVozBuilder n = c.toBuilder();

        AgrupacionVoz agrupacion = c.getAgrupacion();
        if (agrupacion == null) {
            agrupacion = metrica.agrupacionPorDefecto();
        } else if (!metrica.admite(agrupacion)) {
            advertencias.add("El reporte de " + metrica.etiqueta().toLowerCase() + " no se puede agrupar por "
                    + agrupacion.etiqueta().toLowerCase() + "; se agrupó por "
                    + metrica.agrupacionPorDefecto().etiqueta().toLowerCase() + ".");
            agrupacion = metrica.agrupacionPorDefecto();
        }
        n.agrupacion(agrupacion);

        LocalDate inicio = c.getFechaInicio();
        LocalDate fin = c.getFechaFin();
        if (!metrica.usaFechas() && (inicio != null || fin != null)) {
            advertencias.add("El stock se muestra a la fecha actual; se ignoró el rango de fechas.");
            inicio = null;
            fin = null;
        }
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            LocalDate tmp = inicio;
            inicio = fin;
            fin = tmp;
            advertencias.add("La fecha de inicio era posterior a la de fin; se invirtió el rango.");
        }
        n.fechaInicio(inicio).fechaFin(fin);

        if (c.getSucursalId() != null && repository.nombreSucursal(c.getSucursalId()).isEmpty()) {
            advertencias.add("La sucursal indicada no existe; se muestran todas las sucursales.");
            n.sucursalId(null);
        }
        if (c.getProductoId() != null) {
            if (!metrica.admiteProducto()) {
                advertencias.add("El reporte de " + metrica.etiqueta().toLowerCase()
                        + " no se filtra por producto; se ignoró el producto.");
                n.productoId(null);
            } else if (repository.nombreProducto(c.getProductoId()).isEmpty()) {
                advertencias.add("El producto indicado no existe; se muestran todos los productos.");
                n.productoId(null);
            }
        }
        n.soloCriticos(metrica.admiteCriticos() && Boolean.TRUE.equals(c.getSoloCriticos()) ? Boolean.TRUE : null);

        Integer limite = c.getLimite();
        if (agrupacion == AgrupacionVoz.NINGUNA || agrupacion.temporal()) {
            limite = null;
        } else if (limite == null || limite < 1 || limite > 100) {
            limite = LIMITE_POR_DEFECTO;
        }
        n.limite(limite);
        return n.build();
    }

    /** KPI para un total, líneas para series de tiempo y barras si caben; si no, tabla. */
    static Visualizacion visualizacion(AgrupacionVoz agrupacion, int filas) {
        if (agrupacion == AgrupacionVoz.NINGUNA) return Visualizacion.KPI;
        if (filas == 0) return Visualizacion.TABLA;
        if (agrupacion.temporal()) return filas >= 2 ? Visualizacion.LINEAS : Visualizacion.BARRAS;
        return filas <= MAXIMO_BARRAS ? Visualizacion.BARRAS : Visualizacion.TABLA;
    }

    private static String titulo(ConsultaReporteVoz c) {
        String titulo = c.getMetrica().etiqueta();
        if (c.getAgrupacion() != AgrupacionVoz.NINGUNA) {
            titulo += " por " + c.getAgrupacion().etiqueta().toLowerCase();
        }
        return titulo;
    }

    private static String interpretacion(ConsultaReporteVoz c, String sucursal, String producto) {
        List<String> partes = new ArrayList<>();
        partes.add(c.getMetrica().etiqueta());
        partes.add(c.getAgrupacion() == AgrupacionVoz.NINGUNA ? "total general"
                : "agrupado por " + c.getAgrupacion().etiqueta().toLowerCase());
        partes.add(sucursal != null ? "sucursal " + sucursal : "todas las sucursales");
        if (producto != null) {
            partes.add("producto " + producto);
        }
        if (c.getMetrica().usaFechas()) {
            partes.add(rango(c.getFechaInicio(), c.getFechaFin()));
        } else {
            partes.add("a la fecha actual");
        }
        if (Boolean.TRUE.equals(c.getSoloCriticos())) {
            partes.add(c.getMetrica() == MetricaVoz.STOCK ? "solo stock bajo el mínimo" : "solo cuentas vencidas");
        }
        if (c.getLimite() != null) {
            partes.add("primeros " + c.getLimite());
        }
        return String.join(" · ", partes);
    }

    private static String rango(LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) return "todo el historial";
        if (inicio == null) return "hasta el " + fin.format(FORMATO_FECHA);
        if (fin == null) return "desde el " + inicio.format(FORMATO_FECHA);
        if (inicio.equals(fin)) return "el " + inicio.format(FORMATO_FECHA);
        return "del " + inicio.format(FORMATO_FECHA) + " al " + fin.format(FORMATO_FECHA);
    }

    private void auditar(ReporteVozResponse r) {
        String usuario = UsuarioActual.buscar().map(UsuarioPrincipal::email).orElse("anonimo");
        ConsultaReporteVoz c = r.getConsulta();
        auditoria.info("REPORTE_VOZ usuario={} origen={} texto=\"{}\" metrica={} agrupacion={} desde={} hasta={} "
                        + "sucursal={} producto={} criticos={} filas={}",
                usuario, r.getOrigen(), sinSaltos(r.getTextoOriginal()), c.getMetrica(), c.getAgrupacion(),
                c.getFechaInicio(), c.getFechaFin(), c.getSucursalId(), c.getProductoId(), c.getSoloCriticos(),
                r.getFilas().size());
    }

    private static List<OpcionesReporteVozResponse.Referencia> referencias(List<Entidad> entidades) {
        return entidades.stream().map(e -> new OpcionesReporteVozResponse.Referencia(e.id(), e.nombre())).toList();
    }

    /** Evita que la frase dictada inyecte líneas falsas en el log de auditoría. */
    private static String sinSaltos(String texto) {
        return texto == null ? "" : texto.replace('\n', ' ').replace('\r', ' ').replace("\"", "'");
    }

    private static String valorCsv(Object valor, TipoDato tipo) {
        if (valor == null) return "";
        if (tipo == TipoDato.NUMERO || tipo == TipoDato.MONEDA) {
            return valor.toString().replace('.', ',');
        }
        if (valor instanceof LocalDate d) return d.format(FORMATO_FECHA);
        return celda(valor.toString());
    }

    /** Entrecomilla el texto y neutraliza las fórmulas para que Excel no las ejecute. */
    private static String celda(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ');
        if (!limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0)) >= 0) {
            limpio = "'" + limpio;
        }
        return "\"" + limpio + "\"";
    }
}
