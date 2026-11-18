package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service;

import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas puras del plan de cuentas (sin base de datos), para poder probarlas aisladas.
 *
 * <p>El código es jerárquico y separado por puntos: {@code 1}, {@code 1.1}, {@code 1.1.1},
 * {@code 1.1.1.01}. El nivel es la cantidad de segmentos y una subcuenta agrega exactamente un
 * segmento al código de su cuenta padre.
 */
public final class ReglasPlanCuentas {

    public static final Set<String> TIPOS = Set.of("ACTIVO", "PASIVO", "PATRIMONIO", "INGRESO", "EGRESO");
    public static final Set<String> NATURALEZAS = Set.of("DEUDORA", "ACREEDORA");

    /** Cuentas que usa la integración contable automática de ventas y compras (CU28): no se pueden desactivar. */
    public static final Set<String> CUENTAS_INTEGRACION = Set.of(
            ContabilidadVentaService.CAJA,
            ContabilidadVentaService.BANCO,
            ContabilidadVentaService.CUENTAS_POR_COBRAR,
            ContabilidadVentaService.DEBITO_FISCAL_IVA,
            ContabilidadVentaService.IT_POR_PAGAR,
            ContabilidadVentaService.VENTAS,
            ContabilidadVentaService.IT_GASTO,
            ContabilidadVentaService.INVENTARIO,
            ContabilidadVentaService.CREDITO_FISCAL_IVA,
            ContabilidadVentaService.CUENTAS_POR_PAGAR);

    private static final Pattern CODIGO = Pattern.compile("^\\d+(\\.\\d+)*$");

    private ReglasPlanCuentas() {
    }

    public static boolean codigoValido(String codigo) {
        return codigo != null && CODIGO.matcher(codigo).matches();
    }

    public static int nivel(String codigo) {
        return codigo.split("\\.").length;
    }

    /** {@code true} si {@code codigoHijo} cuelga directamente de {@code codigoPadre}. */
    public static boolean esHijoDirecto(String codigoPadre, String codigoHijo) {
        return codigoHijo.startsWith(codigoPadre + ".") && nivel(codigoHijo) == nivel(codigoPadre) + 1;
    }

    /** Activos y egresos son deudores; pasivos, patrimonio e ingresos son acreedores. */
    public static String naturalezaPorDefecto(String tipo) {
        return "ACTIVO".equals(tipo) || "EGRESO".equals(tipo) ? "DEUDORA" : "ACREEDORA";
    }

    public static boolean esCuentaIntegracion(String codigo) {
        return CUENTAS_INTEGRACION.contains(codigo);
    }

    public static String mayusculas(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }
}
