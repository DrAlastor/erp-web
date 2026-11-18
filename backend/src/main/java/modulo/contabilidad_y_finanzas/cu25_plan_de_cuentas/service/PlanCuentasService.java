package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service;

import modulo.contabilidad_y_finanzas.compartido.Solicitante;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaActualizarRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaNodoResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.mapper.PlanCuentasMapper;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.repository.PlanCuentasRepositorio;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service.BitacoraTenantAuditoriaService;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CU25 - Gestionar Plan de Cuentas.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El código es único, numérico por segmentos, y una subcuenta agrega un segmento al de su padre.</li>
 *   <li>Una cuenta raíz define tipo y naturaleza; las subcuentas los heredan del padre.</li>
 *   <li>Solo se cuelgan subcuentas de una cuenta activa y sin asientos; al recibir hijos, el
 *       padre pasa a ser de agrupación (no permite movimiento).</li>
 *   <li>Código, padre, tipo y naturaleza no se modifican.</li>
 *   <li>Nunca se borra (las FK son ON DELETE CASCADE y arrastrarían asientos): se desactiva.
 *       No se desactiva con subcuentas activas ni si la usa la integración contable de ventas.</li>
 * </ul>
 */
@Service
public class PlanCuentasService {

    private static final String ENTIDAD = "cuentas_contables";

    private final PlanCuentasRepositorio cuentas;
    private final PlanCuentasMapper mapper;
    private final BitacoraTenantAuditoriaService bitacora;

    public PlanCuentasService(PlanCuentasRepositorio cuentas, PlanCuentasMapper mapper,
                              BitacoraTenantAuditoriaService bitacora) {
        this.cuentas = cuentas;
        this.mapper = mapper;
        this.bitacora = bitacora;
    }

    // ------------------------------------------------------------- consultas --

    @Transactional(readOnly = true)
    public List<CuentaNodoResponse> arbol() {
        return mapper.aArbol(cuentas.findAllByOrderByCodigoAsc(), idsConMovimientos());
    }

    @Transactional(readOnly = true)
    public Page<CuentaResponse> buscar(String texto, String tipo, Boolean activo, boolean soloMovimiento,
                                       int pagina, int tamanio) {
        PageRequest orden = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, "codigo"));
        String tipoNormal = ReglasPlanCuentas.mayusculas(tipo);
        return cuentas.buscar(texto == null ? "" : texto.trim(), tipoNormal == null ? "" : tipoNormal,
                        activo, soloMovimiento, orden)
                .map(this::respuesta);
    }

    @Transactional(readOnly = true)
    public CuentaResponse obtener(Long id) {
        return respuesta(cargar(id));
    }

    // ------------------------------------------------------------ operaciones --

    @Transactional
    public CuentaResponse crear(CuentaRequest peticion, Solicitante quien) {
        String codigo = peticion.codigo().trim();
        if (!ReglasPlanCuentas.codigoValido(codigo)) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El código solo admite números separados por puntos (ej. 1.1.1.01).");
        }
        if (cuentas.existsByCodigo(codigo)) {
            throw rechazo(HttpStatus.CONFLICT, "Ya existe una cuenta con el código " + codigo + ".");
        }

        CuentaContable nueva = new CuentaContable();
        nueva.setCodigo(codigo);
        nueva.setNombre(peticion.nombre().trim());
        nueva.setNivel(ReglasPlanCuentas.nivel(codigo));
        nueva.setPermiteMovimiento(peticion.permiteMovimiento() == null || peticion.permiteMovimiento());
        nueva.setActivo(true);

        if (peticion.padreId() == null) {
            if (ReglasPlanCuentas.nivel(codigo) != 1) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Una cuenta sin cuenta padre debe tener un código de un solo segmento (ej. 6).");
            }
            String tipo = ReglasPlanCuentas.mayusculas(peticion.tipo());
            if (tipo == null || !ReglasPlanCuentas.TIPOS.contains(tipo)) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El tipo debe ser uno de " + ReglasPlanCuentas.TIPOS + ".");
            }
            String naturaleza = ReglasPlanCuentas.mayusculas(peticion.naturaleza());
            if (naturaleza == null) {
                naturaleza = ReglasPlanCuentas.naturalezaPorDefecto(tipo);
            } else if (!ReglasPlanCuentas.NATURALEZAS.contains(naturaleza)) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "La naturaleza debe ser DEUDORA o ACREEDORA.");
            }
            nueva.setTipo(tipo);
            nueva.setNaturaleza(naturaleza);
        } else {
            CuentaContable padre = cuentas.bloquear(peticion.padreId())
                    .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "La cuenta padre no existe."));
            if (!Boolean.TRUE.equals(padre.getActivo())) {
                throw rechazo(HttpStatus.CONFLICT, "La cuenta padre " + padre.getCodigo() + " está inactiva.");
            }
            if (!ReglasPlanCuentas.esHijoDirecto(padre.getCodigo(), codigo)) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "El código de la subcuenta debe ser "
                        + padre.getCodigo() + ".<n> (un segmento más que su cuenta padre).");
            }
            if (cuentas.tieneMovimientos(padre.getId())) {
                throw rechazo(HttpStatus.CONFLICT, "La cuenta " + padre.getCodigo()
                        + " ya tiene asientos registrados; no se le pueden agregar subcuentas.");
            }
            nueva.setPadreId(padre.getId());
            nueva.setTipo(padre.getTipo());
            nueva.setNaturaleza(padre.getNaturaleza());
            if (Boolean.TRUE.equals(padre.getPermiteMovimiento())) {
                padre.setPermiteMovimiento(false);
                cuentas.save(padre);
            }
        }

        CuentaContable guardada = cuentas.saveAndFlush(nueva);
        bitacora.registrar("CUENTA_CREADA", quien.actor(), ENTIDAD, guardada.getId(), quien.ip());
        return respuesta(guardada);
    }

    @Transactional
    public CuentaResponse actualizar(Long id, CuentaActualizarRequest peticion, Solicitante quien) {
        CuentaContable cuenta = cargarParaCambiar(id);
        if (peticion.permiteMovimiento() && cuentas.existsByPadreId(id)) {
            throw rechazo(HttpStatus.CONFLICT,
                    "La cuenta " + cuenta.getCodigo() + " tiene subcuentas; solo puede ser de agrupación.");
        }
        if (!peticion.permiteMovimiento() && cuentas.tieneMovimientos(id)) {
            throw rechazo(HttpStatus.CONFLICT, "La cuenta " + cuenta.getCodigo()
                    + " ya tiene asientos registrados; debe seguir permitiendo movimiento.");
        }
        cuenta.setNombre(peticion.nombre().trim());
        cuenta.setPermiteMovimiento(peticion.permiteMovimiento());
        CuentaContable guardada = cuentas.saveAndFlush(cuenta);
        bitacora.registrar("CUENTA_MODIFICADA", quien.actor(), ENTIDAD, id, quien.ip());
        return respuesta(guardada);
    }

    @Transactional
    public CuentaResponse desactivar(Long id, Solicitante quien) {
        CuentaContable cuenta = cargarParaCambiar(id);
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw rechazo(HttpStatus.CONFLICT, "La cuenta " + cuenta.getCodigo() + " ya está inactiva.");
        }
        if (ReglasPlanCuentas.esCuentaIntegracion(cuenta.getCodigo())) {
            throw rechazo(HttpStatus.CONFLICT, "La cuenta " + cuenta.getCodigo()
                    + " la usa la contabilización automática de ventas o compras; no se puede desactivar.");
        }
        if (cuentas.existsByPadreIdAndActivoTrue(id)) {
            throw rechazo(HttpStatus.CONFLICT,
                    "Primero desactive las subcuentas activas de " + cuenta.getCodigo() + ".");
        }
        cuenta.setActivo(false);
        CuentaContable guardada = cuentas.saveAndFlush(cuenta);
        bitacora.registrar("CUENTA_DESACTIVADA", quien.actor(), ENTIDAD, id, quien.ip());
        return respuesta(guardada);
    }

    @Transactional
    public CuentaResponse reactivar(Long id, Solicitante quien) {
        CuentaContable cuenta = cargarParaCambiar(id);
        if (Boolean.TRUE.equals(cuenta.getActivo())) {
            throw rechazo(HttpStatus.CONFLICT, "La cuenta " + cuenta.getCodigo() + " ya está activa.");
        }
        if (cuenta.getPadreId() != null) {
            cuentas.findById(cuenta.getPadreId())
                    .filter(p -> !Boolean.TRUE.equals(p.getActivo()))
                    .ifPresent(p -> {
                        throw rechazo(HttpStatus.CONFLICT,
                                "Primero reactive la cuenta padre " + p.getCodigo() + ".");
                    });
        }
        cuenta.setActivo(true);
        CuentaContable guardada = cuentas.saveAndFlush(cuenta);
        bitacora.registrar("CUENTA_REACTIVADA", quien.actor(), ENTIDAD, id, quien.ip());
        return respuesta(guardada);
    }

    // --------------------------------------------------------------- apoyo --

    private CuentaResponse respuesta(CuentaContable cuenta) {
        String codigoPadre = cuenta.getPadreId() == null ? null
                : cuentas.findById(cuenta.getPadreId()).map(CuentaContable::getCodigo).orElse(null);
        return mapper.aRespuesta(cuenta, codigoPadre, cuentas.existsByPadreId(cuenta.getId()),
                cuentas.tieneMovimientos(cuenta.getId()));
    }

    private Set<Long> idsConMovimientos() {
        return cuentas.idsConMovimientos().stream().map(Number::longValue).collect(Collectors.toSet());
    }

    private CuentaContable cargar(Long id) {
        return cuentas.findById(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "La cuenta contable no existe."));
    }

    private CuentaContable cargarParaCambiar(Long id) {
        return cuentas.bloquear(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "La cuenta contable no existe."));
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
