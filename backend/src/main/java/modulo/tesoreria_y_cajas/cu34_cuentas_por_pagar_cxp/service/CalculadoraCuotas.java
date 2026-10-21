package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reparte el total de una compra en cuotas, sin dependencias de base de datos.
 *
 * <p>Cada cuota vale {@code total / n} truncado a 2 decimales y la ultima absorbe la
 * diferencia de redondeo, de modo que la suma de las cuotas es exactamente el total. La
 * cuota {@code i} (desde 0) vence en {@code primerVencimiento + i * diasEntreCuotas}.
 */
public final class CalculadoraCuotas {

    private CalculadoraCuotas() {}

    public record Cuota(int numero, BigDecimal monto, LocalDate fechaVencimiento) {}

    public static List<Cuota> calcular(
            BigDecimal total, int numeroCuotas, LocalDate primerVencimiento, int diasEntreCuotas) {
        if (numeroCuotas < 1) throw rechazo("Debe haber al menos 1 cuota");
        if (diasEntreCuotas < 1) throw rechazo("Los días entre cuotas deben ser al menos 1");
        BigDecimal importe = total == null ? BigDecimal.ZERO : total.setScale(2, RoundingMode.HALF_UP);
        if (importe.signum() <= 0) throw rechazo("La compra no tiene un total positivo para repartir");

        BigDecimal base = importe.divide(BigDecimal.valueOf(numeroCuotas), 2, RoundingMode.DOWN);
        if (base.signum() <= 0) {
            throw rechazo("El total de la compra no alcanza para " + numeroCuotas + " cuotas");
        }
        List<Cuota> cuotas = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < numeroCuotas; i++) {
            boolean ultima = i == numeroCuotas - 1;
            BigDecimal monto = ultima ? importe.subtract(acumulado) : base;
            acumulado = acumulado.add(monto);
            cuotas.add(new Cuota(i + 1, monto, primerVencimiento.plusDays((long) i * diasEntreCuotas)));
        }
        return cuotas;
    }

    /**
     * Primer vencimiento por defecto: fecha de la compra + dias de credito del proveedor, o
     * + 30 dias si el proveedor no tiene dias de credito cargados.
     */
    public static LocalDate primerVencimientoPorDefecto(LocalDate fechaCompra, Integer diasCredito) {
        return fechaCompra.plusDays(diasCredito == null ? 30 : Math.max(diasCredito, 0));
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
