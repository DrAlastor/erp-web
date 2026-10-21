package modulo.compras_y_proveedores.cu21_gestionar_proveedores.service;

import modulo.compras_y_proveedores.compartido.service.AvisosCompras;
import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.HistorialProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorOpcionResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorRequest;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPago;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.mapper.ProveedorMapper;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.repository.ProveedorRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * CU21 - Gestionar Proveedores.
 *
 * <p>Reglas que aplica (las mismas de la HU del Sprint Especial):
 * <ul>
 *   <li>El NIT/CI es único: si ya existe, se informa y no se guarda nada (409).</li>
 *   <li>CONTADO implica 0 días de crédito; CREDITO exige días mayores a cero.</li>
 *   <li>Nunca se borra: se desactiva con un motivo y puede reactivarse.</li>
 *   <li>No se desactiva con órdenes PENDIENTE, APROBADA o ENVIADA, ni con saldo por pagar.</li>
 *   <li>Un proveedor inactivo no se ofrece en órdenes ni compras nuevas.</li>
 *   <li>Alta, modificación, desactivación y reactivación quedan en la bitácora.</li>
 * </ul>
 */
@Service
public class ProveedorService {

    private static final String ENTIDAD = "proveedores";

    private final ProveedorRepository proveedores;
    private final ProveedorMapper mapper;
    private final AvisosCompras avisos;

    public ProveedorService(ProveedorRepository proveedores, ProveedorMapper mapper, AvisosCompras avisos) {
        this.proveedores = proveedores;
        this.mapper = mapper;
        this.avisos = avisos;
    }

    // ------------------------------------------------------------- consultas --

    @Transactional(readOnly = true)
    public Page<ProveedorResponse> buscar(String texto, Boolean activo, String ciudad, int pagina, int tamanio) {
        PageRequest orden = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, "razonSocial"));
        return proveedores.buscar(limpio(texto), activo, limpio(ciudad), orden).map(mapper::aRespuesta);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Integer id) {
        return mapper.aRespuesta(cargar(id));
    }

    @Transactional(readOnly = true)
    public List<ProveedorOpcionResponse> activos() {
        return proveedores.findByActivoTrueOrderByRazonSocialAsc().stream().map(mapper::aOpcion).toList();
    }

    @Transactional(readOnly = true)
    public List<String> ciudades() {
        return proveedores.ciudades();
    }

    @Transactional(readOnly = true)
    public HistorialProveedorResponse historial(Integer id) {
        Proveedor proveedor = cargar(id);
        return new HistorialProveedorResponse(
                mapper.aRespuesta(proveedor),
                proveedores.ultimasOrdenes(id).stream().map(mapper::aDocumento).toList(),
                proveedores.ultimasCompras(id).stream().map(mapper::aDocumento).toList(),
                saldoDe(id),
                proveedores.contarOrdenesAbiertas(id));
    }

    // ------------------------------------------------------------ operaciones --

    @Transactional
    public ProveedorResponse registrar(ProveedorRequest peticion, Solicitante quien) {
        String nit = peticion.nitCi().trim();
        if (proveedores.existsByNitCi(nit)) {
            throw rechazo(HttpStatus.CONFLICT, "Ya existe un proveedor con el NIT/CI " + nit + ".");
        }
        Proveedor proveedor = new Proveedor();
        aplicar(proveedor, peticion);
        proveedor.setActivo(true);
        Proveedor guardado = proveedores.saveAndFlush(proveedor);
        avisos.registrar("PROVEEDOR_CREADO", quien, ENTIDAD, guardado.getId());
        return mapper.aRespuesta(guardado);
    }

    @Transactional
    public ProveedorResponse modificar(Integer id, ProveedorRequest peticion, Solicitante quien) {
        Proveedor proveedor = cargarParaCambiar(id);
        String nit = peticion.nitCi().trim();
        if (proveedores.existsByNitCiAndIdNot(nit, id)) {
            throw rechazo(HttpStatus.CONFLICT, "Ya existe otro proveedor con el NIT/CI " + nit + ".");
        }
        aplicar(proveedor, peticion);
        Proveedor guardado = proveedores.saveAndFlush(proveedor);
        avisos.registrar("PROVEEDOR_MODIFICADO", quien, ENTIDAD, id);
        return mapper.aRespuesta(guardado);
    }

    @Transactional
    public ProveedorResponse desactivar(Integer id, String motivo, Solicitante quien) {
        Proveedor proveedor = cargarParaCambiar(id);
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw rechazo(HttpStatus.CONFLICT, "El proveedor " + proveedor.getRazonSocial() + " ya está inactivo.");
        }
        long abiertas = proveedores.contarOrdenesAbiertas(id);
        if (abiertas > 0) {
            throw rechazo(HttpStatus.CONFLICT, "No se puede desactivar a " + proveedor.getRazonSocial()
                    + ": tiene " + abiertas + " orden(es) de compra abiertas (PENDIENTE, APROBADA o ENVIADA).");
        }
        BigDecimal saldo = saldoDe(id);
        if (saldo.signum() > 0) {
            throw rechazo(HttpStatus.CONFLICT, "No se puede desactivar a " + proveedor.getRazonSocial()
                    + ": todavía se le deben Bs " + saldo + " en cuentas por pagar.");
        }
        proveedor.desactivar();
        avisos.registrar("PROVEEDOR_DESACTIVADO", quien, ENTIDAD, id);
        // La tabla no tiene columna para el motivo: queda en el aviso a los administradores.
        avisos.avisarAdministradores("Proveedor desactivado",
                proveedor.getRazonSocial() + " (NIT " + proveedor.getNitCi() + ") fue desactivado por "
                        + quien.actor() + ". Motivo: " + motivo.trim(), null);
        return mapper.aRespuesta(proveedor);
    }

    @Transactional
    public ProveedorResponse reactivar(Integer id, Solicitante quien) {
        Proveedor proveedor = cargarParaCambiar(id);
        if (Boolean.TRUE.equals(proveedor.getActivo())) {
            throw rechazo(HttpStatus.CONFLICT, "El proveedor " + proveedor.getRazonSocial() + " ya está activo.");
        }
        proveedor.reactivar();
        avisos.registrar("PROVEEDOR_REACTIVADO", quien, ENTIDAD, id);
        return mapper.aRespuesta(proveedor);
    }

    // ---------------------------------------------------------------- reglas --

    /** Vuelca la petición en la entidad aplicando la regla de condición de pago. */
    private void aplicar(Proveedor proveedor, ProveedorRequest peticion) {
        int dias = peticion.diasCredito() == null ? 0 : peticion.diasCredito();
        if (peticion.condicionPago() == CondicionPago.CREDITO && dias <= 0) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Un proveedor a CREDITO necesita días de crédito mayores a cero (por ejemplo 15 o 30).");
        }
        if (peticion.condicionPago() == CondicionPago.CONTADO) {
            dias = 0;
        }
        proveedor.setRazonSocial(peticion.razonSocial().trim());
        proveedor.setNitCi(peticion.nitCi().trim());
        proveedor.setContacto(opcional(peticion.contacto()));
        proveedor.setTelefono(opcional(peticion.telefono()));
        proveedor.setEmail(opcional(peticion.email()) == null ? null : peticion.email().trim().toLowerCase());
        proveedor.setDireccion(opcional(peticion.direccion()));
        proveedor.setCiudad(opcional(peticion.ciudad()));
        proveedor.setCondicionPago(peticion.condicionPago());
        proveedor.setDiasCredito(dias);
    }

    private BigDecimal saldoDe(Integer id) {
        BigDecimal saldo = proveedores.saldoPendiente(id);
        return saldo == null ? BigDecimal.ZERO : saldo;
    }

    /** Lo bloquea para escribir: desactivar mientras se emite una orden espera su turno. */
    private Proveedor cargarParaCambiar(Integer id) {
        return proveedores.bloquear(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "El proveedor " + id + " no existe."));
    }

    private Proveedor cargar(Integer id) {
        return proveedores.findById(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "El proveedor " + id + " no existe."));
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String motivo) {
        return new ResponseStatusException(estado, motivo);
    }
}
