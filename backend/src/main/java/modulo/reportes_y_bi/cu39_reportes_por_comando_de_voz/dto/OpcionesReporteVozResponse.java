package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import java.util.List;

/** Catálogos que usa la pantalla para refinar un reporte por voz. */
public record OpcionesReporteVozResponse(
        List<MetricaOpcion> metricas,
        List<Referencia> sucursales,
        List<Referencia> productos,
        List<String> ejemplos,
        boolean iaHabilitada) {

    public record MetricaOpcion(
            String codigo,
            String etiqueta,
            boolean usaFechas,
            boolean admiteProducto,
            boolean admiteCriticos,
            List<Opcion> agrupaciones) {}

    public record Opcion(String codigo, String etiqueta) {}

    public record Referencia(Long id, String nombre) {}
}
