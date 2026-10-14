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
            // Asegurar movimientos iniciales de apertura para productos que aún no tengan registros en kardex_movimientos
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
                    p.COSTO_COMPRA AS costo_unitario,
                    COALESCE(s.STOCK_DISPONIBLE, p.STOCK_MINIMO, 10.00) AS saldo_cantidad,
                    (COALESCE(s.STOCK_DISPONIBLE, p.STOCK_MINIMO, 10.00) * p.COSTO_COMPRA) AS saldo_valor,
                    p.FECHA_CREACION AS fecha_movimiento
                FROM PRODUCTOS p
                LEFT JOIN INVENTARIO_SUCURSAL s ON s.PRODUCTO_ID = p.ID AND s.SUCURSAL_ID = 1
                WHERE NOT EXISTS (
                    SELECT 1 FROM KARDEX_MOVIMIENTOS km 
                    WHERE km.PRODUCTO_ID = p.ID AND km.SUCURSAL_ID = 1
                )
            """);

            log.info("Kardex de movimientos (CU12) verificado e inicializado para todos los productos.");
        } catch (Exception e) {
            log.warn("No se pudo completar la verificación inicial de kardex_movimientos: {}", e.getMessage());
        }
    }
}
