package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(120)
public class InicializadorDatosMaestrosInventario implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDatosMaestrosInventario.class);

    private final JdbcTemplate jdbc;

    @Autowired
    public InicializadorDatosMaestrosInventario(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Inicializando esquema de datos maestros de inventario...");
        try {
            // 1. Tabla unidades_medida
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS unidades_medida (
                    id SERIAL PRIMARY KEY,
                    codigo VARCHAR(20) NOT NULL,
                    nombre VARCHAR(100) NOT NULL,
                    abreviatura VARCHAR(20) NOT NULL,
                    activo BOOLEAN NOT NULL DEFAULT TRUE,
                    empresa_id UUID,
                    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
            """);

            try {
                jdbc.execute("ALTER TABLE unidades_medida ADD COLUMN IF NOT EXISTS empresa_id UUID;");
            } catch (Exception ignored) {}

            try {
                jdbc.execute("ALTER TABLE unidades_medida ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;");
            } catch (Exception ignored) {}

            try {
                jdbc.execute("ALTER TABLE unidades_medida ALTER COLUMN abreviatura TYPE VARCHAR(20);");
            } catch (Exception ignored) {}

            try {
                jdbc.execute("UPDATE unidades_medida SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;");
            } catch (Exception ignored) {}

            try {
                jdbc.execute("UPDATE unidades_medida SET activo = TRUE WHERE activo IS NULL;");
            } catch (Exception ignored) {}

            // 2. Tabla categorias
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS categorias (
                    id SERIAL PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    descripcion TEXT,
                    padre_id INTEGER,
                    activo BOOLEAN DEFAULT TRUE,
                    empresa_id UUID,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            try {
                jdbc.execute("ALTER TABLE categorias ADD COLUMN IF NOT EXISTS padre_id INTEGER;");
                jdbc.execute("ALTER TABLE categorias ADD COLUMN IF NOT EXISTS empresa_id UUID;");
                jdbc.execute("ALTER TABLE categorias ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;");
            } catch (Exception ignored) {}

            // 3. Semillas básicas de Unidades de Medida si está vacía
            Integer countUnidades = 0;
            try {
                countUnidades = jdbc.queryForObject("SELECT COUNT(*) FROM unidades_medida", Integer.class);
            } catch (Exception ignored) {}

            if (countUnidades == null || countUnidades == 0) {
                jdbc.execute("""
                    INSERT INTO unidades_medida (codigo, nombre, abreviatura, activo) VALUES
                    ('UND', 'Unidad', 'u', true),
                    ('CAJ', 'Caja', 'caja', true),
                    ('PZA', 'Pieza', 'pza', true),
                    ('KG', 'Kilogramo', 'kg', true),
                    ('MTR', 'Metro', 'm', true),
                    ('LIT', 'Litro', 'l', true),
                    ('PQTE', 'Paquete', 'paq', true),
                    ('DOC', 'Docena', 'doc', true),
                    ('SET', 'Set / Kit', 'set', true);
                """);
                log.info("Semillas de unidades_medida insertadas correctamente.");
            }

            // 4. Inicializar y verificar tabla CIUDAD
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS ciudad (
                    id SERIAL PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    pais VARCHAR(100) NOT NULL DEFAULT 'Bolivia'
                );
            """);

            Integer countCiudades = 0;
            try {
                countCiudades = jdbc.queryForObject("SELECT COUNT(*) FROM ciudad", Integer.class);
            } catch (Exception ignored) {}

            if (countCiudades == null || countCiudades == 0) {
                jdbc.execute("""
                    INSERT INTO ciudad (id, nombre, pais) VALUES
                    (1, 'Santa Cruz de la Sierra', 'Bolivia'),
                    (2, 'La Paz', 'Bolivia'),
                    (3, 'Cochabamba', 'Bolivia'),
                    (4, 'Sucre', 'Bolivia'),
                    (5, 'Tarija', 'Bolivia'),
                    (6, 'Oruro', 'Bolivia'),
                    (7, 'Potosí', 'Bolivia'),
                    (8, 'Trinidad (Beni)', 'Bolivia'),
                    (9, 'Cobija (Pando)', 'Bolivia')
                    ON CONFLICT (id) DO NOTHING;
                """);
                log.info("Catálogo de ciudades inicializado.");
            }

            // 5. Asegurar que las sucursales mantengan sus nombres oficiales de ERP_Asus
            try {
                jdbc.execute("""
                    UPDATE sucursal 
                    SET nombre = 'Sucursal Central Equipetrol' 
                    WHERE id = 1 AND (LOWER(nombre) LIKE '%sucursal principal%' OR LOWER(nombre) LIKE '%almac%');
                """);
            } catch (Exception ignored) {}

            log.info("Esquema de datos maestros de inventario listo y verificado.");
        } catch (Exception e) {
            log.warn("Aviso durante inicialización de datos maestros: {}", e.getMessage());
        }
    }
}
