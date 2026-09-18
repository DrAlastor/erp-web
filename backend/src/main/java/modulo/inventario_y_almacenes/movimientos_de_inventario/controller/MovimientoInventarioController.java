package modulo.inventario_y_almacenes.movimientos_de_inventario.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import modulo.inventario_y_almacenes.movimientos_de_inventario.dto.*;
import modulo.inventario_y_almacenes.movimientos_de_inventario.service.MovimientoInventarioService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    /**
     * HU-06: Registrar entradas, salidas y ajustes de inventario.
     * Genera el movimiento inmutable en el Kardex y actualiza las existencias disponibles.
     */
    @PostMapping("/movimientos")
    @PreAuthorize("hasPermission('INVENTARIO','CREAR')")
    public ResponseEntity<MovimientoInventarioResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoInventarioRequest request,
            Principal principal
    ) {
        String username = (principal != null) ? principal.getName() : "sistema";
        MovimientoInventarioResponse response = service.registrarMovimiento(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * HU-06: Consultar el historial de movimientos de inventario (Kardex).
     * Paginado y con filtros por producto, almacén, tipo de movimiento y rango de fechas.
     */
    @GetMapping("/movimientos")
    public ResponseEntity<Page<MovimientoInventarioResponse>> listarMovimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Integer almacenId,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(service.listarMovimientos(productoId, almacenId, tipoMovimiento, fechaInicio, fechaFin, page, size));
    }

    /**
     * HU-06: Consultar detalle de un movimiento específico por su ID.
     */
    @GetMapping("/movimientos/{id}")
    public ResponseEntity<MovimientoInventarioResponse> obtenerMovimientoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerMovimientoPorId(id));
    }

    /**
     * Administrar Stock por Almacén: Consulta general paginada de existencias.
     */
    @GetMapping("/stock")
    public ResponseEntity<Page<StockAlmacenResponse>> listarStock(
            @RequestParam(required = false) Integer almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size
    ) {
        Page<StockAlmacenResponse> stock = service.listarStock(almacenId, productoId, search, page, size);
        return ResponseEntity.ok(stock);
    }

    /**
     * Administrar Stock por Almacén: Consulta paginada de existencias para un almacén específico.
     */
    @GetMapping("/stock/almacen/{almacenId}")
    public ResponseEntity<Page<StockAlmacenResponse>> listarStockPorAlmacen(
            @PathVariable Integer almacenId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size
    ) {
        Page<StockAlmacenResponse> stock = service.listarStockPorAlmacen(almacenId, page, size);
        return ResponseEntity.ok(stock);
    }

    /**
     * Alertas de Stock Mínimo:
     * Retorna productos con existencias en o por debajo de su stock mínimo,
     * calculando el déficit y el nivel de criticidad (AGOTADO, CRITICO, ALERTA).
     */
    @GetMapping("/stock/alertas-minimo")
    public ResponseEntity<List<AlertaStockMinimoResponse>> listarAlertasStockMinimo(
            @RequestParam(required = false) Integer almacenId
    ) {
        List<AlertaStockMinimoResponse> alertas = service.listarAlertasStockMinimo(almacenId);
        return ResponseEntity.ok(alertas);
    }

    /**
     * Consulta rápida de stock disponible para integración con otros módulos (Ventas, Facturación, Despachos).
     */
    @GetMapping("/stock/disponible")
    public ResponseEntity<Map<String, Object>> consultarStockDisponible(
            @RequestParam Long productoId,
            @RequestParam Integer almacenId
    ) {
        BigDecimal disponible = service.obtenerStockDisponible(productoId, almacenId);
        return ResponseEntity.ok(Map.of(
                "productoId", productoId,
                "almacenId", almacenId,
                "stockDisponible", disponible
        ));
    }

    /**
     * Catálogo de productos activos (para combo/select en formularios).
     */
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoSimpleResponse>> listarProductos() {
        return ResponseEntity.ok(service.listarProductosActivos());
    }

    /**
     * Catálogo de almacenes activos (para combo/select en formularios).
     */
    @GetMapping("/almacenes")
    public ResponseEntity<List<AlmacenSimpleResponse>> listarAlmacenes() {
        return ResponseEntity.ok(service.listarAlmacenesActivos());
    }
}
