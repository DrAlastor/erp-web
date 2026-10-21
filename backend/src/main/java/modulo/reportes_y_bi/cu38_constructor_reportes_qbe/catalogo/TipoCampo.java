package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo;

/** Tipo de dato de un campo; decide los operadores de filtro y las agregaciones posibles. */
public enum TipoCampo {
    TEXTO,
    NUMERO,
    MONEDA,
    FECHA,
    BOOLEANO;

    public boolean numerico() {
        return this == NUMERO || this == MONEDA;
    }
}
