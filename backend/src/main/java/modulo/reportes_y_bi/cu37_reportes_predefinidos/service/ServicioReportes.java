package modulo.reportes_y_bi.cu37_reportes_predefinidos.service;

import comun.config.EvaluadorDePermisos;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.CategoriaReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.Columna;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.FiltroReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.ReportePredefinido;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ColumnaDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.FiltrosReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.OpcionesReporteDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.OpcionesReporteDto.Opcion;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteDefinicionDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteResultadoDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.repository.ReportesRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CU37 - Gestionar Reportes Predefinidos.
 *
 * <p>Cada reporte pertenece a una {@link CategoriaReporte} y solo lo ve quien tiene el
 * permiso de esa categoría. La comprobación usa el mismo {@link EvaluadorDePermisos} que los
 * {@code @PreAuthorize("hasPermission(...)")} del resto del sistema, así que respeta la
 * matriz de roles de la HU-03 y el acceso total del Administrador.
 */
@Service
public class ServicioReportes {

    /** Rango máximo de un reporte de ventas, para que una consulta no recorra años de datos. */
    static final int MAXIMO_DIAS = 366;
    static final int LIMITE_POR_DEFECTO = 10;
    static final int LIMITE_MAXIMO = 100;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportesRepositorio repositorio;
    private final EvaluadorDePermisos evaluador;

    public ServicioReportes(ReportesRepositorio repositorio, EvaluadorDePermisos evaluador) {
        this.repositorio = repositorio;
        this.evaluador = evaluador;
    }

    /** Los reportes que este usuario puede generar, en el orden del catálogo. */
    public List<ReporteDefinicionDto> catalogo(Authentication usuario) {
        return Arrays.stream(ReportePredefinido.values())
                .filter(r -> puedeVer(usuario, r.categoria()))
                .map(ServicioReportes::definicion)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpcionesReporteDto opciones() {
        return new OpcionesReporteDto(repositorio.sucursales(), repositorio.categorias());
    }

    @Transactional(readOnly = true)
    public ReporteResultadoDto generar(String codigo, FiltrosReporte filtros, Authentication usuario) {
        ReportePredefinido reporte = buscar(codigo);
        if (!puedeVer(usuario, reporte.categoria())) {
            throw new AccessDeniedException("Se necesita el permiso " + reporte.categoria().permiso());
        }
        FiltrosReporte f = normalizar(reporte, filtros);
        List<Map<String, Object>> filas = repositorio.consultar(reporte, f);

        return new ReporteResultadoDto(
                reporte.name(),
                reporte.titulo(),
                reporte.categoria().etiqueta(),
                LocalDateTime.now(),
                usuario != null ? usuario.getName() : null,
                describirFiltros(reporte, f),
                columnas(reporte),
                filas,
                totales(reporte, filas));
    }

    /** El mismo reporte en CSV para Excel: separador {@code ;}, decimales con coma y BOM UTF-8. */
    @Transactional(readOnly = true)
    public byte[] exportarCsv(String codigo, FiltrosReporte filtros, Authentication usuario) {
        ReporteResultadoDto r = generar(codigo, filtros, usuario);
        List<Columna> columnas = buscar(codigo).columnas();

        StringBuilder csv = new StringBuilder("﻿");
        csv.append(celda(r.titulo())).append('\n');
        csv.append("Generado;").append(celda(r.generadoEn().format(FECHA_HORA)))
                .append(';').append(celda(r.generadoPor())).append('\n');
        r.filtros().forEach((nombre, valor) -> csv.append(celda(nombre)).append(';').append(celda(valor)).append('\n'));
        csv.append('\n');

        csv.append(String.join(";", columnas.stream().map(c -> celda(c.etiqueta())).toList())).append('\n');
        for (Map<String, Object> fila : r.filas()) {
            csv.append(String.join(";", columnas.stream().map(c -> valorCsv(fila.get(c.clave()))).toList())).append('\n');
        }
        if (!r.totales().isEmpty()) {
            StringBuilder total = new StringBuilder();
            for (int i = 0; i < columnas.size(); i++) {
                Columna c = columnas.get(i);
                if (i > 0) total.append(';');
                if (i == 0 && !c.totalizar()) total.append("Totales");
                else total.append(valorCsv(r.totales().get(c.clave())));
            }
            csv.append(total).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Por ejemplo {@code ventas-por-periodo-2026-10-21.csv}. */
    public String nombreArchivo(String codigo) {
        return buscar(codigo).name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + LocalDate.now() + ".csv";
    }

    boolean puedeVer(Authentication usuario, CategoriaReporte categoria) {
        return usuario != null && evaluador.hasPermission(usuario, categoria.modulo(), categoria.accion());
    }

    private static ReportePredefinido buscar(String codigo) {
        try {
            return ReportePredefinido.valueOf(codigo.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el reporte " + codigo);
        }
    }

    /**
     * Completa y valida los filtros que usa el reporte; los que no usa se descartan. Sin
     * fechas, un reporte de ventas toma el mes en curso.
     */
    FiltrosReporte normalizar(ReportePredefinido reporte, FiltrosReporte filtros) {
        FiltrosReporte f = filtros != null ? filtros : new FiltrosReporte(null, null, null, null, null);
        LocalDate desde = null;
        LocalDate hasta = null;
        if (reporte.usaFechas()) {
            LocalDate hoy = LocalDate.now();
            hasta = f.hasta() != null ? f.hasta() : hoy;
            desde = f.desde() != null ? f.desde() : hasta.withDayOfMonth(1);
            if (desde.isAfter(hasta)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha inicial no puede ser posterior a la fecha final");
            }
            if (desde.plusDays(MAXIMO_DIAS).isBefore(hasta)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El rango de fechas no puede superar los " + MAXIMO_DIAS + " días");
            }
        }
        Integer limite = null;
        if (reporte.filtros().contains(FiltroReporte.LIMITE)) {
            limite = f.limite() != null ? f.limite() : LIMITE_POR_DEFECTO;
            if (limite < 1 || limite > LIMITE_MAXIMO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El límite debe estar entre 1 y " + LIMITE_MAXIMO);
            }
        }
        return new FiltrosReporte(
                desde,
                hasta,
                reporte.filtros().contains(FiltroReporte.SUCURSAL) ? f.sucursalId() : null,
                reporte.filtros().contains(FiltroReporte.CATEGORIA) ? f.categoriaId() : null,
                limite);
    }

    private Map<String, String> describirFiltros(ReportePredefinido reporte, FiltrosReporte f) {
        Map<String, String> filtros = new LinkedHashMap<>();
        if (f.desde() != null) filtros.put("Desde", f.desde().format(FECHA));
        if (f.hasta() != null) filtros.put("Hasta", f.hasta().format(FECHA));
        if (reporte.filtros().contains(FiltroReporte.SUCURSAL)) {
            filtros.put("Sucursal", nombreDe(repositorio.sucursales(), f.sucursalId(), "Todas"));
        }
        if (reporte.filtros().contains(FiltroReporte.CATEGORIA)) {
            filtros.put("Categoría", nombreDe(repositorio.categorias(), f.categoriaId(), "Todas"));
        }
        if (f.limite() != null) filtros.put("Top", String.valueOf(f.limite()));
        return filtros;
    }

    private static String nombreDe(List<Opcion> opciones, Integer id, String siNoHay) {
        if (id == null) return siNoHay;
        return opciones.stream().filter(o -> o.id().equals(id)).map(Opcion::nombre).findFirst()
                .orElse("#" + id);
    }

    /** Suma las columnas totalizables. Si no hay filas, los totales quedan vacíos. */
    static Map<String, Object> totales(ReportePredefinido reporte, List<Map<String, Object>> filas) {
        Map<String, Object> totales = new LinkedHashMap<>();
        if (filas.isEmpty()) return totales;
        for (Columna c : reporte.columnas()) {
            if (!c.totalizar()) continue;
            BigDecimal suma = BigDecimal.ZERO;
            for (Map<String, Object> fila : filas) {
                if (fila.get(c.clave()) instanceof Number n) suma = suma.add(new BigDecimal(n.toString()));
            }
            totales.put(c.clave(), c.tipo() == Columna.Tipo.ENTERO ? suma.longValue() : suma);
        }
        return totales;
    }

    private static List<ColumnaDto> columnas(ReportePredefinido reporte) {
        return reporte.columnas().stream()
                .map(c -> new ColumnaDto(c.clave(), c.etiqueta(), c.tipo().name(), c.totalizar()))
                .toList();
    }

    private static ReporteDefinicionDto definicion(ReportePredefinido r) {
        return new ReporteDefinicionDto(
                r.name(), r.titulo(), r.descripcion(),
                r.categoria().name(), r.categoria().etiqueta(),
                r.filtros().stream().sorted().map(Enum::name).toList(),
                columnas(r));
    }

    private static String valorCsv(Object valor) {
        if (valor == null) return "";
        if (valor instanceof BigDecimal b) return b.toPlainString().replace('.', ',');
        if (valor instanceof Double || valor instanceof Float) {
            return new BigDecimal(valor.toString()).toPlainString().replace('.', ',');
        }
        if (valor instanceof Number n) return n.toString();
        if (valor instanceof LocalDate d) return d.format(FECHA);
        if (valor instanceof LocalDateTime t) return t.format(FECHA_HORA);
        return celda(valor.toString());
    }

    /** Entrecomilla el texto y neutraliza las fórmulas para que Excel no las ejecute (como el CU12). */
    private static String celda(String texto) {
        if (texto == null) return "";
        String limpio = texto.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ');
        if (!limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0)) >= 0) {
            limpio = "'" + limpio;
        }
        return "\"" + limpio + "\"";
    }
}
