package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo;

import java.util.Set;

import static modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo.*;

/**
 * Operadores de comparación de los filtros dinámicos. Cada uno indica a qué tipos de dato
 * se aplica y cuántos valores necesita (0 para "hoy" o "es verdadero", 2 para "entre").
 */
public enum OperadorFiltro {

    CONTIENE("contiene", 1, Set.of(TEXTO)),
    NO_CONTIENE("no contiene", 1, Set.of(TEXTO)),
    EMPIEZA_CON("empieza con", 1, Set.of(TEXTO)),
    IGUAL("es igual a", 1, Set.of(TEXTO, NUMERO, MONEDA, FECHA)),
    DISTINTO("es distinto de", 1, Set.of(TEXTO, NUMERO, MONEDA)),
    MAYOR("mayor que", 1, Set.of(NUMERO, MONEDA)),
    MAYOR_IGUAL("mayor o igual que", 1, Set.of(NUMERO, MONEDA)),
    MENOR("menor que", 1, Set.of(NUMERO, MONEDA)),
    MENOR_IGUAL("menor o igual que", 1, Set.of(NUMERO, MONEDA)),
    ENTRE("entre", 2, Set.of(NUMERO, MONEDA, FECHA)),
    DESDE("desde", 1, Set.of(FECHA)),
    HASTA("hasta", 1, Set.of(FECHA)),
    HOY("hoy", 0, Set.of(FECHA)),
    AYER("ayer", 0, Set.of(FECHA)),
    ESTA_SEMANA("esta semana", 0, Set.of(FECHA)),
    ESTE_MES("este mes", 0, Set.of(FECHA)),
    MES_PASADO("el mes pasado", 0, Set.of(FECHA)),
    ESTE_ANIO("este año", 0, Set.of(FECHA)),
    ULTIMOS_DIAS("en los últimos N días", 1, Set.of(FECHA)),
    ES_VERDADERO("es sí", 0, Set.of(BOOLEANO)),
    ES_FALSO("es no", 0, Set.of(BOOLEANO)),
    VACIO("está vacío", 0, Set.of(TEXTO, NUMERO, MONEDA, FECHA)),
    NO_VACIO("no está vacío", 0, Set.of(TEXTO, NUMERO, MONEDA, FECHA));

    private final String etiqueta;
    private final int valores;
    private final Set<TipoCampo> tipos;

    OperadorFiltro(String etiqueta, int valores, Set<TipoCampo> tipos) {
        this.etiqueta = etiqueta;
        this.valores = valores;
        this.tipos = tipos;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Cantidad de valores que el usuario debe ingresar. */
    public int valores() {
        return valores;
    }

    public boolean admite(TipoCampo tipo) {
        return tipos.contains(tipo);
    }
}
