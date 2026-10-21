package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service;

import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Agregacion;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe.Campo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe.Entidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Granularidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.OperadorFiltro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Columna;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Filtro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.OperadorLogico;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Orden;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.ResultadoQbeResponse.ColumnaResultado;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Traduce una {@link DefinicionQbe} a SQL de solo lectura (HU38.1 y HU38.2).
 *
 * <p>Previene la inyección SQL por construcción: la tabla, los JOIN y la expresión de cada
 * campo salen de {@link CatalogoEntidadesQbe}; operadores, agregaciones y agrupaciones son
 * enums con su fragmento fijo; el orden se indica por posición de columna; y todo valor que
 * escribe el usuario viaja como parámetro con su tipo ya convertido.
 */
@Component
public class ConstructorConsultaQbe {

    /** SQL listo para ejecutar: {@code sql} sin ORDER BY (sirve para contar) y {@code orden} aparte. */
    public record ConsultaSql(
            Entidad entidad,
            String sql,
            String orden,
            MapSqlParameterSource parametros,
            List<ColumnaResultado> columnas,
            String descripcionFiltros) {

        public String sqlOrdenado() {
            return sql + orden;
        }
    }

    public ConsultaSql construir(DefinicionQbe definicion, LocalDate hoy) {
        Entidad entidad = CatalogoEntidadesQbe.buscar(definicion.entidad())
                .orElseThrow(() -> invalida("La entidad '" + definicion.entidad() + "' no está habilitada para reportes."));
        List<Columna> columnas = definicion.columnas() == null ? List.of() : definicion.columnas();
        if (columnas.isEmpty()) {
            throw invalida("Selecciona al menos un campo para el reporte.");
        }
        boolean agrupado = columnas.stream().anyMatch(c -> c.agregacion() != null);

        List<String> select = new ArrayList<>();
        List<String> grupos = new ArrayList<>();
        List<ColumnaResultado> resultado = new ArrayList<>();
        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            Campo campo = campo(entidad, c.campo());
            String expr = campo.sql();
            TipoCampo tipo = campo.tipo();
            String etiqueta = campo.etiqueta();

            Agregacion agregacion = c.agregacion();
            if (agregacion != null) {
                if (!agregacion.admite(tipo)) {
                    throw invalida("No se puede calcular " + agregacion.etiqueta().toLowerCase(Locale.ROOT)
                            + " sobre el campo '" + campo.etiqueta() + "'.");
                }
                expr = agregacion.funcionSql() + "(" + expr + ")";
                etiqueta = agregacion.etiqueta() + " de " + campo.etiqueta();
                tipo = agregacion.tipoResultado(tipo);
            } else {
                if (tipo == TipoCampo.FECHA) {
                    // Agrupar por fecha y hora exacta no agrupa nada: por defecto se agrupa por día.
                    Granularidad g = c.granularidad() != null ? c.granularidad()
                            : agrupado ? Granularidad.DIA : Granularidad.FECHA_HORA;
                    expr = g.aplicar(expr);
                    tipo = g.tipoResultado();
                    if (g == Granularidad.MES || g == Granularidad.ANIO) {
                        etiqueta += " (" + g.etiqueta().toLowerCase(Locale.ROOT) + ")";
                    }
                }
                if (agrupado) {
                    grupos.add(String.valueOf(i + 1));
                }
            }
            select.add(expr + " AS c" + i);
            resultado.add(new ColumnaResultado("c" + i, etiqueta, tipo));
        }

        MapSqlParameterSource parametros = new MapSqlParameterSource();
        List<String> condiciones = new ArrayList<>();
        List<String> descripciones = new ArrayList<>();
        List<Filtro> filtros = definicion.filtros() == null ? List.of() : definicion.filtros();
        for (int j = 0; j < filtros.size(); j++) {
            Filtro f = filtros.get(j);
            Campo campo = campo(entidad, f.campo());
            OperadorFiltro op = f.operador();
            if (op == null) {
                throw invalida("Elige un operador para el filtro de '" + campo.etiqueta() + "'.");
            }
            if (!op.admite(campo.tipo())) {
                throw invalida("El operador '" + op.etiqueta() + "' no se puede usar con '" + campo.etiqueta() + "'.");
            }
            condiciones.add("(" + condicion(campo, op, f, "p" + j, parametros, hoy) + ")");
            descripciones.add(describir(campo, op, f));
        }
        boolean usarOr = definicion.operadorLogico() == OperadorLogico.OR;

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", select))
                .append(" FROM ").append(entidad.from());
        if (!condiciones.isEmpty()) {
            sql.append(" WHERE ").append(String.join(usarOr ? " OR " : " AND ", condiciones));
        }
        if (!grupos.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", grupos));
        }

        String descripcion = descripciones.isEmpty() ? "Sin filtros"
                : "Filtros: " + String.join(usarOr ? " o " : " y ", descripciones);
        return new ConsultaSql(entidad, sql.toString(), orden(definicion.orden(), columnas.size()),
                parametros, resultado, descripcion);
    }

    private static String orden(List<Orden> orden, int cantidadColumnas) {
        List<String> partes = new ArrayList<>();
        for (Orden o : orden == null ? List.<Orden>of() : orden) {
            if (o.columna() == null || o.columna() < 0 || o.columna() >= cantidadColumnas) {
                throw invalida("El orden hace referencia a una columna que no está en el reporte.");
            }
            partes.add((o.columna() + 1) + (o.descendente() ? " DESC NULLS LAST" : " ASC NULLS LAST"));
        }
        // Siempre hay un orden para que la paginación sea estable.
        partes.add("1");
        return " ORDER BY " + String.join(", ", partes);
    }

    private static String condicion(
            Campo campo, OperadorFiltro op, Filtro f, String p, MapSqlParameterSource params, LocalDate hoy) {
        String s = campo.sql();
        TipoCampo tipo = campo.tipo();
        String texto = "LOWER(CAST(" + s + " AS TEXT))";
        String fecha = "CAST(" + s + " AS DATE)";

        return switch (op) {
            case CONTIENE -> {
                params.addValue(p, "%" + escaparLike(valor(campo, f.valor())) + "%");
                yield texto + " LIKE :" + p + " ESCAPE '!'";
            }
            case NO_CONTIENE -> {
                params.addValue(p, "%" + escaparLike(valor(campo, f.valor())) + "%");
                yield s + " IS NULL OR " + texto + " NOT LIKE :" + p + " ESCAPE '!'";
            }
            case EMPIEZA_CON -> {
                params.addValue(p, escaparLike(valor(campo, f.valor())) + "%");
                yield texto + " LIKE :" + p + " ESCAPE '!'";
            }
            case IGUAL -> switch (tipo) {
                case TEXTO -> {
                    params.addValue(p, valor(campo, f.valor()).toLowerCase(Locale.ROOT));
                    yield texto + " = :" + p;
                }
                case FECHA -> {
                    params.addValue(p, Date.valueOf(fecha(campo, f.valor())));
                    yield fecha + " = :" + p;
                }
                default -> {
                    params.addValue(p, numero(campo, f.valor()));
                    yield s + " = :" + p;
                }
            };
            case DISTINTO -> {
                if (tipo == TipoCampo.TEXTO) {
                    params.addValue(p, valor(campo, f.valor()).toLowerCase(Locale.ROOT));
                    yield texto + " IS DISTINCT FROM :" + p;
                }
                params.addValue(p, numero(campo, f.valor()));
                yield s + " IS DISTINCT FROM :" + p;
            }
            case MAYOR -> comparar(s, ">", p, numero(campo, f.valor()), params);
            case MAYOR_IGUAL -> comparar(s, ">=", p, numero(campo, f.valor()), params);
            case MENOR -> comparar(s, "<", p, numero(campo, f.valor()), params);
            case MENOR_IGUAL -> comparar(s, "<=", p, numero(campo, f.valor()), params);
            case ENTRE -> {
                if (tipo == TipoCampo.FECHA) {
                    LocalDate a = fecha(campo, f.valor());
                    LocalDate b = fecha(campo, f.valor2());
                    yield rango(fecha, p, a.isAfter(b) ? b : a, a.isAfter(b) ? a : b, params);
                }
                BigDecimal a = numero(campo, f.valor());
                BigDecimal b = numero(campo, f.valor2());
                params.addValue(p, a.min(b)).addValue(p + "b", a.max(b));
                yield s + " BETWEEN :" + p + " AND :" + p + "b";
            }
            case DESDE -> comparar(fecha, ">=", p, Date.valueOf(fecha(campo, f.valor())), params);
            case HASTA -> comparar(fecha, "<=", p, Date.valueOf(fecha(campo, f.valor())), params);
            case HOY -> rango(fecha, p, hoy, hoy, params);
            case AYER -> rango(fecha, p, hoy.minusDays(1), hoy.minusDays(1), params);
            case ESTA_SEMANA -> rango(fecha, p, hoy.with(DayOfWeek.MONDAY), hoy, params);
            case ESTE_MES -> rango(fecha, p, hoy.withDayOfMonth(1), hoy, params);
            case MES_PASADO -> {
                LocalDate inicio = hoy.minusMonths(1).withDayOfMonth(1);
                yield rango(fecha, p, inicio, inicio.withDayOfMonth(inicio.lengthOfMonth()), params);
            }
            case ESTE_ANIO -> rango(fecha, p, hoy.withDayOfYear(1), hoy, params);
            case ULTIMOS_DIAS -> {
                BigDecimal n = numero(campo, f.valor());
                if (n.signum() <= 0 || n.compareTo(BigDecimal.valueOf(3650)) > 0) {
                    throw invalida("La cantidad de días del filtro de '" + campo.etiqueta() + "' debe estar entre 1 y 3650.");
                }
                yield rango(fecha, p, hoy.minusDays(n.longValue() - 1), hoy, params);
            }
            case ES_VERDADERO -> s + " = TRUE";
            case ES_FALSO -> s + " = FALSE";
            case VACIO -> tipo == TipoCampo.TEXTO
                    ? s + " IS NULL OR TRIM(CAST(" + s + " AS TEXT)) = ''"
                    : s + " IS NULL";
            case NO_VACIO -> tipo == TipoCampo.TEXTO
                    ? s + " IS NOT NULL AND TRIM(CAST(" + s + " AS TEXT)) <> ''"
                    : s + " IS NOT NULL";
        };
    }

    private static String comparar(String expr, String operador, String p, Object valor, MapSqlParameterSource params) {
        params.addValue(p, valor);
        return expr + " " + operador + " :" + p;
    }

    private static String rango(String expr, String p, LocalDate desde, LocalDate hasta, MapSqlParameterSource params) {
        params.addValue(p, Date.valueOf(desde)).addValue(p + "b", Date.valueOf(hasta));
        return expr + " BETWEEN :" + p + " AND :" + p + "b";
    }

    private static String describir(Campo campo, OperadorFiltro op, Filtro f) {
        String texto = campo.etiqueta() + " " + op.etiqueta();
        if (op == OperadorFiltro.ULTIMOS_DIAS) {
            return campo.etiqueta() + " en los últimos " + f.valor() + " días";
        }
        if (op.valores() >= 1) texto += " " + f.valor();
        if (op.valores() == 2) texto += " y " + f.valor2();
        return texto;
    }

    private static Campo campo(Entidad entidad, String clave) {
        return entidad.campo(clave).orElseThrow(() -> invalida(
                "El campo '" + clave + "' no existe en " + entidad.etiqueta() + "."));
    }

    private static String valor(Campo campo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw invalida("Ingresa un valor para el filtro de '" + campo.etiqueta() + "'.");
        }
        return valor.trim();
    }

    /** Acepta "1500.50" y "1500,50". */
    static BigDecimal numero(Campo campo, String valor) {
        String limpio = valor(campo, valor).replace(" ", "");
        if (limpio.contains(",") && !limpio.contains(".")) {
            limpio = limpio.replace(',', '.');
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw invalida("El valor '" + valor + "' del filtro de '" + campo.etiqueta() + "' no es un número válido.");
        }
    }

    private static LocalDate fecha(Campo campo, String valor) {
        String limpio = valor(campo, valor);
        try {
            return LocalDate.parse(limpio.length() > 10 ? limpio.substring(0, 10) : limpio);
        } catch (DateTimeParseException e) {
            throw invalida("El valor '" + valor + "' del filtro de '" + campo.etiqueta()
                    + "' no es una fecha válida (AAAA-MM-DD).");
        }
    }

    private static String escaparLike(String texto) {
        return texto.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
