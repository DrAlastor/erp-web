package modulo.catalogo_e_inventario.cu12_consultar_kardex.service;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Recalcula el Kardex valorado de un producto en un almacén a partir de sus movimientos.
 *
 * <p>No depende de la base de datos ni de Spring: recibe los movimientos ya ordenados por
 * fecha e id y devuelve, paso a paso, el costo de cada entrada o salida y el saldo
 * resultante. El saldo valorado que guarda el registro de movimientos es solo
 * {@code existencia × costo del movimiento}, así que el costo real de cada método se
 * reconstruye acá.
 *
 * <p>El stock de un almacén puede existir antes de su primer movimiento (carga inicial sin
 * registro en el Kardex). Por eso la apertura se deduce del primer movimiento: es el saldo
 * que ese registro dejó menos lo que movió, valorado al costo de ese primer movimiento.
 */
public final class KardexCalculador {

    private static final int ESCALA_INTERNA = 6;
    private static final int ESCALA_CANTIDAD = 2;

    private KardexCalculador() {
    }

    /** Un movimiento del Kardex con su costo y el saldo que dejó. */
    public record Paso(
            KardexMovimiento movimiento,
            boolean entrada,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal valor,
            BigDecimal saldoCantidad,
            BigDecimal saldoCostoUnitario,
            BigDecimal saldoValor) {
    }

    /** Resultado completo: el saldo de apertura y cada paso en orden cronológico. */
    public record Calculo(BigDecimal aperturaCantidad, BigDecimal aperturaValor, List<Paso> pasos) {

        public BigDecimal saldoFinalCantidad() {
            return pasos.isEmpty() ? aperturaCantidad : pasos.get(pasos.size() - 1).saldoCantidad();
        }

        public BigDecimal saldoFinalValor() {
            return pasos.isEmpty() ? aperturaValor : pasos.get(pasos.size() - 1).saldoValor();
        }
    }

    /** Indica si el tipo de movimiento aumenta la existencia. */
    public static boolean esEntrada(String tipoMovimiento) {
        if (tipoMovimiento == null) {
            return false;
        }
        String tipo = tipoMovimiento.trim().toUpperCase();
        return tipo.contains("ENTRADA") || tipo.endsWith("POSITIVO") || tipo.equals("AJUSTE");
    }

    public static Calculo calcular(List<KardexMovimiento> movimientos, MetodoCosteo metodo) {
        if (movimientos.isEmpty()) {
            return new Calculo(cero(), cero(), List.of());
        }

        EstadoCosteo estado = metodo == MetodoCosteo.PEPS ? new EstadoPeps() : new EstadoPromedio();

        KardexMovimiento primero = movimientos.get(0);
        BigDecimal movido = esEntrada(primero.getTipoMovimiento())
                ? primero.getCantidad()
                : primero.getCantidad().negate();
        BigDecimal aperturaCantidad = primero.getSaldoCantidad().subtract(movido).max(BigDecimal.ZERO);
        if (aperturaCantidad.signum() > 0) {
            estado.entrar(aperturaCantidad, primero.getCostoUnitario());
        }
        BigDecimal aperturaValor = dinero(estado.valor());

        List<Paso> pasos = new ArrayList<>(movimientos.size());
        for (KardexMovimiento m : movimientos) {
            boolean entrada = esEntrada(m.getTipoMovimiento());
            BigDecimal cantidad = m.getCantidad();
            BigDecimal costoUnitario;
            BigDecimal valor;
            if (entrada) {
                estado.entrar(cantidad, m.getCostoUnitario());
                costoUnitario = m.getCostoUnitario();
                valor = cantidad.multiply(costoUnitario);
            } else {
                valor = estado.salir(cantidad, m.getCostoUnitario());
                costoUnitario = cantidad.signum() == 0
                        ? m.getCostoUnitario()
                        : valor.divide(cantidad, ESCALA_INTERNA, RoundingMode.HALF_UP);
            }
            pasos.add(new Paso(
                    m,
                    entrada,
                    cantidad.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP),
                    costoUnitario.setScale(4, RoundingMode.HALF_UP),
                    dinero(valor),
                    estado.cantidad().setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP),
                    estado.costoUnitario().setScale(4, RoundingMode.HALF_UP),
                    dinero(estado.valor())));
        }
        return new Calculo(aperturaCantidad.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP), aperturaValor, pasos);
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA_CANTIDAD);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Existencia y valor acumulados según el método de costeo. */
    private interface EstadoCosteo {
        void entrar(BigDecimal cantidad, BigDecimal costoUnitario);

        /** Retira la cantidad y devuelve el valor al que salió. */
        BigDecimal salir(BigDecimal cantidad, BigDecimal costoReferencia);

        BigDecimal cantidad();

        BigDecimal valor();

        default BigDecimal costoUnitario() {
            return cantidad().signum() > 0
                    ? valor().divide(cantidad(), ESCALA_INTERNA, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
    }

    private static final class EstadoPromedio implements EstadoCosteo {
        private BigDecimal cantidad = BigDecimal.ZERO;
        private BigDecimal valor = BigDecimal.ZERO;

        @Override
        public void entrar(BigDecimal c, BigDecimal costoUnitario) {
            cantidad = cantidad.add(c);
            valor = valor.add(c.multiply(costoUnitario));
        }

        @Override
        public BigDecimal salir(BigDecimal c, BigDecimal costoReferencia) {
            BigDecimal promedio = costoUnitario().signum() > 0 ? costoUnitario() : costoReferencia;
            BigDecimal retirado = c.min(cantidad);
            BigDecimal exceso = c.subtract(retirado);
            BigDecimal valorSalida = retirado.multiply(promedio).add(exceso.multiply(costoReferencia));
            valor = valor.subtract(retirado.multiply(promedio)).max(BigDecimal.ZERO);
            cantidad = cantidad.subtract(retirado);
            if (cantidad.signum() == 0) {
                valor = BigDecimal.ZERO;
            }
            return valorSalida;
        }

        @Override
        public BigDecimal cantidad() {
            return cantidad;
        }

        @Override
        public BigDecimal valor() {
            return valor;
        }
    }

    private static final class EstadoPeps implements EstadoCosteo {

        /** Lote de entrada con su cantidad restante y su costo unitario. */
        private static final class Capa {
            private BigDecimal restante;
            private final BigDecimal costoUnitario;

            private Capa(BigDecimal restante, BigDecimal costoUnitario) {
                this.restante = restante;
                this.costoUnitario = costoUnitario;
            }
        }

        private final Deque<Capa> capas = new ArrayDeque<>();

        @Override
        public void entrar(BigDecimal c, BigDecimal costoUnitario) {
            capas.addLast(new Capa(c, costoUnitario));
        }

        @Override
        public BigDecimal salir(BigDecimal c, BigDecimal costoReferencia) {
            BigDecimal pendiente = c;
            BigDecimal valorSalida = BigDecimal.ZERO;
            while (pendiente.signum() > 0 && !capas.isEmpty()) {
                Capa capa = capas.peekFirst();
                BigDecimal tomado = pendiente.min(capa.restante);
                valorSalida = valorSalida.add(tomado.multiply(capa.costoUnitario));
                capa.restante = capa.restante.subtract(tomado);
                pendiente = pendiente.subtract(tomado);
                if (capa.restante.signum() == 0) {
                    capas.removeFirst();
                }
            }
            if (pendiente.signum() > 0) {
                valorSalida = valorSalida.add(pendiente.multiply(costoReferencia));
            }
            return valorSalida;
        }

        @Override
        public BigDecimal cantidad() {
            return capas.stream().map(k -> k.restante).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public BigDecimal valor() {
            return capas.stream()
                    .map(k -> k.restante.multiply(k.costoUnitario))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
