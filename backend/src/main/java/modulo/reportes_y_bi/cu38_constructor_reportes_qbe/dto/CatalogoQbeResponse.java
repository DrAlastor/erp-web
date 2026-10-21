package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto;

import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;

import java.util.List;

/** Entidades, campos, operadores y agregaciones que la pantalla ofrece al construir un reporte. */
public record CatalogoQbeResponse(
        List<EntidadDto> entidades,
        List<OperadorDto> operadores,
        List<AgregacionDto> agregaciones,
        List<OpcionDto> granularidades) {

    public record EntidadDto(String codigo, String etiqueta, String modulo, List<CampoDto> campos) {}

    public record CampoDto(String clave, String etiqueta, TipoCampo tipo) {}

    public record OperadorDto(String codigo, String etiqueta, int valores, List<TipoCampo> tipos) {}

    public record AgregacionDto(String codigo, String etiqueta, List<TipoCampo> tipos) {}

    public record OpcionDto(String codigo, String etiqueta) {}
}
