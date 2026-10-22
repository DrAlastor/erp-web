package modulo.ventas_y_facturacion.compartido.service;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.StockAlmacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.KardexMovimientoRepository;
import modulo.ventas_y_facturacion.compartido.repository.StockVentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Efecto de las ventas sobre el inventario: actualiza inventario_sucursal y deja el
 * movimiento valorado en kardex_movimientos, igual que lo hace la CU11.
 *
 * <p>Se ejecuta siempre dentro de la transacción de la venta o de la nota de crédito
 * ({@link Propagation#MANDATORY}): si algo falla después, el stock no queda descontado.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InventarioVentaService {

    public static final String SALIDA = "SALIDA";
    public static final String ENTRADA = "ENTRADA";

    private final StockVentaRepository stockRepository;
    private final KardexMovimientoRepository kardexRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService bitacoraAuditoriaService;

    /** Descuenta stock de la sucursal; rechaza la operación si no alcanza. */
    public void registrarSalida(Almacen sucursal, Producto producto, BigDecimal cantidad,
                                Long usuarioId, String motivo, String documento) {
        StockAlmacen stock = stockRepository.bloquear(producto.getId(), sucursal.getId())
                .orElseThrow(() -> sinStock(sucursal, producto, BigDecimal.ZERO, cantidad));
        BigDecimal disponible = stock.getCantidadActual() == null ? BigDecimal.ZERO : stock.getCantidadActual();
        if (disponible.compareTo(cantidad) < 0) {
            throw sinStock(sucursal, producto, disponible, cantidad);
        }
        mover(stock, producto, sucursal, disponible.subtract(cantidad), cantidad, SALIDA, usuarioId, motivo, documento);
    }

    /** Devuelve stock a la sucursal (anulación o devolución). */
    public void registrarEntrada(Almacen sucursal, Producto producto, BigDecimal cantidad,
                                 Long usuarioId, String motivo, String documento) {
        StockAlmacen stock = stockRepository.bloquear(producto.getId(), sucursal.getId())
                .orElseGet(() -> StockAlmacen.builder()
                        .producto(producto)
                        .almacen(sucursal)
                        .cantidadActual(BigDecimal.ZERO)
                        .stockMinimo(producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO)
                        .build());
        BigDecimal disponible = stock.getCantidadActual() == null ? BigDecimal.ZERO : stock.getCantidadActual();
        mover(stock, producto, sucursal, disponible.add(cantidad), cantidad, ENTRADA, usuarioId, motivo, documento);
    }

    private void mover(StockAlmacen stock, Producto producto, Almacen sucursal, BigDecimal nuevoSaldo,
                       BigDecimal cantidad, String tipo, Long usuarioId, String motivo, String documento) {
        LocalDateTime ahora = LocalDateTime.now();
        stock.setCantidadActual(nuevoSaldo);
        stock.setUltimaActualizacion(ahora);
        stockRepository.save(stock);

        BigDecimal costo = producto.getCostoPromedio() != null ? producto.getCostoPromedio() : BigDecimal.ZERO;
        kardexRepository.save(KardexMovimiento.builder()
                .producto(producto)
                .almacen(sucursal)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .costoUnitario(costo)
                .saldoCantidad(nuevoSaldo)
                .saldoValorado(Montos.redondear(nuevoSaldo.multiply(costo)))
                .referenciaDoc(recortar(documento, 50))
                .motivo(recortar(motivo, 255))
                .fecha(ahora)
                .usuarioId(usuarioId.intValue())
                .unidadMedidaId(producto.getUnidadMedidaId() != null ? producto.getUnidadMedidaId() : 1)
                .build());

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "DESCUENTO_STOCK",
                    "INVENTARIO",
                    String.format("%s de stock por venta/operación: %s u. de '%s' (SKU: %s) en sucursal '%s'. Saldo: %s u.",
                            tipo, cantidad.stripTrailingZeros().toPlainString(),
                            producto.getNombre(), producto.getCodigoSku(), sucursal.getNombre(),
                            nuevoSaldo.stripTrailingZeros().toPlainString()),
                    "EXITO",
                    "BAJA",
                    String.format("{\"productoId\":%d,\"sku\":\"%s\",\"sucursalId\":%d,\"tipo\":\"%s\",\"cantidad\":%s,\"saldo\":%s,\"doc\":\"%s\"}",
                            producto.getId(), producto.getCodigoSku(), sucursal.getId(), tipo,
                            cantidad.stripTrailingZeros().toPlainString(), nuevoSaldo.stripTrailingZeros().toPlainString(),
                            documento != null ? documento : "")
            );
        } catch (Exception ignored) {}

        BigDecimal min = stock.getStockMinimo() != null ? stock.getStockMinimo() : BigDecimal.ZERO;
        BigDecimal prodMin = (producto.getStockMinimo() != null) ? producto.getStockMinimo() : BigDecimal.ZERO;
        BigDecimal umbralMinimo = min.compareTo(BigDecimal.ZERO) > 0 ? min : prodMin;

        if (umbralMinimo.compareTo(BigDecimal.ZERO) > 0 && nuevoSaldo.compareTo(umbralMinimo) <= 0) {
            String titulo = "Alerta de Stock Mínimo";
            String mensaje = String.format("El producto '%s' (%s) en la sucursal '%s' ha alcanzado o bajado de su stock mínimo de reposición (%s u). Saldo actual disponible: %s u.",
                    producto.getNombre(), producto.getCodigoSku(), sucursal.getNombre(),
                    umbralMinimo.stripTrailingZeros().toPlainString(), nuevoSaldo.stripTrailingZeros().toPlainString());
            try {
                jdbcTemplate.update(
                        "INSERT INTO NOTIFICACIONES (USUARIO_ID, TITULO, MENSAJE, TIPO, FECHA_CREACION, LEIDO) VALUES (?, ?, ?, ?, ?, ?)",
                        usuarioId != null ? usuarioId : 1L,
                        titulo,
                        mensaje,
                        "ALERTA",
                        ahora,
                        false
                );
                if (usuarioId != null && usuarioId != 1L) {
                    jdbcTemplate.update(
                            "INSERT INTO NOTIFICACIONES (USUARIO_ID, TITULO, MENSAJE, TIPO, FECHA_CREACION, LEIDO) VALUES (?, ?, ?, ?, ?, ?)",
                            1L,
                            titulo,
                            mensaje,
                            "ALERTA",
                            ahora,
                            false
                    );
                }
            } catch (Exception ignored) {
                // Previene interrumpir la transacción principal si la tabla no estuviera disponible
            }
        }
    }

    private static ResponseStatusException sinStock(Almacen sucursal, Producto producto,
                                                    BigDecimal disponible, BigDecimal requerido) {
        return new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                "Stock insuficiente de '%s' en %s: disponible %s, requerido %s.",
                producto.getNombre(), sucursal.getNombre(),
                disponible.stripTrailingZeros().toPlainString(), requerido.stripTrailingZeros().toPlainString()));
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
