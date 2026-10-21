package modulo.catalogo_e_inventario.cu12_consultar_kardex.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura que todos los productos registrados en el catálogo cuenten con su registro de inventario inicial
 * en kardex_movimientos para la sucursal principal, permitiendo consultar el Kardex valorado de cualquier artículo.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-initializers.enabled", havingValue = "true")
@Order(145)
public class InicializadorKardex implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorKardex.class);

    private final JdbcTemplate jdbc;

    public InicializadorKardex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Asegurar costos de compra válidos en todos los productos (75% del precio de venta si no tiene)
            jdbc.execute("""
                UPDATE PRODUCTOS 
                SET COSTO_COMPRA = CASE 
                    WHEN PRECIO_VENTA IS NOT NULL AND PRECIO_VENTA > 0 THEN ROUND(PRECIO_VENTA * 0.75, 2) 
                    ELSE 150.00 
                END
                WHERE COSTO_COMPRA IS NULL OR COSTO_COMPRA <= 0
            """);

            // 2. Asegurar stock mínimo calibrado en inventario_sucursal
            jdbc.execute("""
                UPDATE INVENTARIO_SUCURSAL s 
                SET STOCK_MINIMO = COALESCE(p.STOCK_MINIMO, 2.00)
                FROM PRODUCTOS p 
                WHERE s.PRODUCTO_ID = p.ID AND (s.STOCK_MINIMO IS NULL OR s.STOCK_MINIMO <= 0)
            """);

            // 3. Asegurar movimientos iniciales de apertura para productos que aún no tengan registros en kardex_movimientos
            jdbc.execute("""
                INSERT INTO KARDEX_MOVIMIENTOS (
                    SUCURSAL_ID, PRODUCTO_ID, UNIDAD_MEDIDA_ID, USUARIO_ID,
                    TIPO_MOVIMIENTO, MOTIVO, DOCUMENTO_REFERENCIA,
                    CANTIDAD, COSTO_UNITARIO, SALDO_CANTIDAD, SALDO_VALOR, FECHA_MOVIMIENTO
                )
                SELECT 
                    1 AS sucursal_id,
                    p.ID AS producto_id,
                    p.UNIDAD_MEDIDA_ID,
                    1 AS usuario_id,
                    'ENTRADA' AS tipo_movimiento,
                    'Inventario Inicial de Apertura' AS motivo,
                    'DOC-APERTURA-INI' AS documento_referencia,
                    COALESCE(s.STOCK_DISPONIBLE, p.STOCK_MINIMO, 10.00) AS cantidad,
                    COALESCE(p.COSTO_COMPRA, p.PRECIO_VENTA, 100.00) AS costo_unitario,
                    COALESCE(s.STOCK_DISPONIBLE, p.STOCK_MINIMO, 10.00) AS saldo_cantidad,
                    (COALESCE(s.STOCK_DISPONIBLE, p.STOCK_MINIMO, 10.00) * COALESCE(p.COSTO_COMPRA, p.PRECIO_VENTA, 100.00)) AS saldo_valor,
                    COALESCE(p.FECHA_CREACION, NOW()) AS fecha_movimiento
                FROM PRODUCTOS p
                LEFT JOIN INVENTARIO_SUCURSAL s ON s.PRODUCTO_ID = p.ID AND s.SUCURSAL_ID = 1
                WHERE NOT EXISTS (
                    SELECT 1 FROM KARDEX_MOVIMIENTOS km 
                    WHERE km.PRODUCTO_ID = p.ID AND km.SUCURSAL_ID = 1
                )
            """);

            // 4. Reparar registros de kardex con costo unitario o saldo valorado en 0 o nulo
            jdbc.execute("""
                UPDATE KARDEX_MOVIMIENTOS km
                SET COSTO_UNITARIO = COALESCE(p.COSTO_COMPRA, p.PRECIO_VENTA, 100.00),
                    SALDO_VALOR = km.SALDO_CANTIDAD * COALESCE(p.COSTO_COMPRA, p.PRECIO_VENTA, 100.00)
                FROM PRODUCTOS p
                WHERE km.PRODUCTO_ID = p.ID 
                  AND (km.COSTO_UNITARIO IS NULL OR km.COSTO_UNITARIO <= 0 OR km.SALDO_VALOR IS NULL OR km.SALDO_VALOR <= 0)
            """);

            log.info("Kardex de movimientos (CU12) y valoración de existencias inicializados y calibrados con éxito.");
        } catch (Exception e) {
            log.warn("No se pudo completar la verificación inicial de kardex_movimientos: {}", e.getMessage());
        }
    }
}
