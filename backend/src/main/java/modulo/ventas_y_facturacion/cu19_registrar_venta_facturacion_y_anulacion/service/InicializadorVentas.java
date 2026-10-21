package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Agrega a la tabla ventas las columnas que pide la CU19 y que el script ERP_Asus no trae:
 * el tipo de comprobante (factura o nota de venta) y el registro de la anulación (motivo,
 * fecha y usuario). Los cambios son aditivos e idempotentes: las ventas existentes quedan
 * como facturas.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-initializers.enabled", havingValue = "true")
@Order(130)
public class InicializadorVentas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorVentas.class);

    private final JdbcTemplate jdbc;

    public InicializadorVentas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tipo_comprobante VARCHAR(20) NOT NULL DEFAULT 'FACTURA'");
            jdbc.execute("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(200)");
            jdbc.execute("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP");
            jdbc.execute("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS anulado_por INTEGER");
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM clientes WHERE nit_ci = '0'", Integer.class);
            if (count == null || count == 0) {
                jdbc.execute("INSERT INTO clientes (razon_social, nit_ci, email, telefono, direccion, activo, limite_credito, dias_credito) " +
                             "VALUES ('Sin Nombre', '0', 'ventas@erp.com', '0', 'Venta en mostrador', true, 0, 0)");
                log.info("Cliente comodin 'Sin Nombre (NIT 0)' asegurado en BD.");
            }
            log.info("Tabla ventas lista para comprobantes y anulaciones de la CU19.");
        } catch (Exception e) {
            log.warn("No se pudieron preparar las columnas de la CU19 en ventas: {}", e.getMessage());
        }
    }
}
