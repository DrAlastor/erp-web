package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura la existencia de pedidos de prueba con estado PENDIENTE para la validación comercial (CU17).
 * Los inserts son idempotentes y no duplican información si los pedidos ya fueron cargados previamente.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-initializers.enabled", havingValue = "true")
@Order(140)
public class InicializadorPedidosValidacion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorPedidosValidacion.class);

    private final JdbcTemplate jdbc;

    public InicializadorPedidosValidacion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Insertar pedidos pendientes para la demo de validación comercial
            jdbc.execute("""
                INSERT INTO PEDIDOS (CLIENTE_ID, VENDEDOR_ID, SUCURSAL_ID, NUMERO_PEDIDO, FECHA, SUBTOTAL, DESCUENTO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES
                (1, 2, 1, 'PED-DEMO-0001', CURRENT_DATE, 17800.00,   0.00, 17800.00, 'PENDIENTE', 'Demo CU17: dentro del límite de crédito', NOW()),
                (2, 2, 1, 'PED-DEMO-0002', CURRENT_DATE, 1225000.00, 0.00, 1225000.00, 'PENDIENTE', 'Demo CU17: stock insuficiente', NOW()),
                (1, 2, 1, 'PED-DEMO-0003', CURRENT_DATE, 62300.00,   0.00, 62300.00, 'PENDIENTE', 'Demo CU17: supera el límite de crédito', NOW()),
                (3, 2, 1, 'PED-DEMO-0004', CURRENT_DATE, 2900.00,  435.00,  2465.00, 'PENDIENTE', 'Demo CU17: descuento sobre el tope', NOW())
                ON CONFLICT (NUMERO_PEDIDO) DO NOTHING
            """);

            // 2. Insertar detalles para los pedidos de prueba
            jdbc.execute("""
                INSERT INTO PEDIDO_DETALLES (PEDIDO_ID, PRODUCTO_ID, CANTIDAD, PRECIO_UNITARIO, DESCUENTO, SUBTOTAL)
                SELECT p.ID, d.producto_id, d.cantidad, d.precio, d.descuento, d.subtotal
                FROM PEDIDOS p
                JOIN (VALUES
                    ('PED-DEMO-0001', 6,  2, 8900.00,  0.00, 17800.00),
                    ('PED-DEMO-0002', 1, 50, 24500.00, 0.00, 1225000.00),
                    ('PED-DEMO-0003', 6,  7, 8900.00,  0.00, 62300.00),
                    ('PED-DEMO-0004', 7,  2, 1450.00, 435.00, 2465.00)
                ) AS d(numero, producto_id, cantidad, precio, descuento, subtotal) ON d.numero = p.NUMERO_PEDIDO
                WHERE NOT EXISTS (SELECT 1 FROM PEDIDO_DETALLES x WHERE x.PEDIDO_ID = p.ID)
            """);

            log.info("Pedidos de validación comercial (CU17) listos en base de datos.");
        } catch (Exception e) {
            log.warn("No se pudieron inicializar pedidos pendientes de prueba para CU17: {}", e.getMessage());
        }
    }
}
