package modulo.compras_y_proveedores.cu23_ordenes_de_compra.service;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.compras_y_proveedores.compartido.service.AvisosCompras;
import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.mapper.ProveedorMapper;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.repository.ProveedorRepository;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.ItemOrdenCompraRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OpcionesOrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResumenResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.SugerenciaReposicionResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.EstadoOrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompraDetalle;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.mapper.OrdenCompraMapper;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.repository.OrdenCompraRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionar Órdenes de Compra (CU22 en el documento del Sprint Especial; carpeta cu23 del repo).
 *
 * <p>Reglas que aplica (las mismas de la HU):
 * <ul>
 *   <li>Sólo a proveedores activos, verificado al registrar y otra vez al aprobar.</li>
 *   <li>Cantidades y costos mayores a cero; total = Σ subtotales − descuento, y el
 *       descuento no supera el subtotal.</li>
 *   <li>Número correlativo único OC-AAAA-NNNN, por año.</li>
 *   <li>Queda PENDIENTE hasta que el Administrador la apruebe o la rechace; quien la
 *       registró no puede aprobarla. Una rechazada vuelve a BORRADOR con el motivo.</li>
 *   <li>Sólo se modifica en BORRADOR o PENDIENTE; APROBADA o ENVIADA se anula y se emite
 *       otra. Una RECIBIDA no se anula.</li>
 *   <li>No mueve stock ni genera deuda: eso es de la compra (CU23/CU24).</li>
 *   <li>Cada cambio de estado queda en la bitácora y se avisa al responsable.</li>
 * </ul>
 */
@Service
public class OrdenCompraService {

    private static final String ENTIDAD = "ordenes_compra";
    private static final LocalDate DESDE_SIEMPRE = LocalDate.of(1900, 1, 1);
    private static final LocalDate HASTA_SIEMPRE = LocalDate.of(9999, 12, 31);

    private final OrdenCompraRepository ordenes;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final AlmacenRepository sucursales;
    private final UsuarioRepository usuarios;
    private final OrdenCompraMapper mapper;
    private final ProveedorMapper proveedorMapper;
    private final AvisosCompras avisos;
    private final Clock reloj;

    @Autowired
    public OrdenCompraService(OrdenCompraRepository ordenes,
                              ProveedorRepository proveedores,
                              ProductoRepository productos,
                              AlmacenRepository sucursales,
                              UsuarioRepository usuarios,
                              OrdenCompraMapper mapper,
                              ProveedorMapper proveedorMapper,
                              AvisosCompras avisos) {
        this(ordenes, proveedores, productos, sucursales, usuarios, mapper, proveedorMapper, avisos,
                Clock.systemDefaultZone());
    }

    OrdenCompraService(OrdenCompraRepository ordenes,
                       ProveedorRepository proveedores,
                       ProductoRepository productos,
                       AlmacenRepository sucursales,
                       UsuarioRepository usuarios,
                       OrdenCompraMapper mapper,
                       ProveedorMapper proveedorMapper,
                       AvisosCompras avisos,
                       Clock reloj) {
        this.ordenes = ordenes;
        this.proveedores = proveedores;
        this.productos = productos;
        this.sucursales = sucursales;
        this.usuarios = usuarios;
        this.mapper = mapper;
        this.proveedorMapper = proveedorMapper;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------- consultas --

    @Transactional(readOnly = true)
    public Page<OrdenCompraResumenResponse> listar(String texto, EstadoOrdenCompra estado, Integer proveedorId,
                                                   LocalDate desde, LocalDate hasta, int pagina, int tamanio) {
        PageRequest orden = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "id"));
        return ordenes.buscar(texto == null ? "" : texto.trim(), estado, proveedorId,
                        desde == null ? DESDE_SIEMPRE : desde,
                        hasta == null ? HASTA_SIEMPRE : hasta, orden)
                .map(mapper::aResumen);
    }

    @Transactional(readOnly = true)
    public OpcionesOrdenCompraResponse opciones() {
        return new OpcionesOrdenCompraResponse(
                proveedores.findByActivoTrueOrderByRazonSocialAsc().stream().map(proveedorMapper::aOpcion).toList(),
                ordenes.productosConUltimoCosto().stream().map(mapper::aProducto).toList(),
                sucursales.findByActivoTrue().stream()
                        .map(s -> new OpcionesOrdenCompraResponse.SucursalOpcion(s.getId(), s.getNombre()))
                        .toList());
    }

    /** Productos de la sucursal por debajo del stock mínimo, con su último costo de compra. */
    @Transactional(readOnly = true)
    public List<SugerenciaReposicionResponse> sugerencias(Integer sucursalId) {
        return ordenes.bajoStockMinimo(sucursalId).stream().map(mapper::aSugerencia).toList();
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse detalle(Integer id, Long usuarioActual) {
        return responder(cargar(id), usuarioActual);
    }

    // ------------------------------------------------------------ operaciones --

    @Transactional
    public OrdenCompraResponse registrar(OrdenCompraRequest peticion, Solicitante quien) {
        OrdenCompra orden = new OrdenCompra();
        orden.setCreadoPor(quien.usuarioId());
        orden.setFecha(hoy());
        aplicar(orden, peticion);
        orden.setEstado(Boolean.TRUE.equals(peticion.borrador())
                ? EstadoOrdenCompra.BORRADOR
                : EstadoOrdenCompra.PENDIENTE);
        orden.setNumeroOrden(siguienteNumero());

        OrdenCompra guardada = ordenes.saveAndFlush(orden);
        avisos.registrar("ORDEN_CREADA", quien, ENTIDAD, guardada.getId());
        if (guardada.getEstado() == EstadoOrdenCompra.PENDIENTE) {
            avisarPorAprobar(guardada, quien);
        }
        return responder(guardada, quien.usuarioId());
    }

    @Transactional
    public OrdenCompraResponse modificar(Integer id, OrdenCompraRequest peticion, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        if (!EstadoOrdenCompra.MODIFICABLES.contains(orden.getEstado())) {
            throw rechazo(HttpStatus.CONFLICT, "La orden " + orden.getNumeroOrden() + " está " + orden.getEstado()
                    + " y ya no se puede modificar: anúlala y emite otra.");
        }
        EstadoOrdenCompra antes = orden.getEstado();
        orden.getDetalles().clear();
        aplicar(orden, peticion);
        if (antes == EstadoOrdenCompra.BORRADOR && Boolean.FALSE.equals(peticion.borrador())) {
            orden.setEstado(EstadoOrdenCompra.PENDIENTE);
        }
        OrdenCompra guardada = ordenes.saveAndFlush(orden);
        avisos.registrar("ORDEN_MODIFICADA", quien, ENTIDAD, id);
        if (antes == EstadoOrdenCompra.BORRADOR && guardada.getEstado() == EstadoOrdenCompra.PENDIENTE) {
            avisarPorAprobar(guardada, quien);
        }
        return responder(guardada, quien.usuarioId());
    }

    /** Manda a aprobación un borrador (por ejemplo, una orden rechazada ya corregida). */
    @Transactional
    public OrdenCompraResponse solicitarAprobacion(Integer id, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        exigirEstado(orden, EstadoOrdenCompra.BORRADOR, "enviar a aprobación");
        validarProveedorActivo(orden.getProveedor());
        orden.setEstado(EstadoOrdenCompra.PENDIENTE);
        avisos.registrar("ORDEN_POR_APROBAR", quien, ENTIDAD, id);
        avisarPorAprobar(orden, quien);
        return responder(orden, quien.usuarioId());
    }

    @Transactional
    public OrdenCompraResponse aprobar(Integer id, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        exigirEstado(orden, EstadoOrdenCompra.PENDIENTE, "aprobar");
        if (orden.getCreadoPor() != null && orden.getCreadoPor().equals(quien.usuarioId())) {
            throw rechazo(HttpStatus.CONFLICT, "No puedes aprobar la orden " + orden.getNumeroOrden()
                    + " porque la registraste tú: la aprueba otro Administrador (separación de funciones).");
        }
        validarProveedorActivo(orden.getProveedor());
        orden.setEstado(EstadoOrdenCompra.APROBADA);
        orden.anotar("Aprobada por " + quien.actor() + " el " + hoy());
        avisos.registrar("ORDEN_APROBADA", quien, ENTIDAD, id);
        avisos.avisarUsuario(orden.getCreadoPor(), "Orden de compra aprobada",
                "La orden " + orden.getNumeroOrden() + " a " + orden.getProveedor().getRazonSocial()
                        + " fue aprobada por " + quien.actor() + ".");
        return responder(orden, quien.usuarioId());
    }

    @Transactional
    public OrdenCompraResponse rechazar(Integer id, String motivo, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        exigirEstado(orden, EstadoOrdenCompra.PENDIENTE, "rechazar");
        orden.setEstado(EstadoOrdenCompra.BORRADOR);
        orden.anotar("Rechazada por " + quien.actor() + " el " + hoy() + ": " + motivo.trim());
        avisos.registrar("ORDEN_RECHAZADA", quien, ENTIDAD, id);
        avisos.avisarUsuario(orden.getCreadoPor(), "Orden de compra rechazada",
                "La orden " + orden.getNumeroOrden() + " volvió a borrador para corregirla. Motivo: " + motivo.trim());
        return responder(orden, quien.usuarioId());
    }

    @Transactional
    public OrdenCompraResponse enviar(Integer id, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        exigirEstado(orden, EstadoOrdenCompra.APROBADA, "marcar como enviada");
        orden.setEstado(EstadoOrdenCompra.ENVIADA);
        orden.anotar("Enviada al proveedor por " + quien.actor() + " el " + hoy());
        avisos.registrar("ORDEN_ENVIADA", quien, ENTIDAD, id);
        avisos.avisarUsuario(orden.getCreadoPor(), "Orden de compra enviada",
                "La orden " + orden.getNumeroOrden() + " se envió a " + orden.getProveedor().getRazonSocial() + ".");
        return responder(orden, quien.usuarioId());
    }

    @Transactional
    public OrdenCompraResponse anular(Integer id, String motivo, Solicitante quien) {
        OrdenCompra orden = cargar(id);
        if (orden.getEstado() == EstadoOrdenCompra.RECIBIDA || orden.getEstado() == EstadoOrdenCompra.ANULADA) {
            throw rechazo(HttpStatus.CONFLICT, "La orden " + orden.getNumeroOrden() + " está "
                    + orden.getEstado() + " y no se puede anular.");
        }
        orden.setEstado(EstadoOrdenCompra.ANULADA);
        orden.anotar("Anulada por " + quien.actor() + " el " + hoy() + ": " + motivo.trim());
        avisos.registrar("ORDEN_ANULADA", quien, ENTIDAD, id);
        avisos.avisarUsuario(orden.getCreadoPor(), "Orden de compra anulada",
                "La orden " + orden.getNumeroOrden() + " fue anulada. Motivo: " + motivo.trim());
        return responder(orden, quien.usuarioId());
    }

    // ---------------------------------------------------------------- reglas --

    /** Vuelca proveedor, renglones y descuento de la petición y recalcula los importes. */
    private void aplicar(OrdenCompra orden, OrdenCompraRequest peticion) {
        Proveedor proveedor = proveedores.findById(peticion.proveedorId())
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND,
                        "El proveedor " + peticion.proveedorId() + " no existe."));
        validarProveedorActivo(proveedor);
        if (peticion.fechaEntrega() != null && peticion.fechaEntrega().isBefore(hoy())) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "La fecha de entrega no puede ser anterior a hoy.");
        }
        orden.setProveedor(proveedor);
        orden.setFechaEntrega(peticion.fechaEntrega());
        orden.setObservaciones(peticion.observaciones() == null || peticion.observaciones().isBlank()
                ? orden.getObservaciones()
                : peticion.observaciones().trim());

        Set<Long> vistos = new HashSet<>();
        for (ItemOrdenCompraRequest item : peticion.items()) {
            if (!vistos.add(item.productoId())) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El producto " + item.productoId() + " está repetido: suma las cantidades en un solo renglón.");
            }
            Producto producto = productos.findById(item.productoId())
                    .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND,
                            "El producto " + item.productoId() + " no existe."));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, "El producto " + producto.getNombre() + " está inactivo.");
            }
            OrdenCompraDetalle renglon = new OrdenCompraDetalle();
            renglon.setProducto(producto);
            renglon.setCantidad(item.cantidad().setScale(2, RoundingMode.HALF_UP));
            renglon.setCostoUnitario(item.costoUnitario().setScale(2, RoundingMode.HALF_UP));
            orden.agregarDetalle(renglon);
        }

        BigDecimal descuento = peticion.descuento() == null ? BigDecimal.ZERO : peticion.descuento();
        orden.calcularTotales(descuento);
        if (orden.getDescuento().compareTo(orden.getSubtotal()) > 0) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY, String.format(
                    "El descuento (Bs %s) no puede superar el subtotal de la orden (Bs %s).",
                    orden.getDescuento(), orden.getSubtotal()));
        }
    }

    private void validarProveedorActivo(Proveedor proveedor) {
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw rechazo(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El proveedor " + proveedor.getRazonSocial() + " está inactivo y no se le pueden emitir órdenes.");
        }
    }

    private void exigirEstado(OrdenCompra orden, EstadoOrdenCompra esperado, String accion) {
        if (orden.getEstado() != esperado) {
            throw rechazo(HttpStatus.CONFLICT, "Para " + accion + " la orden " + orden.getNumeroOrden()
                    + " debe estar " + esperado + ", y está " + orden.getEstado() + ".");
        }
    }

    /** OC-AAAA-NNNN: el correlativo vuelve a 1 cada año. */
    private String siguienteNumero() {
        ordenes.bloquearNumeracion();
        String prefijo = "OC-" + hoy().getYear() + "-";
        Integer ultimo = ordenes.ultimoCorrelativo(prefijo);
        return prefijo + String.format("%04d", (ultimo == null ? 0 : ultimo) + 1);
    }

    private void avisarPorAprobar(OrdenCompra orden, Solicitante quien) {
        avisos.avisarAdministradores("Orden de compra por aprobar",
                "La orden " + orden.getNumeroOrden() + " a " + orden.getProveedor().getRazonSocial()
                        + " por Bs " + orden.getTotal() + " espera aprobación (registrada por " + quien.actor() + ").");
    }

    private OrdenCompra cargar(Integer id) {
        return ordenes.buscarCompleta(id)
                .orElseThrow(() -> rechazo(HttpStatus.NOT_FOUND, "La orden de compra " + id + " no existe."));
    }

    private OrdenCompraResponse responder(OrdenCompra orden, Long usuarioActual) {
        String creador = orden.getCreadoPor() == null
                ? null
                : usuarios.findById(orden.getCreadoPor())
                        .map(u -> u.getEmail())
                        .orElse("Usuario #" + orden.getCreadoPor());
        return mapper.aRespuesta(orden, creador, usuarioActual);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static ResponseStatusException rechazo(HttpStatus estado, String motivo) {
        return new ResponseStatusException(estado, motivo);
    }
}
