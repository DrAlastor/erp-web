package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de la validación comercial de un pedido de venta (CU17).
 *
 * <p>Es lógica pura: recibe los datos que el servicio ya leyó y devuelve el resultado de cada
 * regla. Una regla <em>bloqueante</em> impide aprobar el pedido; una que no lo es solo exige
 * que quien aprueba deje una justificación.
 *
 * <ul>
 *   <li>El pedido debe tener líneas y un total mayor a cero.</li>
 *   <li>El cliente debe estar activo.</li>
 *   <li>El cliente no puede tener saldos vencidos (bloqueo preventivo por mora).</li>
 *   <li>En ventas a crédito, la deuda pendiente más este pedido no puede superar el límite.</li>
 *   <li>Cada producto debe tener existencia libre: stock físico menos lo ya reservado por
 *       otros pedidos aprobados.</li>
 *   <li>El descuento no debe pasar del tope sin autorización.</li>
 * </ul>
 */
public final class ReglasValidacionComercial {

    public static final String PEDIDO_CON_DETALLE = "PEDIDO_CON_DETALLE";
    public static final String CLIENTE_ACTIVO = "CLIENTE_ACTIVO";
    public static final String SALDO_VENCIDO = "SALDO_VENCIDO";
    public static final String LIMITE_CREDITO = "LIMITE_CREDITO";
    public static final String STOCK_DISPONIBLE = "STOCK_DISPONIBLE";
    public static final String DESCUENTO_AUTORIZADO = "DESCUENTO_AUTORIZADO";

    /** Descuento máximo, en porcentaje del subtotal, que se aprueba sin justificar. */
    public static final BigDecimal DESCUENTO_MAXIMO_PORCENTAJE = new BigDecimal("10");

    private static final Locale LOCALE_BO = Locale.of("es", "BO");

    private ReglasValidacionComercial() {
    }

    /** Datos comerciales del cliente del pedido. */
    public record Cliente(String razonSocial, String condicionPago, BigDecimal limiteCredito, boolean activo) {
        public boolean esCredito() {
            return condicionPago != null && condicionPago.toUpperCase(Locale.ROOT).contains("CRED");
        }
    }

    /** Cuentas por cobrar del cliente a la fecha de la validación. */
    public record Deuda(BigDecimal saldoPendiente, BigDecimal saldoVencido, int cuotasVencidas) {
    }

    /** Línea del pedido con la existencia del producto en la sucursal. */
    public record Linea(Integer productoId, String producto, BigDecimal cantidad,
                        BigDecimal stockFisico, BigDecimal reservado) {
        public BigDecimal disponible() {
            return stockFisico.subtract(reservado).max(BigDecimal.ZERO);
        }

        public boolean cubierta() {
            return disponible().compareTo(cantidad) >= 0;
        }
    }

    /** Resultado de una regla. */
    public record Regla(String codigo, String nombre, boolean cumple, boolean bloqueante, String detalle) {
        /** Una regla bloqueante incumplida impide la aprobación. */
        public boolean impideAprobar() {
            return !cumple && bloqueante;
        }
    }

    public static List<Regla> evaluar(
            Cliente cliente,
            Deuda deuda,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            List<Linea> lineas
    ) {
        List<Regla> reglas = new ArrayList<>();

        boolean conDetalle = !lineas.isEmpty() && total.signum() > 0;
        reglas.add(new Regla(PEDIDO_CON_DETALLE, "El pedido tiene productos y un total válido", conDetalle, true,
                conDetalle
                        ? lineas.size() + " línea(s) por un total de " + bs(total) + "."
                        : "El pedido no tiene líneas o su total no es mayor a cero."));

        if (cliente == null) {
            reglas.add(new Regla(CLIENTE_ACTIVO, "El cliente está activo", false, true,
                    "El cliente del pedido no existe."));
            return reglas;
        }

        reglas.add(new Regla(CLIENTE_ACTIVO, "El cliente está activo", cliente.activo(), true,
                cliente.activo()
                        ? cliente.razonSocial() + " está habilitado para operar."
                        : cliente.razonSocial() + " está inactivo."));

        boolean sinMora = deuda.saldoVencido().signum() <= 0;
        reglas.add(new Regla(SALDO_VENCIDO, "El cliente no tiene saldos vencidos", sinMora, true,
                sinMora
                        ? "Sin cuentas por cobrar vencidas."
                        : deuda.cuotasVencidas() + " cuota(s) vencida(s) por " + bs(deuda.saldoVencido()) + "."));

        reglas.add(reglaCredito(cliente, deuda, total));
        reglas.add(reglaStock(lineas));
        reglas.add(reglaDescuento(subtotal, descuento));
        return reglas;
    }

    private static Regla reglaCredito(Cliente cliente, Deuda deuda, BigDecimal total) {
        String nombre = "El pedido respeta el límite de crédito";
        if (!cliente.esCredito()) {
            return new Regla(LIMITE_CREDITO, nombre, true, true,
                    "Venta de contado: no consume línea de crédito.");
        }
        BigDecimal limite = cliente.limiteCredito() != null ? cliente.limiteCredito() : BigDecimal.ZERO;
        BigDecimal exposicion = deuda.saldoPendiente().add(total);
        if (limite.signum() <= 0) {
            return new Regla(LIMITE_CREDITO, nombre, false, true,
                    "El cliente compra a crédito pero no tiene un límite de crédito asignado.");
        }
        boolean cumple = exposicion.compareTo(limite) <= 0;
        String detalle = "Deuda pendiente " + bs(deuda.saldoPendiente()) + " + pedido " + bs(total)
                + " = " + bs(exposicion) + " frente a un límite de " + bs(limite) + ".";
        return new Regla(LIMITE_CREDITO, nombre, cumple, true, detalle);
    }

    private static Regla reglaStock(List<Linea> lineas) {
        String nombre = "Hay existencia libre para todos los productos";
        List<String> faltantes = lineas.stream()
                .filter(l -> !l.cubierta())
                .map(l -> l.producto() + " (pide " + cantidad(l.cantidad()) + ", libres "
                        + cantidad(l.disponible()) + ")")
                .toList();
        if (faltantes.isEmpty()) {
            return new Regla(STOCK_DISPONIBLE, nombre, true, true,
                    "Todas las líneas están cubiertas con la existencia de la sucursal.");
        }
        return new Regla(STOCK_DISPONIBLE, nombre, false, true,
                "Sin stock suficiente en: " + String.join("; ", faltantes) + ".");
    }

    private static Regla reglaDescuento(BigDecimal subtotal, BigDecimal descuento) {
        String nombre = "El descuento está dentro de lo autorizado";
        if (subtotal.signum() <= 0 || descuento.signum() <= 0) {
            return new Regla(DESCUENTO_AUTORIZADO, nombre, true, false, "El pedido no lleva descuento.");
        }
        BigDecimal porcentaje = descuento.multiply(BigDecimal.valueOf(100))
                .divide(subtotal, 2, RoundingMode.HALF_UP);
        boolean cumple = porcentaje.compareTo(DESCUENTO_MAXIMO_PORCENTAJE) <= 0;
        return new Regla(DESCUENTO_AUTORIZADO, nombre, cumple, false,
                cumple
                        ? "Descuento de " + porcentaje + "% (tope " + DESCUENTO_MAXIMO_PORCENTAJE + "%)."
                        : "Descuento de " + porcentaje + "% supera el tope de " + DESCUENTO_MAXIMO_PORCENTAJE
                                + "%: se requiere justificación para aprobar.");
    }

    private static String bs(BigDecimal valor) {
        return "Bs " + String.format(LOCALE_BO, "%,.2f", valor);
    }

    private static String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
