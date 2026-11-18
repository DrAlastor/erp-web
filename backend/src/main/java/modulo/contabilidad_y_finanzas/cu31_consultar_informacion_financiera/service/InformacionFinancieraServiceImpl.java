package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.service;

import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.*;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.InformacionFinancieraRepository;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.SaldoCuentaProjection;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InformacionFinancieraServiceImpl implements InformacionFinancieraService {

    private final InformacionFinancieraRepository informacionFinancieraRepository;
    private final CuentaContableRepository cuentaContableRepository;
    private final PeriodoContableRepository periodoContableRepository;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<Integer> listarGestionesDisponibles() {
        List<PeriodoContable> periodos = periodoContableRepository.findAll();
        List<Integer> gestiones = periodos.stream()
                .map(PeriodoContable::getGestion)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (gestiones.isEmpty()) {
            return List.of(LocalDate.now().getYear());
        }
        return gestiones;
    }

    @Override
    public BalanceGeneralResponse generarBalanceGeneral(
            Integer gestion,
            Integer mesCorte,
            LocalDate fechaCorte,
            Integer nivelDetalle
    ) {
        LocalDate fechaEfectivaCorte = resolverFechaCorte(gestion, mesCorte, fechaCorte);
        int maxNivel = (nivelDetalle != null && nivelDetalle >= 1 && nivelDetalle <= 4) ? nivelDetalle : 4;
        String periodoNombre = "Al " + fechaEfectivaCorte.format(FORMATO_FECHA);

        List<CuentaContable> todasLasCuentas = cuentaContableRepository.findByActivoTrueOrderByCodigoAsc();
        List<SaldoCuentaProjection> saldosProj = informacionFinancieraRepository.obtenerSaldosAcumuladosAFecha(fechaEfectivaCorte);

        Map<Long, SaldoCuentaProjection> projMap = new HashMap<>();
        for (SaldoCuentaProjection p : saldosProj) {
            projMap.put(p.getCuentaId(), p);
        }

        // 1. Calcular saldos netos directos por cuenta
        Map<Long, BigDecimal> saldosDirectos = new HashMap<>();
        for (CuentaContable c : todasLasCuentas) {
            SaldoCuentaProjection p = projMap.get(c.getId());
            BigDecimal debe = (p != null && p.getTotalDebe() != null) ? p.getTotalDebe() : BigDecimal.ZERO;
            BigDecimal haber = (p != null && p.getTotalHaber() != null) ? p.getTotalHaber() : BigDecimal.ZERO;

            boolean esDeudora = "DEUDORA".equalsIgnoreCase(c.getNaturaleza());
            BigDecimal saldo = esDeudora ? debe.subtract(haber) : haber.subtract(debe);
            saldosDirectos.put(c.getId(), saldo);
        }

        // 2. Construir mapa de jerarquía (padre -> hijos)
        Map<Long, List<CuentaContable>> hijosPorPadre = new HashMap<>();
        List<CuentaContable> raicesActivo = new ArrayList<>();
        List<CuentaContable> raicesPasivo = new ArrayList<>();
        List<CuentaContable> raicesPatrimonio = new ArrayList<>();

        for (CuentaContable c : todasLasCuentas) {
            if (c.getPadreId() == null) {
                if ("ACTIVO".equalsIgnoreCase(c.getTipo())) raicesActivo.add(c);
                else if ("PASIVO".equalsIgnoreCase(c.getTipo())) raicesPasivo.add(c);
                else if ("PATRIMONIO".equalsIgnoreCase(c.getTipo())) raicesPatrimonio.add(c);
            } else {
                hijosPorPadre.computeIfAbsent(c.getPadreId(), k -> new ArrayList<>()).add(c);
            }
        }

        // 3. Calcular saldos acumulados totales (haciendo rollup de abajo hacia arriba)
        Map<Long, BigDecimal> saldosTotales = new HashMap<>();
        for (CuentaContable c : todasLasCuentas) {
            calcularSaldoRollup(c, hijosPorPadre, saldosDirectos, saldosTotales);
        }

        // 4. Calcular Utilidad del Ejercicio a la fecha de corte (Ingresos - Egresos)
        BigDecimal totalIngresosPeriodo = BigDecimal.ZERO;
        BigDecimal totalEgresosPeriodo = BigDecimal.ZERO;
        for (CuentaContable c : todasLasCuentas) {
            if (Boolean.TRUE.equals(c.getPermiteMovimiento())) {
                BigDecimal saldo = saldosDirectos.getOrDefault(c.getId(), BigDecimal.ZERO);
                if ("INGRESO".equalsIgnoreCase(c.getTipo())) {
                    totalIngresosPeriodo = totalIngresosPeriodo.add(saldo);
                } else if ("EGRESO".equalsIgnoreCase(c.getTipo())) {
                    totalEgresosPeriodo = totalEgresosPeriodo.add(saldo);
                }
            }
        }
        BigDecimal utilidadEjercicio = totalIngresosPeriodo.subtract(totalEgresosPeriodo);

        // 5. Construir árboles por sección respetando maxNivel
        List<NodoFinancieroDTO> nodosActivo = construirArbolNodos(raicesActivo, hijosPorPadre, saldosTotales, maxNivel, BigDecimal.ZERO);
        List<NodoFinancieroDTO> nodosPasivo = construirArbolNodos(raicesPasivo, hijosPorPadre, saldosTotales, maxNivel, BigDecimal.ZERO);
        List<NodoFinancieroDTO> nodosPatrimonio = construirArbolNodos(raicesPatrimonio, hijosPorPadre, saldosTotales, maxNivel, BigDecimal.ZERO);

        BigDecimal totalActivos = sumarNodosRaiz(nodosActivo);
        BigDecimal totalPasivos = sumarNodosRaiz(nodosPasivo);
        BigDecimal totalPatrimonioCuentas = sumarNodosRaiz(nodosPatrimonio);
        BigDecimal totalPatrimonioNeto = totalPatrimonioCuentas.add(utilidadEjercicio);
        BigDecimal totalPasivoMasPatrimonio = totalPasivos.add(totalPatrimonioNeto);
        BigDecimal diferencia = totalActivos.subtract(totalPasivoMasPatrimonio);
        boolean estaCuadrado = diferencia.compareTo(BigDecimal.ZERO) == 0;

        CuadreBalanceDTO cuadre = new CuadreBalanceDTO(
                totalActivos,
                totalPasivos,
                totalPatrimonioCuentas,
                utilidadEjercicio,
                totalPatrimonioNeto,
                totalPasivoMasPatrimonio,
                diferencia,
                estaCuadrado
        );

        return new BalanceGeneralResponse(
                gestion != null ? gestion : fechaEfectivaCorte.getYear(),
                mesCorte != null ? mesCorte : fechaEfectivaCorte.getMonthValue(),
                fechaEfectivaCorte,
                periodoNombre,
                maxNivel,
                nodosActivo,
                nodosPasivo,
                nodosPatrimonio,
                cuadre
        );
    }

    @Override
    public EstadoResultadosResponse generarEstadoResultados(
            Integer gestion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer nivelDetalle
    ) {
        LocalDate[] rango = resolverRangoFechas(gestion, fechaInicio, fechaFin);
        LocalDate fInicio = rango[0];
        LocalDate fFin = rango[1];

        if (fInicio.isAfter(fFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }

        int maxNivel = (nivelDetalle != null && nivelDetalle >= 1 && nivelDetalle <= 4) ? nivelDetalle : 4;
        String periodoNombre = "Del " + fInicio.format(FORMATO_FECHA) + " al " + fFin.format(FORMATO_FECHA);

        List<CuentaContable> todasLasCuentas = cuentaContableRepository.findByActivoTrueOrderByCodigoAsc();
        List<SaldoCuentaProjection> movimientos = informacionFinancieraRepository.obtenerMovimientosPeriodo(fInicio, fFin);

        Map<Long, SaldoCuentaProjection> movMap = new HashMap<>();
        for (SaldoCuentaProjection p : movimientos) {
            movMap.put(p.getCuentaId(), p);
        }

        Map<Long, BigDecimal> saldosDirectos = new HashMap<>();
        for (CuentaContable c : todasLasCuentas) {
            SaldoCuentaProjection p = movMap.get(c.getId());
            BigDecimal debe = (p != null && p.getTotalDebe() != null) ? p.getTotalDebe() : BigDecimal.ZERO;
            BigDecimal haber = (p != null && p.getTotalHaber() != null) ? p.getTotalHaber() : BigDecimal.ZERO;

            boolean esDeudora = "DEUDORA".equalsIgnoreCase(c.getNaturaleza());
            BigDecimal saldo = esDeudora ? debe.subtract(haber) : haber.subtract(debe);
            saldosDirectos.put(c.getId(), saldo);
        }

        Map<Long, List<CuentaContable>> hijosPorPadre = new HashMap<>();
        List<CuentaContable> raicesIngreso = new ArrayList<>();
        List<CuentaContable> raicesEgreso = new ArrayList<>();

        for (CuentaContable c : todasLasCuentas) {
            if (c.getPadreId() == null) {
                if ("INGRESO".equalsIgnoreCase(c.getTipo())) raicesIngreso.add(c);
                else if ("EGRESO".equalsIgnoreCase(c.getTipo())) raicesEgreso.add(c);
            } else {
                hijosPorPadre.computeIfAbsent(c.getPadreId(), k -> new ArrayList<>()).add(c);
            }
        }

        Map<Long, BigDecimal> saldosTotales = new HashMap<>();
        for (CuentaContable c : todasLasCuentas) {
            calcularSaldoRollup(c, hijosPorPadre, saldosDirectos, saldosTotales);
        }

        // Clasificación de Ingresos y Egresos
        BigDecimal totalIngresosOperativos = BigDecimal.ZERO;
        BigDecimal totalCostoVentas = BigDecimal.ZERO;
        BigDecimal totalGastosOperativos = BigDecimal.ZERO;
        BigDecimal otrosIngresos = BigDecimal.ZERO;
        BigDecimal otrosGastos = BigDecimal.ZERO;

        for (CuentaContable c : todasLasCuentas) {
            if (Boolean.TRUE.equals(c.getPermiteMovimiento())) {
                BigDecimal s = saldosDirectos.getOrDefault(c.getId(), BigDecimal.ZERO);
                if ("INGRESO".equalsIgnoreCase(c.getTipo())) {
                    if (c.getCodigo().startsWith("4.1") || c.getCodigo().startsWith("4")) {
                        totalIngresosOperativos = totalIngresosOperativos.add(s);
                    } else {
                        otrosIngresos = otrosIngresos.add(s);
                    }
                } else if ("EGRESO".equalsIgnoreCase(c.getTipo())) {
                    if (c.getCodigo().startsWith("5.1") || c.getNombre().toLowerCase().contains("costo")) {
                        totalCostoVentas = totalCostoVentas.add(s);
                    } else if (c.getCodigo().startsWith("5.2") || c.getNombre().toLowerCase().contains("gasto") || c.getNombre().toLowerCase().contains("operativ")) {
                        totalGastosOperativos = totalGastosOperativos.add(s);
                    } else {
                        otrosGastos = otrosGastos.add(s);
                    }
                }
            }
        }

        BigDecimal utilidadBruta = totalIngresosOperativos.subtract(totalCostoVentas);
        BigDecimal resultadoOperativo = utilidadBruta.subtract(totalGastosOperativos);
        BigDecimal utilidadNeta = resultadoOperativo.add(otrosIngresos).subtract(otrosGastos);

        BigDecimal baseIngresos = totalIngresosOperativos.compareTo(BigDecimal.ZERO) > 0 ? totalIngresosOperativos : BigDecimal.ZERO;

        BigDecimal margenBrutoPorc = baseIngresos.compareTo(BigDecimal.ZERO) > 0
                ? utilidadBruta.multiply(BigDecimal.valueOf(100)).divide(baseIngresos, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal margenNetoPorc = baseIngresos.compareTo(BigDecimal.ZERO) > 0
                ? utilidadNeta.multiply(BigDecimal.valueOf(100)).divide(baseIngresos, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<RubroResultadoDTO> resumen = List.of(
                new RubroResultadoDTO("INGRESOS", "Ingresos Operativos Netos", totalIngresosOperativos, calcularPorcentaje(totalIngresosOperativos, baseIngresos)),
                new RubroResultadoDTO("COSTO_VENTAS", "(-) Costo de Mercadería Vendida", totalCostoVentas, calcularPorcentaje(totalCostoVentas, baseIngresos)),
                new RubroResultadoDTO("UTILIDAD_BRUTA", "(=) Utilidad Bruta en Ventas", utilidadBruta, margenBrutoPorc),
                new RubroResultadoDTO("GASTOS_OPERATIVOS", "(-) Gastos Operativos de Administración y Ventas", totalGastosOperativos, calcularPorcentaje(totalGastosOperativos, baseIngresos)),
                new RubroResultadoDTO("RESULTADO_OPERATIVO", "(=) Resultado de Operación", resultadoOperativo, calcularPorcentaje(resultadoOperativo, baseIngresos)),
                new RubroResultadoDTO("OTROS_INGRESOS", "(+) Otros Ingresos No Operativos", otrosIngresos, calcularPorcentaje(otrosIngresos, baseIngresos)),
                new RubroResultadoDTO("OTROS_GASTOS", "(-) Otros Gastos No Operativos", otrosGastos, calcularPorcentaje(otrosGastos, baseIngresos)),
                new RubroResultadoDTO("UTILIDAD_NETA", "(=) Utilidad / Pérdida Neta del Ejercicio", utilidadNeta, margenNetoPorc)
        );

        List<NodoFinancieroDTO> detalleIngresos = construirArbolNodos(raicesIngreso, hijosPorPadre, saldosTotales, maxNivel, baseIngresos);
        List<NodoFinancieroDTO> detalleEgresos = construirArbolNodos(raicesEgreso, hijosPorPadre, saldosTotales, maxNivel, baseIngresos);

        return new EstadoResultadosResponse(
                gestion != null ? gestion : fInicio.getYear(),
                fInicio,
                fFin,
                periodoNombre,
                maxNivel,
                totalIngresosOperativos,
                totalCostoVentas,
                utilidadBruta,
                totalGastosOperativos,
                resultadoOperativo,
                otrosIngresos,
                otrosGastos,
                utilidadNeta,
                margenBrutoPorc,
                margenNetoPorc,
                resumen,
                detalleIngresos,
                detalleEgresos
        );
    }

    @Override
    public byte[] exportarBalanceCsv(
            Integer gestion,
            Integer mesCorte,
            LocalDate fechaCorte,
            Integer nivelDetalle
    ) {
        BalanceGeneralResponse data = generarBalanceGeneral(gestion, mesCorte, fechaCorte, nivelDetalle);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("BALANCE GENERAL - ESTADO DE SITUACIÓN PATRIMONIAL\n");
        sb.append("Empresa:;Nexo ERP S.R.L.;Corte:;").append(data.periodoNombre()).append("\n");
        sb.append("Total Activos:;").append(data.cuadre().totalActivos()).append(";Total Pasivo + Patrimonio:;")
                .append(data.cuadre().totalPasivoMasPatrimonio()).append(";Cuadrado:;").append(data.cuadre().estaCuadrado() ? "SI" : "NO").append("\n\n");

        sb.append("Tipo;Codigo;Nombre Cuenta;Nivel;Permite Movimiento;Saldo (BOB)\n");

        sb.append("--- ACTIVO ---\n");
        exportarNodosCsv(data.activos(), sb, "");

        sb.append("\n--- PASIVO ---\n");
        exportarNodosCsv(data.pasivos(), sb, "");

        sb.append("\n--- PATRIMONIO ---\n");
        exportarNodosCsv(data.patrimonio(), sb, "");
        sb.append("PATRIMONIO;RESULTADO;Utilidad del Ejercicio;;;").append(data.cuadre().utilidadEjercicio()).append("\n");
        sb.append("PATRIMONIO;TOTAL;Total Patrimonio Neto;;;").append(data.cuadre().totalPatrimonioNeto()).append("\n\n");

        sb.append("RESUMEN DE CUADRE\n");
        sb.append("Total Activos:;").append(data.cuadre().totalActivos()).append("\n");
        sb.append("Total Pasivos:;").append(data.cuadre().totalPasivos()).append("\n");
        sb.append("Total Patrimonio Neto:;").append(data.cuadre().totalPatrimonioNeto()).append("\n");
        sb.append("Total Pasivo + Patrimonio:;").append(data.cuadre().totalPasivoMasPatrimonio()).append("\n");
        sb.append("Diferencia:;").append(data.cuadre().diferencia()).append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportarEstadoResultadosCsv(
            Integer gestion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer nivelDetalle
    ) {
        EstadoResultadosResponse data = generarEstadoResultados(gestion, fechaInicio, fechaFin, nivelDetalle);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("ESTADO DE RESULTADOS - PÉRDIDAS Y GANANCIAS\n");
        sb.append("Periodo:;").append(data.periodoNombre()).append("\n\n");

        sb.append("RESUMEN ESCALONADO\n");
        sb.append("Rubro;Monto (BOB);Participacion (%)\n");
        for (RubroResultadoDTO r : data.resumenEscalonado()) {
            sb.append(escaparCsv(r.titulo())).append(";")
                    .append(r.monto()).append(";")
                    .append(r.porcentajeVertical()).append("%\n");
        }

        sb.append("\nDETALLE DE INGRESOS\n");
        sb.append("Codigo;Nombre Cuenta;Nivel;Saldo (BOB);% Vertical\n");
        exportarNodosCsv(data.detalleIngresos(), sb, "");

        sb.append("\nDETALLE DE EGRESOS\n");
        sb.append("Codigo;Nombre Cuenta;Nivel;Saldo (BOB);% Vertical\n");
        exportarNodosCsv(data.detalleEgresos(), sb, "");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- Helpers de cálculo y jerarquía ---

    private BigDecimal calcularSaldoRollup(
            CuentaContable cuenta,
            Map<Long, List<CuentaContable>> hijosPorPadre,
            Map<Long, BigDecimal> saldosDirectos,
            Map<Long, BigDecimal> cacheTotales
    ) {
        if (cacheTotales.containsKey(cuenta.getId())) {
            return cacheTotales.get(cuenta.getId());
        }

        BigDecimal suma = saldosDirectos.getOrDefault(cuenta.getId(), BigDecimal.ZERO);
        List<CuentaContable> hijos = hijosPorPadre.getOrDefault(cuenta.getId(), Collections.emptyList());
        for (CuentaContable h : hijos) {
            suma = suma.add(calcularSaldoRollup(h, hijosPorPadre, saldosDirectos, cacheTotales));
        }

        cacheTotales.put(cuenta.getId(), suma);
        return suma;
    }

    private List<NodoFinancieroDTO> construirArbolNodos(
            List<CuentaContable> cuentasNivel,
            Map<Long, List<CuentaContable>> hijosPorPadre,
            Map<Long, BigDecimal> saldosTotales,
            int maxNivel,
            BigDecimal baseIngresos
    ) {
        List<NodoFinancieroDTO> resultado = new ArrayList<>();
        for (CuentaContable c : cuentasNivel) {
            BigDecimal saldo = saldosTotales.getOrDefault(c.getId(), BigDecimal.ZERO);
            BigDecimal porc = calcularPorcentaje(saldo, baseIngresos);

            List<NodoFinancieroDTO> subcuentas = Collections.emptyList();
            if (c.getNivel() < maxNivel) {
                List<CuentaContable> hijos = hijosPorPadre.getOrDefault(c.getId(), Collections.emptyList());
                if (!hijos.isEmpty()) {
                    subcuentas = construirArbolNodos(hijos, hijosPorPadre, saldosTotales, maxNivel, baseIngresos);
                }
            }

            resultado.add(new NodoFinancieroDTO(
                    c.getId(),
                    c.getCodigo(),
                    c.getNombre(),
                    c.getNivel(),
                    c.getTipo(),
                    c.getNaturaleza(),
                    c.getPermiteMovimiento(),
                    saldo,
                    porc,
                    subcuentas
            ));
        }
        return resultado;
    }

    private BigDecimal sumarNodosRaiz(List<NodoFinancieroDTO> nodos) {
        BigDecimal sum = BigDecimal.ZERO;
        for (NodoFinancieroDTO n : nodos) {
            sum = sum.add(n.saldo());
        }
        return sum;
    }

    private BigDecimal calcularPorcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0 || parte == null) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private LocalDate resolverFechaCorte(Integer gestion, Integer mesCorte, LocalDate fechaCorte) {
        if (fechaCorte != null) return fechaCorte;
        if (gestion != null && mesCorte != null && mesCorte >= 1 && mesCorte <= 12) {
            return LocalDate.of(gestion, mesCorte, 1).plusMonths(1).minusDays(1);
        }
        if (gestion != null) {
            return LocalDate.of(gestion, 12, 31);
        }
        return LocalDate.now();
    }

    private LocalDate[] resolverRangoFechas(Integer gestion, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null) {
            return new LocalDate[]{fechaInicio, fechaFin};
        }
        if (gestion != null) {
            return new LocalDate[]{LocalDate.of(gestion, 1, 1), LocalDate.of(gestion, 12, 31)};
        }
        int anioActual = LocalDate.now().getYear();
        return new LocalDate[]{LocalDate.of(anioActual, 1, 1), LocalDate.now()};
    }

    private void exportarNodosCsv(List<NodoFinancieroDTO> nodos, StringBuilder sb, String indent) {
        for (NodoFinancieroDTO n : nodos) {
            sb.append(escaparCsv(n.tipo())).append(";")
                    .append(escaparCsv(n.codigo())).append(";")
                    .append(escaparCsv(indent + n.nombre())).append(";")
                    .append(n.nivel()).append(";")
                    .append(Boolean.TRUE.equals(n.permiteMovimiento()) ? "SI" : "NO").append(";")
                    .append(n.saldo()).append(";")
                    .append(n.porcentajeVertical()).append("%\n");

            if (n.subcuentas() != null && !n.subcuentas().isEmpty()) {
                exportarNodosCsv(n.subcuentas(), sb, indent + "  ");
            }
        }
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        String limpio = valor.replace("\"", "\"\"");
        if (limpio.contains(";") || limpio.contains("\n") || limpio.contains(",")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }
}
