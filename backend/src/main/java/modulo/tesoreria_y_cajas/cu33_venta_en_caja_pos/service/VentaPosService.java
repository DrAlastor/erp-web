package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.service;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosRequest;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosResponse;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.ventas_y_facturacion.compartido.repository.FormaPagoRepository;
import modulo.ventas_y_facturacion.compartido.repository.TurnoCajaRepository;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ClienteVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ProductoVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service.VentaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU33 - Venta en Caja (POS).
 *
 * <p>Toda venta queda ligada al turno abierto del cajero (CU32): sin turno no se vende. La
 * emisión del comprobante, el stock, los impuestos y el asiento contable los resuelve la
 * CU19 ({@link VentaService}); aquí solo se fijan las reglas propias de la caja: contado,
 * un único pago presencial y el cálculo del cambio.
 */
@Service
@RequiredArgsConstructor
public class VentaPosService {

    private final TurnoCajaRepository turnoRepository;
    private final FormaPagoRepository formaPagoRepository;
    private final ProductoRepository productoRepository;
    private final VentaService ventaService;

    @Transactional(readOnly = true)
    public List<ProductoVentaResponse> productos(String search) {
        return ventaService.productos(turnoAbierto().getSucursalId(), search);
    }

    @Transactional(readOnly = true)
    public List<ClienteVentaResponse> clientes(String search) {
        return ventaService.clientes(search);
    }

    @Transactional
    public VentaPosResponse vender(VentaPosRequest request) {
        TurnoCaja turno = turnoAbierto();
        FormaPago forma = formaPagoRepository.findById(request.formaPagoId())
                .filter(f -> Boolean.TRUE.equals(f.getActivo()))
                .orElseThrow(() -> rechazo("La forma de pago no existe o está inactiva."));
        if (forma.esPasarelaOnline()) {
            throw rechazo("En caja solo se cobra de forma presencial; el pago en línea se registra desde Facturación.");
        }

        Map<Long, BigDecimal> cantidades = new LinkedHashMap<>();
        for (VentaPosRequest.Linea linea : request.detalles()) {
            cantidades.merge(linea.productoId(), linea.cantidad(), BigDecimal::add);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(e.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto #" + e.getKey() + " no encontrado."));
            total = total.add(CalculadoraCobroPos.importeLinea(e.getValue(), producto.getPrecioVenta()));
        }

        BigDecimal cambio;
        BigDecimal recibido;
        if (forma.esEfectivo()) {
            try {
                cambio = CalculadoraCobroPos.cambio(total, request.montoRecibido());
            } catch (IllegalArgumentException ex) {
                throw rechazo(ex.getMessage());
            }
            recibido = total.add(cambio);
        } else {
            cambio = BigDecimal.ZERO;
            recibido = total;
        }

        // Al turno entra el total; el cambio entregado no forma parte del cobro.
        VentaResponse venta = ventaService.registrar(new VentaRequest(
                turno.getSucursalId(),
                request.clienteId(),
                turno.getId(),
                "CONTADO",
                request.comprobante(),
                null, null, null,
                cantidades.entrySet().stream()
                        .map(e -> new VentaRequest.Linea(e.getKey(), e.getValue(), null, null)).toList(),
                List.of(new VentaRequest.Pago(forma.getId(), total, null))));

        return new VentaPosResponse(venta.id(), venta.tipoComprobante(), venta.numeroFactura(), venta.fecha(),
                venta.turnoCajaId(), venta.razonSocialCliente(), venta.nitCliente(), forma.getNombre(),
                venta.total(), recibido, cambio);
    }

    private TurnoCaja turnoAbierto() {
        Long usuarioId = UsuarioActual.obtener().usuarioId();
        return turnoRepository.findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(usuarioId, TurnoCaja.ESTADO_ABIERTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No tienes un turno de caja abierto. Abre la caja antes de vender."));
    }

    private ResponseStatusException rechazo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
