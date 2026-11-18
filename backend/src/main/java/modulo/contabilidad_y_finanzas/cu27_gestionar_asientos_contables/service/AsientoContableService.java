package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.contabilidad_y_finanzas.compartido.PermisoFuncionContable;
import modulo.contabilidad_y_finanzas.compartido.UsuarioContableResolver;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AnularAsientoRequest;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoRequest;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResumenResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.CuentaOpcionResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.repository.AsientoConsultaRepository;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.repository.AsientoConsultaRepository.Cabecera;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service.ReglasAsiento.AsientoInvalido;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import modulo.ventas_y_facturacion.compartido.entity.AsientoDetalle;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.AsientoContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HU-27 / CU27: gestionar asientos contables (Libro Diario).
 *
 * <p>Ciclo de un asiento manual: se registra en BORRADOR (se puede corregir), se
 * contabiliza (MAYORIZADO, ya no se toca) y, si estaba mal, se anula: un borrador pasa a
 * ANULADO sin más; uno mayorizado genera un asiento de REVERSION con las mismas líneas
 * invertidas y queda ANULADO. Nunca se borra un asiento. Los asientos automáticos (ventas,
 * cobros, notas de crédito...) se ven aquí pero se corrigen desde su documento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AsientoContableService {

    private static final int MAX_OPCIONES = 30;

    private final AsientoContableRepository asientos;
    private final CuentaContableRepository cuentas;
    private final AsientoConsultaRepository consultas;
    private final ContabilidadVentaService contabilidad;
    private final UsuarioContableResolver usuarioResolver;
    private final PermisoFuncionContable permisos;
    private final BitacoraAuditoriaService bitacora;

    public Page<AsientoResumenResponse> list(AsientoConsultaRepository.Filtro filtro, int page, int size) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw rechazo("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        if (filtro.estado() != null && !filtro.estado().isBlank()
                && !ReglasAsiento.ESTADOS.contains(filtro.estado().trim().toUpperCase(Locale.ROOT))) {
            throw rechazo("Estado no válido: " + filtro.estado());
        }
        if (filtro.origen() != null && !filtro.origen().isBlank()
                && !List.of("MANUAL", "AUTOMATICO").contains(filtro.origen().trim().toUpperCase(Locale.ROOT))) {
            throw rechazo("Origen no válido: use MANUAL o AUTOMATICO.");
        }
        long total = consultas.contar(filtro);
        return new PageImpl<>(consultas.listar(filtro, size, (long) page * size), PageRequest.of(page, size), total);
    }

    public AsientoResponse detail(Long id) {
        return respuesta(id);
    }

    @Transactional
    public AsientoResponse registrar(AsientoRequest request, Authentication authentication) {
        Long usuarioId = editor(authentication);
        String tipo = tipo(request.tipo());
        ReglasAsiento.Totales totales = validar(request);
        PeriodoContable periodo = contabilidad.periodoAbierto(request.fecha());

        AsientoContable asiento = AsientoContable.builder()
                .periodoContableId(periodo.getId())
                .creadoPor(usuarioId)
                .numeroAsiento(contabilidad.siguienteNumero(request.fecha().getYear()))
                .fecha(request.fecha())
                .tipoAsiento(tipo)
                .glosa(request.glosa().trim())
                .totalDebe(totales.debe())
                .totalHaber(totales.haber())
                .estado(ReglasAsiento.BORRADOR)
                .build();
        agregarLineas(asiento, request.lineas());
        asiento = asientos.saveAndFlush(asiento);
        auditar("REGISTRO_ASIENTO", asiento, "Registro del asiento " + asiento.getNumeroAsiento()
                + " (" + tipo + ") en borrador por Bs. " + totales.debe().toPlainString() + ".");
        return respuesta(asiento.getId());
    }

    @Transactional
    public AsientoResponse modificar(Long id, AsientoRequest request, Authentication authentication) {
        editor(authentication);
        AsientoContable asiento = buscar(id);
        exigirBorrador(asiento, "modificar");
        String tipo = tipo(request.tipo());
        ReglasAsiento.Totales totales = validar(request);
        PeriodoContable periodo = contabilidad.periodoAbierto(request.fecha());
        if (asiento.getFecha().getYear() != request.fecha().getYear()) {
            throw rechazo("La fecha no puede cambiar de gestión: el número " + asiento.getNumeroAsiento()
                    + " es de " + asiento.getFecha().getYear() + ".");
        }

        asiento.setPeriodoContableId(periodo.getId());
        asiento.setFecha(request.fecha());
        asiento.setTipoAsiento(tipo);
        asiento.setGlosa(request.glosa().trim());
        asiento.setTotalDebe(totales.debe());
        asiento.setTotalHaber(totales.haber());
        asiento.getDetalles().clear();
        asientos.flush();
        agregarLineas(asiento, request.lineas());
        asientos.saveAndFlush(asiento);
        auditar("MODIFICACION_ASIENTO", asiento, "Modificación del asiento en borrador " + asiento.getNumeroAsiento() + ".");
        return respuesta(id);
    }

    /** Pasa un borrador a MAYORIZADO: desde aquí cuenta en los libros y ya no se edita. */
    @Transactional
    public AsientoResponse contabilizar(Long id, Authentication authentication) {
        editor(authentication);
        AsientoContable asiento = buscar(id);
        exigirBorrador(asiento, "contabilizar");
        contabilidad.periodoAbierto(asiento.getFecha());
        asiento.getDetalles().forEach(d -> exigirCuentaDeMovimiento(d.getCuentaContable()));
        if (asiento.getTotalDebe().compareTo(asiento.getTotalHaber()) != 0) {
            throw rechazo("El asiento no cuadra; corríjalo antes de contabilizarlo.");
        }
        asiento.setEstado(ReglasAsiento.MAYORIZADO);
        asientos.saveAndFlush(asiento);
        auditar("CONTABILIZACION_ASIENTO", asiento, "Contabilización del asiento " + asiento.getNumeroAsiento() + ".");
        return respuesta(id);
    }

    /**
     * Anula un asiento manual. Si ya estaba mayorizado se registra la reversión en la fecha
     * de hoy (el periodo de hoy debe estar abierto) y el original queda ANULADO.
     */
    @Transactional
    public AsientoResponse anular(Long id, AnularAsientoRequest request, Authentication authentication) {
        AsientoContable asiento = buscar(id);
        if (!ReglasAsiento.esManual(asiento.getTipoAsiento())) {
            throw conflicto("El asiento " + asiento.getNumeroAsiento() + " es automático (" + asiento.getTipoAsiento()
                    + "); se anula desde su documento de origen.");
        }
        if (ReglasAsiento.ANULADO.equalsIgnoreCase(asiento.getEstado())) {
            throw conflicto("El asiento " + asiento.getNumeroAsiento() + " ya está anulado.");
        }
        String motivo = request.motivo().trim();
        String detalle;
        if (ReglasAsiento.MAYORIZADO.equalsIgnoreCase(asiento.getEstado())) {
            List<ContabilidadVentaService.Partida> inversas = asiento.getDetalles().stream()
                    .map(d -> new ContabilidadVentaService.Partida(
                            d.getCuentaContable().getCodigo(), d.getGlosa(), d.getDebe(), d.getHaber()).invertida())
                    .toList();
            String reversion = contabilidad.registrarAsiento(LocalDate.now(), ReglasAsiento.REVERSION,
                    "Reversión de " + asiento.getNumeroAsiento() + ". Motivo: " + motivo,
                    usuarioResolver.resolver(authentication), inversas);
            detalle = "Anulación del asiento " + asiento.getNumeroAsiento() + " con la reversión " + reversion
                    + ". Motivo: " + motivo;
        } else {
            detalle = "Anulación del borrador " + asiento.getNumeroAsiento() + ". Motivo: " + motivo;
        }
        asiento.setEstado(ReglasAsiento.ANULADO);
        asientos.saveAndFlush(asiento);
        auditar("ANULACION_ASIENTO", asiento, detalle);
        return respuesta(id);
    }

    public List<CuentaOpcionResponse> cuentas(String search) {
        return consultas.cuentasDeMovimiento(search, MAX_OPCIONES);
    }

    /** Qué puede hacer el usuario conectado, para que la pantalla muestre solo esos botones. */
    public Map<String, Boolean> permisos(Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        return Map.of("editar", permisos.puedeEditar(usuarioId, PermisoFuncionContable.ASIENTOS),
                "anular", permisos.esAdministrador(usuarioId));
    }

    public List<String> tipos() {
        return ReglasAsiento.TIPOS_MANUALES;
    }

    // ------------------------------------------------------------------ internos

    private AsientoResponse respuesta(Long id) {
        Cabecera c = consultas.cabecera(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asiento contable inexistente"));
        AsientoResumenResponse r = c.resumen();
        boolean borrador = ReglasAsiento.BORRADOR.equalsIgnoreCase(r.estado());
        boolean anulable = r.manual() && !ReglasAsiento.ANULADO.equalsIgnoreCase(r.estado());
        return new AsientoResponse(r.id(), r.numero(), r.fecha(), r.tipo(), r.glosa(), r.totalDebe(),
                r.totalHaber(), r.estado(), r.manual(), r.manual() && borrador, anulable, r.periodo(),
                c.estadoPeriodo(), r.creadoPor(), c.fechaCreacion(), consultas.lineas(id));
    }

    /** Usuario autenticado con Edición en «Gestionar Asientos Contables». */
    private Long editor(Authentication authentication) {
        Long usuarioId = usuarioResolver.resolver(authentication);
        permisos.exigirEdicion(usuarioId, PermisoFuncionContable.ASIENTOS);
        return usuarioId;
    }

    private AsientoContable buscar(Long id) {
        return asientos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asiento contable inexistente"));
    }

    private void exigirBorrador(AsientoContable asiento, String accion) {
        if (!ReglasAsiento.esManual(asiento.getTipoAsiento())) {
            throw conflicto("El asiento " + asiento.getNumeroAsiento() + " es automático; no se puede " + accion + ".");
        }
        if (!ReglasAsiento.BORRADOR.equalsIgnoreCase(asiento.getEstado())) {
            throw conflicto("Solo se puede " + accion + " un asiento en BORRADOR; el " + asiento.getNumeroAsiento()
                    + " está " + asiento.getEstado() + ".");
        }
    }

    private String tipo(String tipo) {
        try {
            return ReglasAsiento.tipoManual(tipo);
        } catch (AsientoInvalido e) {
            throw rechazo(e.getMessage());
        }
    }

    private ReglasAsiento.Totales validar(AsientoRequest request) {
        try {
            return ReglasAsiento.validarLineas(request.lineas());
        } catch (AsientoInvalido e) {
            throw rechazo(e.getMessage());
        }
    }

    private void agregarLineas(AsientoContable asiento, List<AsientoRequest.Linea> lineas) {
        for (AsientoRequest.Linea linea : lineas) {
            CuentaContable cuenta = cuentas.findById(linea.cuentaId())
                    .orElseThrow(() -> rechazo("La cuenta #" + linea.cuentaId() + " no existe en el plan de cuentas."));
            exigirCuentaDeMovimiento(cuenta);
            String glosa = linea.glosa() == null || linea.glosa().isBlank() ? null : linea.glosa().trim();
            asiento.agregarDetalle(AsientoDetalle.builder()
                    .cuentaContable(cuenta)
                    .glosa(glosa)
                    .debe(ReglasAsiento.escala(linea.debe()))
                    .haber(ReglasAsiento.escala(linea.haber()))
                    .build());
        }
    }

    private void exigirCuentaDeMovimiento(CuentaContable cuenta) {
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw rechazo("La cuenta " + cuenta.getCodigo() + " - " + cuenta.getNombre() + " está inactiva.");
        }
        if (!Boolean.TRUE.equals(cuenta.getPermiteMovimiento())) {
            throw rechazo("La cuenta " + cuenta.getCodigo() + " - " + cuenta.getNombre()
                    + " es de agrupación; use una cuenta de movimiento.");
        }
    }

    private void auditar(String operacion, AsientoContable asiento, String detalle) {
        try {
            bitacora.registrarEventoActual(operacion, "CONTABILIDAD", detalle, "EXITO", "MEDIA",
                    String.format("{\"asientoId\":%d,\"numero\":\"%s\",\"estado\":\"%s\"}",
                            asiento.getId(), asiento.getNumeroAsiento(), asiento.getEstado()));
        } catch (Exception e) {
            log.warn("No se pudo registrar en la bitácora: {}", e.getMessage());
        }
    }

    private static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private static ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
