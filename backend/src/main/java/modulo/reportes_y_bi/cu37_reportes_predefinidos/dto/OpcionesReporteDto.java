package modulo.reportes_y_bi.cu37_reportes_predefinidos.dto;

import java.util.List;

/** Valores para los combos de filtros de la pantalla. */
public record OpcionesReporteDto(List<Opcion> sucursales, List<Opcion> categorias) {

    public record Opcion(Integer id, String nombre) {
    }
}
