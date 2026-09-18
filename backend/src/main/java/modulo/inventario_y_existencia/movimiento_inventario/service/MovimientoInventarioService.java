package modulo.inventario_y_existencia.movimiento_inventario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.inventario_y_existencia.movimiento_inventario.dto.*;
import modulo.inventario_y_existencia.movimiento_inventario.entity.Almacen;
import modulo.inventario_y_existencia.movimiento_inventario.entity.KardexMovimiento;
import modulo.inventario_y_existencia.movimiento_inventario.entity.Producto;
import modulo.inventario_y_existencia.movimiento_inventario.entity.StockAlmacen;
import modulo.inventario_y_existencia.movimiento_inventario.repository.AlmacenRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.KardexMovimientoRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.ProductoRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.StockAlmacenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovimientoInventarioService {

    private final KardexMovimientoRepository kardexRepository;
    private final StockAlmacenRepository stockRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;

    /**
     * HU-06: Registra de forma atómica e inmutable un movimiento de inventario (Kardex)
     * y actualiza las existencias disponibles en el almacén.
     */
    @Transactional
    public MovimientoInventarioResponse registrarMovimiento(MovimientoInventarioRequest request, String username) {
        log.info("Iniciando registro de movimiento de inventario: productoId={}, almacenId={}, tipo={}, cantidad={}",
                request.getProductoId(), request.getAlmacenId(), request.getTipoMovimiento(), request.getCantidad());

        // 1. Validar Producto
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Producto con ID " + request.getProductoId() + " no encontrado."));

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto '" + producto.getNombre() + "' se encuentra inactivo.");
        }

        // 2. Validar Almacén
        Almacen almacen = almacenRepository.findById(request.getAlmacenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Almacén con ID " + request.getAlmacenId() + " no encontrado."));

        if (!Boolean.TRUE.equals(almacen.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El almacén '" + almacen.getNombre() + "' se encuentra inactivo.");
        }

        // 3. Normalizar y Validar Tipo de Movimiento
        String tipoNorm = request.getTipoMovimiento().trim().toUpperCase();
        validarTipoMovimiento(tipoNorm);

        // 4. Validar Justificación Obligatoria para Ajustes
        if (tipoNorm.startsWith("AJUSTE")) {
            if (request.getMotivo() == null || request.getMotivo().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El motivo o justificación es obligatorio para registrar ajustes de inventario.");
            }
        }

        // 5. Obtener o Inicializar Existencia en Almacén
        StockAlmacen stock = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> StockAlmacen.builder()
                        .producto(producto)
                        .almacen(almacen)
                        .cantidadActual(BigDecimal.ZERO)
                        .build());

        BigDecimal stockAnterior = stock.getCantidadActual() != null ? stock.getCantidadActual() : BigDecimal.ZERO;
        BigDecimal cantidad = request.getCantidad();
        BigDecimal nuevoStock;
        String tipoEfectivo;

        // 6. Calcular Nuevo Saldo y Validar Stock Disponible
        if (tipoNorm.equals("ENTRADA") || tipoNorm.equals("AJUSTE_POSITIVO")) {
            nuevoStock = stockAnterior.add(cantidad);
            tipoEfectivo = tipoNorm.equals("AJUSTE") ? "AJUSTE_POSITIVO" : tipoNorm;
        } else if (tipoNorm.equals("SALIDA") || tipoNorm.equals("AJUSTE_NEGATIVO")) {
            if (stockAnterior.compareTo(cantidad) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Stock insuficiente en '%s'. Stock actual disponible: %s %s, cantidad requerida para la salida: %s %s.",
                                almacen.getNombre(), stockAnterior, producto.getUnidadMedida(), cantidad, producto.getUnidadMedida()));
            }
            nuevoStock = stockAnterior.subtract(cantidad);
            tipoEfectivo = tipoNorm.equals("AJUSTE") ? "AJUSTE_NEGATIVO" : tipoNorm;
        } else if (tipoNorm.equals("AJUSTE")) {
            nuevoStock = stockAnterior.add(cantidad);
            tipoEfectivo = "AJUSTE_POSITIVO";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de movimiento no soportado: " + tipoNorm);
        }

        // 7. Costo Unitario y Valoración del Saldo
        BigDecimal costoUnitario = (request.getCostoUnitario() != null && request.getCostoUnitario().compareTo(BigDecimal.ZERO) > 0)
                ? request.getCostoUnitario()
                : (producto.getCostoPromedio() != null ? producto.getCostoPromedio() : BigDecimal.ZERO);

        BigDecimal saldoValorado = nuevoStock.multiply(costoUnitario);

        // 8. Persistir Actualización de Stock
        stock.setCantidadActual(nuevoStock);
        stockRepository.save(stock);

        // 9. Registrar Movimiento Inmutable en Kardex
        LocalDateTime fechaMovimiento = request.getFecha() != null ? request.getFecha() : LocalDateTime.now();

        KardexMovimiento movimiento = KardexMovimiento.builder()
                .producto(producto)
                .almacen(almacen)
                .tipoMovimiento(tipoEfectivo)
                .cantidad(cantidad)
                .costoUnitario(costoUnitario)
                .saldoCantidad(nuevoStock)
                .saldoValorado(saldoValorado)
                .referenciaDoc(request.getReferenciaDoc())
                .motivo(request.getMotivo())
                .fecha(fechaMovimiento)
                .creadoPor(username != null ? username : "sistema")
                .build();

        KardexMovimiento guardado = kardexRepository.save(movimiento);
        log.info("Movimiento Kardex #{} registrado con éxito. Nuevo stock: {}", guardado.getId(), nuevoStock);

        return toMovimientoResponse(guardado);
    }

    /**
     * HU-06: Consulta paginada y filtrada del historial de movimientos (Kardex).
     */
    @Transactional(readOnly = true)
    public Page<MovimientoInventarioResponse> listarMovimientos(
            Long productoId,
            Integer almacenId,
            String tipoMovimiento,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String tipoFiltrado = (tipoMovimiento != null && !tipoMovimiento.isBlank()) ? tipoMovimiento.trim().toUpperCase() : null;
        return kardexRepository.buscarMovimientos(productoId, almacenId, tipoFiltrado, fechaInicio, fechaFin, pageable)
                .map(this::toMovimientoResponse);
    }

    /**
     * Consulta el detalle individual de un movimiento por su ID.
     */
    @Transactional(readOnly = true)
    public MovimientoInventarioResponse obtenerMovimientoPorId(Long id) {
        KardexMovimiento m = kardexRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimiento #" + id + " no encontrado."));
        return toMovimientoResponse(m);
    }

    /**
     * Administrar Stock por Almacén: Consulta paginada de existencias.
     */
    @Transactional(readOnly = true)
    public Page<StockAlmacenResponse> listarStock(Integer almacenId, Long productoId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.buscarStock(almacenId, productoId, search, pageable)
                .map(this::toStockResponse);
    }

    /**
     * Administrar Stock por Almacén: Consulta paginada para un almacén específico.
     */
    @Transactional(readOnly = true)
    public Page<StockAlmacenResponse> listarStockPorAlmacen(Integer almacenId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findByAlmacenId(almacenId, pageable)
                .map(this::toStockResponse);
    }

    /**
     * Alertas de Stock Mínimo:
     * Devuelve los productos cuyas existencias en almacén están en o por debajo del stock mínimo,
     * calculando el déficit para reposición prioritaria.
     */
    @Transactional(readOnly = true)
    public List<AlertaStockMinimoResponse> listarAlertasStockMinimo(Integer almacenId) {
        return stockRepository.findAlertasStockMinimo(almacenId).stream()
                .map(s -> {
                    BigDecimal actual = s.getCantidadActual() != null ? s.getCantidadActual() : BigDecimal.ZERO;
                    int minimo = s.getProducto().getStockMinimo() != null ? s.getProducto().getStockMinimo() : 0;
                    BigDecimal minDecimal = BigDecimal.valueOf(minimo);
                    BigDecimal deficit = minDecimal.subtract(actual);

                    String criticidad;
                    if (actual.compareTo(BigDecimal.ZERO) <= 0) {
                        criticidad = "AGOTADO";
                    } else if (actual.compareTo(minDecimal.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP)) <= 0) {
                        criticidad = "CRITICO";
                    } else {
                        criticidad = "ALERTA";
                    }

                    return AlertaStockMinimoResponse.builder()
                            .productoId(s.getProducto().getId())
                            .productoNombre(s.getProducto().getNombre())
                            .productoSku(s.getProducto().getCodigoSku())
                            .unidadMedida(s.getProducto().getUnidadMedida())
                            .almacenId(s.getAlmacen().getId())
                            .almacenNombre(s.getAlmacen().getNombre())
                            .cantidadActual(actual)
                            .stockMinimo(minimo)
                            .deficit(deficit.max(BigDecimal.ZERO))
                            .nivelCriticidad(criticidad)
                            .build();
                })
                .toList();
    }

    /**
     * Consulta de stock disponible requerida por otros módulos (Ventas, Facturación, Despachos).
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerStockDisponible(Long productoId, Integer almacenId) {
        return stockRepository.findByProductoIdAndAlmacenId(productoId, almacenId)
                .map(StockAlmacen::getCantidadActual)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Verificación rápida de disponibilidad.
     */
    @Transactional(readOnly = true)
    public boolean verificarStockSuficiente(Long productoId, Integer almacenId, BigDecimal cantidadRequerida) {
        BigDecimal disponible = obtenerStockDisponible(productoId, almacenId);
        return disponible.compareTo(cantidadRequerida) >= 0;
    }

    /**
     * Catálogo de productos activos para selección en formularios.
     */
    @Transactional(readOnly = true)
    public List<ProductoSimpleResponse> listarProductosActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(p -> ProductoSimpleResponse.builder()
                        .id(p.getId())
                        .nombre(p.getNombre())
                        .codigoSku(p.getCodigoSku())
                        .codigoBarra(p.getCodigoBarra())
                        .unidadMedida(p.getUnidadMedida())
                        .costoPromedio(p.getCostoPromedio())
                        .stockMinimo(p.getStockMinimo())
                        .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : "Sin Categoría")
                        .build())
                .toList();
    }

    /**
     * Catálogo de almacenes activos para selección en formularios.
     */
    @Transactional(readOnly = true)
    public List<AlmacenSimpleResponse> listarAlmacenesActivos() {
        return almacenRepository.findByActivoTrue().stream()
                .map(a -> AlmacenSimpleResponse.builder()
                        .id(a.getId())
                        .nombre(a.getNombre())
                        .direccion(a.getDireccion())
                        .esPrincipal(a.getEsPrincipal())
                        .build())
                .toList();
    }

    private void validarTipoMovimiento(String tipo) {
        List<String> permitidos = List.of("ENTRADA", "SALIDA", "AJUSTE", "AJUSTE_POSITIVO", "AJUSTE_NEGATIVO");
        if (!permitidos.contains(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de movimiento no válido: '" + tipo + "'. Los tipos permitidos son: ENTRADA, SALIDA, AJUSTE_POSITIVO, AJUSTE_NEGATIVO, AJUSTE.");
        }
    }

    private MovimientoInventarioResponse toMovimientoResponse(KardexMovimiento m) {
        return MovimientoInventarioResponse.builder()
                .id(m.getId())
                .productoId(m.getProducto().getId())
                .productoNombre(m.getProducto().getNombre())
                .productoSku(m.getProducto().getCodigoSku())
                .unidadMedida(m.getProducto().getUnidadMedida())
                .almacenId(m.getAlmacen().getId())
                .almacenNombre(m.getAlmacen().getNombre())
                .tipoMovimiento(m.getTipoMovimiento())
                .cantidad(m.getCantidad())
                .costoUnitario(m.getCostoUnitario())
                .saldoCantidad(m.getSaldoCantidad())
                .saldoValorado(m.getSaldoValorado())
                .referenciaDoc(m.getReferenciaDoc())
                .motivo(m.getMotivo())
                .fecha(m.getFecha())
                .creadoPor(m.getCreadoPor())
                .build();
    }

    private StockAlmacenResponse toStockResponse(StockAlmacen s) {
        BigDecimal actual = s.getCantidadActual() != null ? s.getCantidadActual() : BigDecimal.ZERO;
        int minimo = s.getProducto().getStockMinimo() != null ? s.getProducto().getStockMinimo() : 0;
        boolean bajoStock = actual.compareTo(BigDecimal.valueOf(minimo)) <= 0;

        return StockAlmacenResponse.builder()
                .id(s.getId())
                .productoId(s.getProducto().getId())
                .productoNombre(s.getProducto().getNombre())
                .productoSku(s.getProducto().getCodigoSku())
                .codigoBarra(s.getProducto().getCodigoBarra())
                .unidadMedida(s.getProducto().getUnidadMedida())
                .precioVenta(s.getProducto().getPrecioVenta())
                .costoPromedio(s.getProducto().getCostoPromedio())
                .stockMinimo(minimo)
                .almacenId(s.getAlmacen().getId())
                .almacenNombre(s.getAlmacen().getNombre())
                .cantidadActual(actual)
                .bajoStockMinimo(bajoStock)
                .build();
    }
}
