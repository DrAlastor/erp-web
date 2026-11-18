package modulo.contabilidad_y_finanzas.cu26_periodos_contables.service;

import modulo.contabilidad_y_finanzas.compartido.Solicitante;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.GestionResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.PeriodoResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.repository.PeriodosContablesRepositorio;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service.BitacoraTenantAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CU26 - Gestionar Periodos Contables.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Una gestión tiene 12 periodos mensuales. Generarla crea solo los meses que faltan (idempotente),
 *       entre {@value ReglasPeriodos#ANIOS_ATRAS} años atrás y {@value ReglasPeriodos#ANIOS_ADELANTE} adelante.</li>
 *   <li>El cierre es secuencial: un mes se cierra solo si los anteriores de la gestión ya están cerrados.</li>
 *   <li>Solo se reabre el último mes cerrado de la gestión (el siguiente no puede estar cerrado).</li>
 *   <li>Un periodo cerrado no admite asientos: la integración de ventas responde 409.</li>
 *   <li>Los periodos nunca se borran (sus asientos caerían en cascada).</li>
 * </ul>
 * Los asientos de ajuste y de cierre del ejercicio corresponden al CU28.
 */
@Service
public class PeriodosContablesService {

    private static final String ENTIDAD = "periodos_contables";

    private final PeriodosContablesRepositorio periodos;
    private final BitacoraTenantAuditoriaService bitacora;
    private final Clock reloj;

    @Autowired
    public PeriodosContablesService(PeriodosContablesRepositorio periodos, BitacoraTenantAuditoriaService bitacora) {
        this(periodos, bitacora, Clock.systemDefaultZone());
    }

    PeriodosContablesService(PeriodosContablesRepositorio periodos, BitacoraTenantAuditoriaService bitacora,
                             Clock reloj) {
        this.periodos = periodos;
        this.bitacora = bitacora;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------- consultas --

    @Transactional(readOnly = true)
    public List<Integer> gestiones() {
        return periodos.gestiones();
    }

    @Transactional(readOnly = true)
    public GestionResponse gestion(int gestion) {
        return respuesta(gestion, periodos.findByGestionOrderByMesAsc(gestion), 0);
    }

    @Transactional(readOnly = true)
    public PeriodoResponse obtener(Long id) {
        PeriodoContable periodo = cargar(id);
        return aRespuesta(periodo, contarAsientos(periodo.getGestion()));
    }

    // ------------------------------------------------------------ operaciones --

    @Transactional
    public GestionResponse generarGestion(int gestion, Solicitante quien) {
        int anioActual = Year.now(reloj).getValue();
        if (!ReglasPeriodos.gestionPermitida(gestion, anioActual)) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "Solo se pueden generar gestiones entre "
                    + (anioActual - ReglasPeriodos.ANIOS_ATRAS) + " y " + (anioActual + ReglasPeriodos.ANIOS_ADELANTE) + ".");
        }
        Set<Integer> existentes = periodos.bloquearGestion(gestion).stream()
                .map(PeriodoContable::getMes).collect(Collectors.toSet());
        List<PeriodoContable> nuevos = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            if (!existentes.contains(mes)) {
                nuevos.add(ReglasPeriodos.nuevo(gestion, mes));
            }
        }
        if (!nuevos.isEmpty()) {
            List<PeriodoContable> guardados = periodos.saveAllAndFlush(nuevos);
            bitacora.registrar("PERIODO_GENERADO", quien.actor(), ENTIDAD, guardados.get(0).getId(), quien.ip());
        }
        return respuesta(gestion, periodos.findByGestionOrderByMesAsc(gestion), nuevos.size());
    }

    @Transactional
    public PeriodoResponse cerrar(Long id, Solicitante quien) {
        PeriodoContable periodo = cargar(id);
        Map<Integer, PeriodoContable> porMes = bloquearGestion(periodo.getGestion());
        periodo = porMes.get(periodo.getMes());
        if (periodo.estaCerrado()) {
            throw rechazo(HttpStatus.CONFLICT, "El periodo " + periodo.getNombre() + " ya está cerrado.");
        }
        for (int mes = 1; mes < periodo.getMes(); mes++) {
            PeriodoContable anterior = porMes.get(mes);
            if (anterior == null) {
                throw rechazo(HttpStatus.CONFLICT, "Falta el periodo " + ReglasPeriodos.nombre(periodo.getGestion(), mes)
                        + "; genere la gestión completa antes de cerrar.");
            }
            if (!anterior.estaCerrado()) {
                throw rechazo(HttpStatus.CONFLICT, "Primero cierre el periodo " + anterior.getNombre()
                        + ": los periodos se cierran en orden.");
            }
        }
        periodo.setEstado(PeriodoContable.ESTADO_CERRADO);
        periodo.setFechaCierre(LocalDateTime.now(reloj));
        PeriodoContable guardado = periodos.saveAndFlush(periodo);
        bitacora.registrar("PERIODO_CERRADO", quien.actor(), ENTIDAD, guardado.getId(), quien.ip());
        return aRespuesta(guardado, contarAsientos(guardado.getGestion()));
    }

    @Transactional
    public PeriodoResponse reabrir(Long id, Solicitante quien) {
        PeriodoContable periodo = cargar(id);
        Map<Integer, PeriodoContable> porMes = bloquearGestion(periodo.getGestion());
        periodo = porMes.get(periodo.getMes());
        if (!periodo.estaCerrado()) {
            throw rechazo(HttpStatus.CONFLICT, "El periodo " + periodo.getNombre() + " no está cerrado.");
        }
        PeriodoContable siguiente = porMes.get(periodo.getMes() + 1);
        if (siguiente != null && siguiente.estaCerrado()) {
            throw rechazo(HttpStatus.CONFLICT, "Solo se puede reabrir el último periodo cerrado; primero reabra "
                    + siguiente.getNombre() + ".");
        }
        periodo.setEstado(PeriodoContable.ESTADO_ABIERTO);
        periodo.setFechaCierre(null);
        PeriodoContable guardado = periodos.saveAndFlush(periodo);
        bitacora.registrar("PERIODO_REABIERTO", quien.actor(), ENTIDAD, guardado.getId(), quien.ip());
        return aRespuesta(guardado, contarAsientos(guardado.getGestion()));
    }

    // --------------------------------------------------------------- apoyo --

    private Map<Integer, PeriodoContable> bloquearGestion(Integer gestion) {
        Map<Integer, PeriodoContable> porMes = new HashMap<>();
        periodos.bloquearGestion(gestion).forEach(p -> porMes.put(p.getMes(), p));
        return porMes;
    }

    private GestionResponse respuesta(int gestion, List<PeriodoContable> lista, int creados) {
        Map<Long, Long> asientos = contarAsientos(gestion);
        List<PeriodoResponse> datos = lista.stream().map(p -> aRespuesta(p, asientos)).toList();
        int cerrados = (int) lista.stream().filter(PeriodoContable::estaCerrado).count();
        return new GestionResponse(gestion, lista.size() - cerrados, cerrados, creados, datos);
    }

    private Map<Long, Long> contarAsientos(Integer gestion) {
        Map<Long, Long> conteo = new HashMap<>();
        for (Object[] fila : periodos.contarAsientosPorPeriodo(gestion)) {
            conteo.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        return conteo;
    }

    private static PeriodoResponse aRespuesta(PeriodoContable p, Map<Long, Long> asientos) {
        return new PeriodoResponse(p.getId(), p.getGestion(), p.getMes(), p.getNombre(), p.getFechaInicio(),
                p.getFechaFin(), p.getEstado(), p.getFechaCierre(), asientos.getOrDefault(p.getId(), 0L));
    }

    private PeriodoContable cargar(Long id) {
        return periodos.findById(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "El periodo contable no existe."));
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
