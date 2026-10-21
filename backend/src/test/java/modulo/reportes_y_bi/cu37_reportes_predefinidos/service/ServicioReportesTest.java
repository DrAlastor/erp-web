package modulo.reportes_y_bi.cu37_reportes_predefinidos.service;

import comun.config.EvaluadorDePermisos;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.ReportePredefinido;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.FiltrosReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.OpcionesReporteDto.Opcion;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteDefinicionDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteResultadoDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.repository.ReportesRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioReportesTest {

    private static final FiltrosReporte SIN_FILTROS = new FiltrosReporte(null, null, null, null, null);

    private ReportesRepositorio repositorio;
    private EvaluadorDePermisos evaluador;
    private ServicioReportes servicio;
    private final Authentication usuario = new UsernamePasswordAuthenticationToken("cajero", null, List.of());

    @BeforeEach
    void preparar() {
        repositorio = mock(ReportesRepositorio.class);
        evaluador = mock(EvaluadorDePermisos.class);
        servicio = new ServicioReportes(repositorio, evaluador);
        when(repositorio.sucursales()).thenReturn(List.of(new Opcion(1, "Sucursal Central")));
        when(repositorio.categorias()).thenReturn(List.of(new Opcion(4, "Laptops")));
    }

    private void permitir(String modulo) {
        when(evaluador.hasPermission(eq(usuario), eq(modulo), eq("CONSULTAR"))).thenReturn(true);
    }

    private static Map<String, Object> fila(Object... claveValor) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) fila.put((String) claveValor[i], claveValor[i + 1]);
        return fila;
    }

    @Test
    @DisplayName("el catálogo muestra solo las categorías que el usuario tiene permitidas")
    void catalogoFiltradoPorPermiso() {
        permitir("INVENTARIO");

        List<ReporteDefinicionDto> catalogo = servicio.catalogo(usuario);

        assertThat(catalogo).extracting(ReporteDefinicionDto::categoria).containsOnly("INVENTARIO");
        assertThat(catalogo).extracting(ReporteDefinicionDto::codigo)
                .containsExactly("EXISTENCIAS_POR_SUCURSAL", "PRODUCTOS_BAJO_MINIMO", "VALORIZACION_INVENTARIO");
    }

    @Test
    @DisplayName("con ambos permisos aparecen los seis reportes")
    void catalogoCompleto() {
        permitir("INVENTARIO");
        permitir("FACTURACION");

        assertThat(servicio.catalogo(usuario)).hasSize(ReportePredefinido.values().length);
    }

    @Test
    @DisplayName("generar un reporte de ventas sin FACTURACION_CONSULTAR responde 403 y no consulta la base")
    void ventasSinPermiso() {
        permitir("INVENTARIO");

        assertThatThrownBy(() -> servicio.generar("VENTAS_POR_PERIODO", SIN_FILTROS, usuario))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FACTURACION_CONSULTAR");
        verify(repositorio, never()).consultar(any(), any());
    }

    @Test
    @DisplayName("un código desconocido responde 404")
    void reporteInexistente() {
        assertThatThrownBy(() -> servicio.generar("NO_EXISTE", SIN_FILTROS, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("sin fechas, un reporte de ventas toma el mes en curso hasta hoy")
    void fechasPorDefecto() {
        FiltrosReporte f = servicio.normalizar(ReportePredefinido.VENTAS_POR_PERIODO, SIN_FILTROS);

        assertThat(f.hasta()).isEqualTo(LocalDate.now());
        assertThat(f.desde()).isEqualTo(LocalDate.now().withDayOfMonth(1));
    }

    @Test
    @DisplayName("una fecha inicial posterior a la final responde 400")
    void fechasInvertidas() {
        FiltrosReporte f = new FiltrosReporte(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null, null);

        assertThatThrownBy(() -> servicio.normalizar(ReportePredefinido.VENTAS_POR_CAJERO, f))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("un rango mayor a un año responde 400")
    void rangoDemasiadoLargo() {
        FiltrosReporte f = new FiltrosReporte(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), null, null, null);

        assertThatThrownBy(() -> servicio.normalizar(ReportePredefinido.VENTAS_POR_PERIODO, f))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("366");
    }

    @Test
    @DisplayName("el top de productos usa 10 por defecto y rechaza valores fuera de 1 a 100")
    void limiteDelTop() {
        assertThat(servicio.normalizar(ReportePredefinido.PRODUCTOS_MAS_VENDIDOS, SIN_FILTROS).limite()).isEqualTo(10);
        assertThatThrownBy(() -> servicio.normalizar(ReportePredefinido.PRODUCTOS_MAS_VENDIDOS,
                new FiltrosReporte(null, null, null, null, 500)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("los filtros que el reporte no usa se descartan")
    void filtrosAjenosSeDescartan() {
        FiltrosReporte f = servicio.normalizar(ReportePredefinido.VALORIZACION_INVENTARIO,
                new FiltrosReporte(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, 4, 20));

        assertThat(f).isEqualTo(new FiltrosReporte(null, null, 1, null, null));
    }

    @Test
    @DisplayName("el resultado trae los totales de las columnas totalizables y los filtros legibles")
    void resultadoConTotales() {
        permitir("FACTURACION");
        when(repositorio.consultar(eq(ReportePredefinido.VENTAS_POR_CAJERO), any())).thenReturn(List.of(
                fila("cajero", "Ana", "ventas", 2L, "total", new BigDecimal("1500.50"), "ticket_promedio", new BigDecimal("750.25")),
                fila("cajero", "Luis", "ventas", 1L, "total", new BigDecimal("499.50"), "ticket_promedio", new BigDecimal("499.50"))));

        ReporteResultadoDto r = servicio.generar("ventas-por-cajero",
                new FiltrosReporte(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1, null, null), usuario);

        assertThat(r.totales()).containsEntry("ventas", 3L).containsEntry("total", new BigDecimal("2000.00"));
        assertThat(r.totales()).doesNotContainKey("ticket_promedio");
        assertThat(r.filtros()).containsEntry("Desde", "01/01/2026").containsEntry("Sucursal", "Sucursal Central");
        assertThat(r.generadoPor()).isEqualTo("cajero");
    }

    @Test
    @DisplayName("el CSV usa ; y coma decimal, y neutraliza fórmulas de Excel")
    void exportarCsv() {
        permitir("INVENTARIO");
        when(repositorio.consultar(eq(ReportePredefinido.PRODUCTOS_BAJO_MINIMO), any())).thenReturn(List.of(
                fila("sucursal", "Central", "codigo", "=CMD()", "producto", "Laptop", "stock", new BigDecimal("1.50"),
                        "minimo", new BigDecimal("3.00"), "faltante", new BigDecimal("1.50"))));

        String csv = new String(servicio.exportarCsv("PRODUCTOS_BAJO_MINIMO", SIN_FILTROS, usuario), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿\"Productos bajo stock mínimo\"");
        assertThat(csv).contains("\"Sucursal\";\"Código\";\"Producto\";\"Stock\";\"Mínimo\";\"Faltante\"");
        assertThat(csv).contains("\"Central\";\"'=CMD()\";\"Laptop\";1,50;3,00;1,50");
        assertThat(csv).contains("Totales;;;;;1,50");
    }

    @Test
    @DisplayName("sin filas, el reporte sale vacío y sin totales")
    void sinDatos() {
        permitir("INVENTARIO");
        when(repositorio.consultar(any(), any())).thenReturn(List.of());

        ReporteResultadoDto r = servicio.generar("EXISTENCIAS_POR_SUCURSAL", SIN_FILTROS, usuario);

        assertThat(r.filas()).isEmpty();
        assertThat(r.totales()).isEmpty();
        assertThat(r.filtros()).containsEntry("Sucursal", "Todas").containsEntry("Categoría", "Todas");
    }
}
