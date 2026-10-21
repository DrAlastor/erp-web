package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service;

import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Agregacion;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Granularidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.OperadorFiltro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Columna;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Filtro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.OperadorLogico;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Orden;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ConstructorConsultaQbe.ConsultaSql;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstructorConsultaQbeTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    private final ConstructorConsultaQbe constructor = new ConstructorConsultaQbe();

    private ConsultaSql construir(List<Columna> columnas, List<Filtro> filtros, OperadorLogico op, List<Orden> orden) {
        return constructor.construir(new DefinicionQbe("VENTAS", columnas, filtros, op, orden), HOY);
    }

    @Test
    void columnasSinAgregacionAgrupanCuandoHayUnCalculo() {
        ConsultaSql c = construir(
                List.of(new Columna("sucursal", null, null),
                        new Columna("fecha", null, Granularidad.MES),
                        new Columna("total", Agregacion.SUMA, null),
                        new Columna("numero_factura", Agregacion.CONTEO, null)),
                null, null, List.of(new Orden(2, true)));

        assertThat(c.sql()).contains("SUM(v.total) AS c2", "COUNT(CAST(v.numero_factura AS TEXT)) AS c3",
                "TO_CHAR(v.fecha, 'YYYY-MM') AS c1", "GROUP BY 1, 2");
        assertThat(c.orden()).isEqualTo(" ORDER BY 3 DESC NULLS LAST, 1");
        assertThat(c.columnas()).extracting(r -> r.etiqueta())
                .containsExactly("Sucursal", "Fecha (mes)", "Suma de Total", "Conteo de N° factura");
        assertThat(c.columnas().get(3).tipo()).isEqualTo(TipoCampo.NUMERO);
    }

    @Test
    void sinAgregacionNoAgrupaYLasFechasMantienenLaHora() {
        ConsultaSql c = construir(List.of(new Columna("fecha", null, null), new Columna("cliente", null, null)),
                null, null, null);

        assertThat(c.sql()).doesNotContain("GROUP BY").contains("v.fecha AS c0");
        assertThat(c.orden()).isEqualTo(" ORDER BY 1");
    }

    @Test
    void filtrosVanComoParametrosYSeCombinanConOr() {
        ConsultaSql c = construir(List.of(new Columna("total", null, null)),
                List.of(new Filtro("cliente", OperadorFiltro.CONTIENE, "50%_x", null),
                        new Filtro("total", OperadorFiltro.ENTRE, "5000", "1000,5"),
                        new Filtro("fecha", OperadorFiltro.MES_PASADO, null, null)),
                OperadorLogico.OR, null);

        assertThat(c.sql()).contains(
                "WHERE (LOWER(CAST(COALESCE(c.razon_social, v.razon_social_cliente) AS TEXT)) LIKE :p0 ESCAPE '!')"
                        + " OR (v.total BETWEEN :p1 AND :p1b)"
                        + " OR (CAST(v.fecha AS DATE) BETWEEN :p2 AND :p2b)");
        assertThat(c.parametros().getValue("p0")).isEqualTo("%50!%!_x%");
        assertThat(c.parametros().getValue("p1")).isEqualTo(new BigDecimal("1000.5"));
        assertThat(c.parametros().getValue("p1b")).isEqualTo(new BigDecimal("5000"));
        assertThat(c.parametros().getValue("p2")).isEqualTo(Date.valueOf("2026-09-01"));
        assertThat(c.parametros().getValue("p2b")).isEqualTo(Date.valueOf("2026-09-30"));
        assertThat(c.descripcionFiltros()).isEqualTo(
                "Filtros: Cliente contiene 50%_x o Total entre 5000 y 1000,5 o Fecha el mes pasado");
    }

    @Test
    void elTextoDelUsuarioNuncaLlegaAlSql() {
        ConsultaSql c = construir(List.of(new Columna("cliente", null, null)),
                List.of(new Filtro("estado", OperadorFiltro.IGUAL, "x'; DROP TABLE ventas; --", null)),
                null, null);

        assertThat(c.sql()).doesNotContain("DROP");
        assertThat(c.parametros().getValue("p0")).isEqualTo("x'; drop table ventas; --");
    }

    @Test
    void rechazaCamposEntidadesYOperadoresFueraDelCatalogo() {
        assertThatThrownBy(() -> construir(List.of(new Columna("total; DROP TABLE ventas", null, null)), null, null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no existe");
        assertThatThrownBy(() -> constructor.construir(
                new DefinicionQbe("usuario", List.of(new Columna("password", null, null)), null, null, null), HOY))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no está habilitada");
        assertThatThrownBy(() -> construir(List.of(new Columna("cliente", Agregacion.SUMA, null)), null, null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("suma");
        assertThatThrownBy(() -> construir(List.of(new Columna("cliente", null, null)),
                List.of(new Filtro("cliente", OperadorFiltro.MAYOR, "1", null)), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no se puede usar");
        assertThatThrownBy(() -> construir(List.of(new Columna("cliente", null, null)), null, null, List.of(new Orden(5, false))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validaLosValoresSegunElTipo() {
        assertThatThrownBy(() -> construir(List.of(new Columna("total", null, null)),
                List.of(new Filtro("total", OperadorFiltro.MAYOR, "1 OR 1=1", null)), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no es un número");
        assertThatThrownBy(() -> construir(List.of(new Columna("total", null, null)),
                List.of(new Filtro("fecha", OperadorFiltro.DESDE, "ayer", null)), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no es una fecha");
        assertThatThrownBy(() -> construir(List.of(new Columna("total", null, null)),
                List.of(new Filtro("cliente", OperadorFiltro.CONTIENE, " ", null)), null, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Ingresa un valor");
    }

    @Test
    void ultimosDiasYEsteMesUsanLaFechaDeReferencia() {
        ConsultaSql c = construir(List.of(new Columna("total", null, null)),
                List.of(new Filtro("fecha", OperadorFiltro.ULTIMOS_DIAS, "7", null),
                        new Filtro("fecha", OperadorFiltro.ESTE_MES, null, null)), null, null);

        assertThat(c.parametros().getValue("p0")).isEqualTo(Date.valueOf("2026-10-15"));
        assertThat(c.parametros().getValue("p1")).isEqualTo(Date.valueOf("2026-10-01"));
        assertThat(c.parametros().getValue("p1b")).isEqualTo(Date.valueOf(HOY));
    }
}
