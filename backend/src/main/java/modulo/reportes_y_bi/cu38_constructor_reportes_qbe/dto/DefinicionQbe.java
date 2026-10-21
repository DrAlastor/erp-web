package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Agregacion;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Granularidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.OperadorFiltro;

import java.util.List;

/**
 * Definición de un reporte QBE: entidad base, columnas en el orden de visualización,
 * filtros combinados con AND u OR y orden del resultado. Es lo que se ejecuta en la vista
 * previa y lo que se guarda como plantilla en {@code REPORTES_QBE}.
 */
public record DefinicionQbe(
        @NotBlank(message = "Selecciona la entidad del reporte.")
        String entidad,

        @NotEmpty(message = "Selecciona al menos un campo para el reporte.")
        @Size(max = 20, message = "Un reporte puede tener como máximo 20 columnas.")
        List<@Valid Columna> columnas,

        @Size(max = 20, message = "Un reporte puede tener como máximo 20 filtros.")
        List<@Valid Filtro> filtros,

        OperadorLogico operadorLogico,

        @Size(max = 5, message = "Puedes ordenar por un máximo de 5 columnas.")
        List<@Valid Orden> orden) {

    public enum OperadorLogico { AND, OR }

    /** Columna del reporte; con agregación se vuelve un cálculo y el resto agrupa. */
    public record Columna(
            @NotBlank(message = "Cada columna necesita un campo.") String campo,
            Agregacion agregacion,
            Granularidad granularidad) {}

    /** Condición de filtro. {@code valor2} solo se usa con el operador ENTRE. */
    public record Filtro(
            @NotBlank(message = "Cada filtro necesita un campo.") String campo,
            OperadorFiltro operador,
            @Size(max = 200, message = "El valor de un filtro no puede superar 200 caracteres.") String valor,
            @Size(max = 200, message = "El valor de un filtro no puede superar 200 caracteres.") String valor2) {}

    /** Orden por posición de columna (desde 0). */
    public record Orden(Integer columna, boolean descendente) {}
}
