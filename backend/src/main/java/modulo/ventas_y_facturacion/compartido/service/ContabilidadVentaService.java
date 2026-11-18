package modulo.ventas_y_facturacion.compartido.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.AsientoContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Integración contable automática de ventas, anulaciones, notas de crédito y compras a proveedores: arma el
 * comprobante de partida doble en asientos_contables y asiento_detalles.
 *
 * <p>Las cuentas se buscan por código en el plan de cuentas. Las que el plan inicial de
 * ERP_Asus no trae (cuentas por cobrar, débito fiscal, IT) se crean la primera vez que se
 * necesitan, colgadas de su grupo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.MANDATORY)
public class ContabilidadVentaService {

    public static final String CAJA = "1.1.1.01";
    public static final String BANCO = "1.1.1.02";
    public static final String CUENTAS_POR_COBRAR = "1.1.2.01";
    public static final String DEBITO_FISCAL_IVA = "2.1.2.01";
    public static final String IT_POR_PAGAR = "2.1.2.02";
    public static final String VENTAS = "4.1.1.01";
    public static final String IT_GASTO = "5.2.1.01";
    // Compras a proveedores (CU28): inventario y cuentas por pagar vienen en el plan inicial de ERP_Asus.
    public static final String INVENTARIO = "1.1.3.01";
    public static final String CREDITO_FISCAL_IVA = "1.1.4.01";
    public static final String CUENTAS_POR_PAGAR = "2.1.1.01";

    /**
     * Cuentas que se crean si faltan: nombre, tipo y naturaleza. Se cuelgan del grupo existente
     * más cercano en el plan de cuentas (ej. 1.1.2.01 de 1.1.2 o, si no existe, de 1.1).
     */
    private static final Map<String, DefinicionCuenta> CUENTAS_AUTOMATICAS = Map.of(
            CAJA, new DefinicionCuenta("Caja Moneda Nacional", "ACTIVO", "DEUDORA"),
            BANCO, new DefinicionCuenta("Bancos Cuenta Corriente", "ACTIVO", "DEUDORA"),
            CUENTAS_POR_COBRAR, new DefinicionCuenta("Cuentas por Cobrar Clientes", "ACTIVO", "DEUDORA"),
            DEBITO_FISCAL_IVA, new DefinicionCuenta("Debito Fiscal IVA", "PASIVO", "ACREEDORA"),
            IT_POR_PAGAR, new DefinicionCuenta("IT por Pagar", "PASIVO", "ACREEDORA"),
            VENTAS, new DefinicionCuenta("Ventas de Productos", "INGRESO", "ACREEDORA"),
            IT_GASTO, new DefinicionCuenta("Impuesto a las Transacciones", "EGRESO", "DEUDORA"),
            INVENTARIO, new DefinicionCuenta("Inventario de Mercaderia", "ACTIVO", "DEUDORA"),
            CREDITO_FISCAL_IVA, new DefinicionCuenta("Credito Fiscal IVA", "ACTIVO", "DEUDORA"),
            CUENTAS_POR_PAGAR, new DefinicionCuenta("Cuentas por Pagar Proveedores", "PASIVO", "ACREEDORA"));

    private final AsientoContableRepository asientoRepository;
    private final CuentaContableRepository cuentaRepository;
    private final PeriodoContableRepository periodoRepository;

    /** Una línea del comprobante: cuenta, glosa y el importe en el debe o en el haber. */
    public record Partida(String codigoCuenta, String glosa, BigDecimal debe, BigDecimal haber) {

        public static Partida debe(String cuenta, String glosa, BigDecimal monto) {
            return new Partida(cuenta, glosa, Montos.redondear(monto), BigDecimal.ZERO);
        }

        public static Partida haber(String cuenta, String glosa, BigDecimal monto) {
            return new Partida(cuenta, glosa, BigDecimal.ZERO, Montos.redondear(monto));
        }

        /** La misma partida con debe y haber intercambiados, para revertir un asiento. */
        public Partida invertida() {
            return new Partida(codigoCuenta, glosa, haber, debe);
        }
    }

    private record DefinicionCuenta(String nombre, String tipo, String naturaleza) {
    }

    /**
     * Registra un asiento balanceado en el periodo contable de la fecha.
     *
     * @return el número de asiento generado
     */
    public String registrarAsiento(LocalDate fecha, String tipo, String glosa, Long usuarioId, List<Partida> partidas) {
        List<Partida> lineas = partidas.stream()
                .filter(p -> Montos.positivo(p.debe()) || Montos.positivo(p.haber()))
                .toList();
        BigDecimal totalDebe = lineas.stream().map(Partida::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHaber = lineas.stream().map(Partida::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineas.isEmpty() || totalDebe.compareTo(totalHaber) != 0) {
            throw new IllegalStateException(
                    "Asiento descuadrado: debe " + totalDebe + " / haber " + totalHaber);
        }

        PeriodoContable periodo = periodoAbierto(fecha);
        AsientoContable asiento = AsientoContable.builder()
                .periodoContableId(periodo.getId())
                .creadoPor(usuarioId)
                .numeroAsiento(siguienteNumero(fecha.getYear()))
                .fecha(fecha)
                .tipoAsiento(tipo)
                .glosa(glosa)
                .totalDebe(totalDebe)
                .totalHaber(totalHaber)
                .estado("MAYORIZADO")
                .build();
        for (Partida partida : lineas) {
            asiento.agregarDetalle(AsientoDetalle.builder()
                    .cuentaContable(cuenta(partida.codigoCuenta()))
                    .glosa(partida.glosa() != null && partida.glosa().length() > 255
                            ? partida.glosa().substring(0, 255) : partida.glosa())
                    .debe(partida.debe())
                    .haber(partida.haber())
                    .build());
        }
        return asientoRepository.save(asiento).getNumeroAsiento();
    }

    /** Periodo de la fecha (lo crea ABIERTO si no existe); 409 si está cerrado. */
    public PeriodoContable periodoAbierto(LocalDate fecha) {
        PeriodoContable periodo = periodoRepository.findByGestionAndMes(fecha.getYear(), fecha.getMonthValue())
                .orElseGet(() -> periodoRepository.save(PeriodoContable.builder()
                        .gestion(fecha.getYear())
                        .mes(fecha.getMonthValue())
                        .nombre(nombreMes(fecha) + " " + fecha.getYear())
                        .fechaInicio(fecha.withDayOfMonth(1))
                        .fechaFin(fecha.withDayOfMonth(fecha.lengthOfMonth()))
                        .estado(PeriodoContable.ESTADO_ABIERTO)
                        .build()));
        if (!periodo.estaAbierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El periodo contable " + periodo.getNombre() + " está cerrado; no se pueden registrar asientos.");
        }
        return periodo;
    }

    /** Siguiente número correlativo AS-AAAA-NNNN de la gestión. */
    public String siguienteNumero(int gestion) {
        String prefijo = "AS-" + gestion + "-";
        String ultimo = asientoRepository.ultimoNumero(prefijo + "%");
        int siguiente = 1;
        if (ultimo != null) {
            try {
                siguiente = Integer.parseInt(ultimo.substring(prefijo.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("Número de asiento con formato inesperado: {}", ultimo);
            }
        }
        return prefijo + String.format("%04d", siguiente);
    }

    private CuentaContable cuenta(String codigo) {
        return cuentaRepository.findByCodigo(codigo).orElseGet(() -> crearCuenta(codigo));
    }

    private CuentaContable crearCuenta(String codigo) {
        DefinicionCuenta definicion = CUENTAS_AUTOMATICAS.get(codigo);
        if (definicion == null) {
            throw new IllegalStateException("La cuenta contable " + codigo + " no existe en el plan de cuentas");
        }
        Long padreId = padreMasCercano(codigo);
        log.info("Creando la cuenta contable {} - {} para la integración de ventas", codigo, definicion.nombre());
        return cuentaRepository.save(CuentaContable.builder()
                .codigo(codigo)
                .nombre(definicion.nombre())
                .nivel(codigo.split("\\.").length)
                .tipo(definicion.tipo())
                .naturaleza(definicion.naturaleza())
                .permiteMovimiento(true)
                .padreId(padreId)
                .activo(true)
                .build());
    }

    /** Id de la cuenta existente cuyo código es el prefijo más largo de {@code codigo}, o null. */
    private Long padreMasCercano(String codigo) {
        String actual = codigo;
        int punto;
        while ((punto = actual.lastIndexOf('.')) > 0) {
            actual = actual.substring(0, punto);
            var padre = cuentaRepository.findByCodigo(actual);
            if (padre.isPresent()) {
                return padre.get().getId();
            }
        }
        return null;
    }

    private static String nombreMes(LocalDate fecha) {
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
    }
}
