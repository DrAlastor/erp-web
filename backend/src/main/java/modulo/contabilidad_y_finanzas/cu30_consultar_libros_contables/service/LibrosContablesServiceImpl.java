package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto.*;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository.LibroDiarioRepository;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository.LibroMayorRepository;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository.SaldoCuentaProjection;
import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibrosContablesServiceImpl implements LibrosContablesService {

    private final LibroDiarioRepository libroDiarioRepository;
    private final LibroMayorRepository libroMayorRepository;
    private final PeriodoContableRepository periodoContableRepository;
    private final CuentaContableRepository cuentaContableRepository;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<PeriodoContableSimpleDTO> listarPeriodos() {
        return periodoContableRepository.findAllByOrderByGestionDescMesDesc().stream()
                .map(p -> new PeriodoContableSimpleDTO(
                        p.getId(),
                        p.getGestion(),
                        p.getMes(),
                        p.getNombre(),
                        p.getFechaInicio(),
                        p.getFechaFin(),
                        p.getEstado()
                ))
                .toList();
    }

    @Override
    public List<CuentaContableSimpleDTO> listarCuentasMovimiento() {
        return cuentaContableRepository.findByPermiteMovimientoTrueAndActivoTrueOrderByCodigoAsc().stream()
                .map(c -> new CuentaContableSimpleDTO(
                        c.getId(),
                        c.getCodigo(),
                        c.getNombre(),
                        c.getNivel(),
                        c.getTipo(),
                        c.getNaturaleza(),
                        c.getPermiteMovimiento()
                ))
                .toList();
    }

    @Override
    public List<String> listarTiposAsiento() {
        return List.of("TODOS", "APERTURA", "VENTAS", "COMPRAS", "INGRESO", "EGRESO", "TRASPASO", "AJUSTE", "CIERRE");
    }

    @Override
    public LibroDiarioResponse obtenerLibroDiario(
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tipoAsiento,
            String estado,
            String busqueda
    ) {
        String periodoNombre = "Todos los periodos";

        if (periodoId != null) {
            PeriodoContable periodo = periodoContableRepository.findById(periodoId).orElse(null);
            if (periodo != null) {
                periodoNombre = periodo.getNombre() + " (" + periodo.getGestion() + ")";
                if (fechaInicio == null) fechaInicio = periodo.getFechaInicio();
                if (fechaFin == null) fechaFin = periodo.getFechaFin();
            }
        }

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        final LocalDate finalFechaInicio = fechaInicio;
        final LocalDate finalFechaFin = fechaFin;
        final String tipoFiltro = (tipoAsiento != null && !tipoAsiento.isBlank() && !"TODOS".equalsIgnoreCase(tipoAsiento))
                ? tipoAsiento.trim().toUpperCase() : null;
        final String estadoFiltro = (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado))
                ? estado.trim().toUpperCase() : null;
        final String busquedaFiltro = (busqueda != null && !busqueda.isBlank()) ? busqueda.trim().toLowerCase() : null;

        Specification<AsientoContable> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (periodoId != null) {
                predicates.add(cb.equal(root.get("periodoContableId"), periodoId));
            }
            if (finalFechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), finalFechaInicio));
            }
            if (finalFechaFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), finalFechaFin));
            }
            if (tipoFiltro != null) {
                predicates.add(cb.equal(cb.upper(root.get("tipoAsiento")), tipoFiltro));
            }
            if (estadoFiltro != null) {
                predicates.add(cb.equal(cb.upper(root.get("estado")), estadoFiltro));
            }
            if (busquedaFiltro != null) {
                String pattern = "%" + busquedaFiltro + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("numeroAsiento")), pattern),
                        cb.like(cb.lower(root.get("glosa")), pattern)
                ));
            }

            if (query != null) {
                query.orderBy(cb.asc(root.get("fecha")), cb.asc(root.get("numeroAsiento")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<AsientoContable> rawList = libroDiarioRepository.findAll(spec);
        Set<Long> idsVistos = new HashSet<>();
        List<AsientoContable> asientos = new ArrayList<>();
        for (AsientoContable a : rawList) {
            if (idsVistos.add(a.getId())) {
                asientos.add(a);
            }
        }

        BigDecimal totalDebeGeneral = BigDecimal.ZERO;
        BigDecimal totalHaberGeneral = BigDecimal.ZERO;
        List<AsientoDiarioDTO> asientoDTOs = new ArrayList<>();

        for (AsientoContable a : asientos) {
            List<LineaAsientoDTO> lineas = new ArrayList<>();
            for (AsientoDetalle d : a.getDetalles()) {
                lineas.add(new LineaAsientoDTO(
                        d.getId(),
                        d.getCuentaContable().getId(),
                        d.getCuentaContable().getCodigo(),
                        d.getCuentaContable().getNombre(),
                        d.getGlosa(),
                        d.getDebe(),
                        d.getHaber()
                ));
            }

            totalDebeGeneral = totalDebeGeneral.add(a.getTotalDebe());
            totalHaberGeneral = totalHaberGeneral.add(a.getTotalHaber());

            asientoDTOs.add(new AsientoDiarioDTO(
                    a.getId(),
                    a.getNumeroAsiento(),
                    a.getFecha(),
                    a.getTipoAsiento(),
                    a.getGlosa(),
                    a.getTotalDebe(),
                    a.getTotalHaber(),
                    a.getEstado(),
                    lineas
            ));
        }

        boolean estaCuadrado = totalDebeGeneral.compareTo(totalHaberGeneral) == 0;

        return new LibroDiarioResponse(
                periodoNombre,
                totalDebeGeneral,
                totalHaberGeneral,
                estaCuadrado,
                asientoDTOs.size(),
                asientoDTOs
        );
    }

    @Override
    public LibroMayorResponse obtenerLibroMayor(
            Long cuentaId,
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        String periodoNombre = "Todos los periodos";

        if (periodoId != null) {
            PeriodoContable periodo = periodoContableRepository.findById(periodoId).orElse(null);
            if (periodo != null) {
                periodoNombre = periodo.getNombre() + " (" + periodo.getGestion() + ")";
                if (fechaInicio == null) fechaInicio = periodo.getFechaInicio();
                if (fechaFin == null) fechaFin = periodo.getFechaFin();
            }
        }

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        List<CuentaContable> cuentasAProcesar = new ArrayList<>();
        if (cuentaId != null) {
            CuentaContable c = cuentaContableRepository.findById(cuentaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta contable no encontrada."));
            if (!Boolean.TRUE.equals(c.getPermiteMovimiento())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta " + c.getCodigo() + " es de agrupación y no tiene movimientos directos en el Libro Mayor.");
            }
            cuentasAProcesar.add(c);
        } else {
            cuentasAProcesar = cuentaContableRepository.findByPermiteMovimientoTrueAndActivoTrueOrderByCodigoAsc();
        }

        LocalDate fInicio = (fechaInicio != null) ? fechaInicio : LocalDate.of(1900, 1, 1);
        LocalDate fFin = (fechaFin != null) ? fechaFin : LocalDate.of(2099, 12, 31);
        List<CuentaMayorDTO> cuentasMayor = new ArrayList<>();

        Map<Long, SaldoCuentaProjection> saldosAntMap = new HashMap<>();
        Map<Long, List<AsientoDetalle>> movimientosMap = new HashMap<>();

        if (cuentaId == null) {
            // OPTIMIZACIÓN DE ALTO RENDIMIENTO: Batch queries únicas en lugar de N+1 consultas
            if (fechaInicio != null) {
                for (SaldoCuentaProjection p : libroMayorRepository.sumSaldosAnterioresBatch(fechaInicio)) {
                    saldosAntMap.put(p.getCuentaId(), p);
                }
            }
            List<AsientoDetalle> todosMovimientos = libroMayorRepository.findMovimientosMayorBatch(fInicio, fFin);
            for (AsientoDetalle d : todosMovimientos) {
                movimientosMap.computeIfAbsent(d.getCuentaContable().getId(), k -> new ArrayList<>()).add(d);
            }
        }

        for (CuentaContable c : cuentasAProcesar) {
            BigDecimal debeAnt;
            BigDecimal haberAnt;
            List<AsientoDetalle> detalles;

            if (cuentaId != null) {
                debeAnt = (fechaInicio != null)
                        ? libroMayorRepository.sumDebeAnterior(c.getId(), fechaInicio)
                        : BigDecimal.ZERO;
                haberAnt = (fechaInicio != null)
                        ? libroMayorRepository.sumHaberAnterior(c.getId(), fechaInicio)
                        : BigDecimal.ZERO;
                detalles = libroMayorRepository.findMovimientosMayor(c.getId(), fInicio, fFin);
            } else {
                SaldoCuentaProjection p = saldosAntMap.get(c.getId());
                debeAnt = (p != null && p.getTotalDebe() != null) ? p.getTotalDebe() : BigDecimal.ZERO;
                haberAnt = (p != null && p.getTotalHaber() != null) ? p.getTotalHaber() : BigDecimal.ZERO;
                detalles = movimientosMap.getOrDefault(c.getId(), Collections.emptyList());
            }

            if (debeAnt == null) debeAnt = BigDecimal.ZERO;
            if (haberAnt == null) haberAnt = BigDecimal.ZERO;

            boolean esDeudora = "DEUDORA".equalsIgnoreCase(c.getNaturaleza());
            BigDecimal saldoAnterior = esDeudora
                    ? debeAnt.subtract(haberAnt)
                    : haberAnt.subtract(debeAnt);

            // Si es consulta general y la cuenta no tiene saldo anterior ni movimientos en el periodo, se omite
            if (cuentaId == null && saldoAnterior.compareTo(BigDecimal.ZERO) == 0 && detalles.isEmpty()) {
                continue;
            }

            BigDecimal saldoAcumulado = saldoAnterior;
            BigDecimal totalDebePeriodo = BigDecimal.ZERO;
            BigDecimal totalHaberPeriodo = BigDecimal.ZERO;
            List<MovimientoMayorDTO> movimientosDTO = new ArrayList<>();

            for (AsientoDetalle d : detalles) {
                totalDebePeriodo = totalDebePeriodo.add(d.getDebe());
                totalHaberPeriodo = totalHaberPeriodo.add(d.getHaber());

                if (esDeudora) {
                    saldoAcumulado = saldoAcumulado.add(d.getDebe()).subtract(d.getHaber());
                } else {
                    saldoAcumulado = saldoAcumulado.add(d.getHaber()).subtract(d.getDebe());
                }

                String glosaEfectiva = (d.getGlosa() != null && !d.getGlosa().isBlank())
                        ? d.getGlosa()
                        : d.getAsiento().getGlosa();

                movimientosDTO.add(new MovimientoMayorDTO(
                        d.getAsiento().getFecha(),
                        d.getAsiento().getNumeroAsiento(),
                        d.getAsiento().getTipoAsiento(),
                        glosaEfectiva,
                        d.getDebe(),
                        d.getHaber(),
                        saldoAcumulado
                ));
            }

            cuentasMayor.add(new CuentaMayorDTO(
                    c.getId(),
                    c.getCodigo(),
                    c.getNombre(),
                    c.getTipo(),
                    c.getNaturaleza(),
                    saldoAnterior,
                    totalDebePeriodo,
                    totalHaberPeriodo,
                    saldoAcumulado,
                    movimientosDTO
            ));
        }

        return new LibroMayorResponse(
                periodoNombre,
                fechaInicio,
                fechaFin,
                cuentasMayor.size(),
                cuentasMayor
        );
    }

    @Override
    public byte[] exportarLibroDiarioCsv(
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tipoAsiento,
            String estado,
            String busqueda
    ) {
        LibroDiarioResponse data = obtenerLibroDiario(periodoId, fechaInicio, fechaFin, tipoAsiento, estado, busqueda);
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 para apertura correcta en Excel
        sb.append('\uFEFF');
        sb.append("LIBRO DIARIO - NEXO ERP\n");
        sb.append("Periodo:;").append(data.periodoSeleccionado()).append("\n");
        sb.append("Total Debe:;").append(data.totalDebeGeneral()).append(";Total Haber:;").append(data.totalHaberGeneral())
                .append(";Cuadrado:;").append(data.estaCuadrado() ? "SI" : "NO").append("\n\n");

        sb.append("Fecha;Numero Asiento;Tipo;Estado;Codigo Cuenta;Nombre Cuenta;Glosa Detalle;Debe (BOB);Haber (BOB)\n");

        for (AsientoDiarioDTO a : data.asientos()) {
            for (LineaAsientoDTO l : a.detalles()) {
                sb.append(a.fecha().format(FORMATO_FECHA)).append(";")
                        .append(escaparCsv(a.numeroAsiento())).append(";")
                        .append(escaparCsv(a.tipoAsiento())).append(";")
                        .append(escaparCsv(a.estado())).append(";")
                        .append(escaparCsv(l.cuentaCodigo())).append(";")
                        .append(escaparCsv(l.cuentaNombre())).append(";")
                        .append(escaparCsv(l.glosa() != null ? l.glosa() : a.glosa())).append(";")
                        .append(l.debe()).append(";")
                        .append(l.haber()).append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportarLibroMayorCsv(
            Long cuentaId,
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        LibroMayorResponse data = obtenerLibroMayor(cuentaId, periodoId, fechaInicio, fechaFin);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("LIBRO MAYOR GENERAL - NEXO ERP\n");
        sb.append("Periodo:;").append(data.periodoSeleccionado()).append("\n\n");

        for (CuentaMayorDTO c : data.cuentas()) {
            sb.append("CUENTA:;").append(c.codigo()).append(" - ").append(escaparCsv(c.nombre()))
                    .append(";Tipo:;").append(c.tipo()).append(";Naturaleza:;").append(c.naturaleza()).append("\n");
            sb.append("Saldo Anterior:;").append(c.saldoAnterior()).append("\n");
            sb.append("Fecha;Comprobante;Tipo;Concepto / Glosa;Debe (BOB);Haber (BOB);Saldo Acumulado (BOB)\n");

            for (MovimientoMayorDTO m : c.movimientos()) {
                sb.append(m.fecha().format(FORMATO_FECHA)).append(";")
                        .append(escaparCsv(m.numeroAsiento())).append(";")
                        .append(escaparCsv(m.tipoAsiento())).append(";")
                        .append(escaparCsv(m.glosa())).append(";")
                        .append(m.debe()).append(";")
                        .append(m.haber()).append(";")
                        .append(m.saldoAcumulado()).append("\n");
            }

            sb.append("Totales Periodo:;;;;").append(c.totalDebe()).append(";").append(c.totalHaber()).append(";")
                    .append(c.saldoFinal()).append("\n\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
