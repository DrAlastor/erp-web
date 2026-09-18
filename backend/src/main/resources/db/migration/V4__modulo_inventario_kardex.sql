-- ===================================================================
-- Migración V4: Módulo de Inventario y Kardex de Movimientos
-- Historia de Usuario: CU-09 Movimientos de Inventario
-- Desarrollador: Mopy Cabezas Leonardo (Backend)
-- ===================================================================

-- 1. Tabla de Categorías de Productos
CREATE TABLE IF NOT EXISTS categorias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE
);

-- 2. Tabla de Almacenes
CREATE TABLE IF NOT EXISTS almacenes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255),
    es_principal BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE
);

-- 3. Tabla de Productos (Catálogo de Artículos)
CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    categoria_id INTEGER REFERENCES categorias(id) ON DELETE SET NULL,
    impuesto_id INTEGER,
    codigo_sku VARCHAR(50) UNIQUE,
    codigo_barra VARCHAR(50) UNIQUE,
    unidad_medida VARCHAR(20) DEFAULT 'UNIDAD',
    nombre VARCHAR(150) NOT NULL,
    precio_venta NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    costo_promedio NUMERIC(12,2) DEFAULT 0.00,
    stock_minimo INTEGER DEFAULT 5,
    activo BOOLEAN DEFAULT TRUE,
    creado_por VARCHAR(50),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modificado_por VARCHAR(50),
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabla de Existencias por Almacén (Stock Actual)
CREATE TABLE IF NOT EXISTS stock_almacen (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    almacen_id INTEGER NOT NULL REFERENCES almacenes(id) ON DELETE CASCADE,
    cantidad_actual NUMERIC(12,2) DEFAULT 0.00,
    CONSTRAINT uq_stock_producto_almacen UNIQUE (producto_id, almacen_id)
);

-- 5. Tabla de Kardex de Movimientos (Registro Inmutable / Append-Only)
CREATE TABLE IF NOT EXISTS kardex_movimientos (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    almacen_id INTEGER NOT NULL REFERENCES almacenes(id) ON DELETE RESTRICT,
    tipo_movimiento VARCHAR(20) NOT NULL, -- ENTRADA, SALIDA, AJUSTE_POSITIVO, AJUSTE_NEGATIVO
    cantidad NUMERIC(12,2) NOT NULL,
    costo_unitario NUMERIC(12,2) NOT NULL,
    saldo_cantidad NUMERIC(12,2) NOT NULL,
    saldo_valorado NUMERIC(12,2) NOT NULL,
    referencia_doc VARCHAR(100),
    motivo VARCHAR(255), -- Obligatorio en ajustes
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creado_por VARCHAR(50)
);

-- Índices de Rendimiento
CREATE INDEX IF NOT EXISTS idx_stock_producto_almacen ON stock_almacen(producto_id, almacen_id);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_kardex_producto_almacen ON kardex_movimientos(producto_id, almacen_id);
CREATE INDEX IF NOT EXISTS idx_kardex_fecha ON kardex_movimientos(fecha);

-- 6. Permisos y Roles de Inventario (RBAC)
INSERT INTO permisos (modulo, pantalla, accion, descripcion) VALUES
    ('INVENTARIO', 'MOVIMIENTOS', 'LECTURA', 'Consultar kardex y movimientos de inventario'),
    ('INVENTARIO', 'MOVIMIENTOS', 'ESCRITURA', 'Registrar movimientos de inventario'),
    ('INVENTARIO', 'STOCK', 'LECTURA', 'Consultar existencias de stock')
ON CONFLICT (modulo, pantalla, accion) DO NOTHING;

INSERT INTO roles (nombre, descripcion) VALUES
    ('ALMACENERO', 'Gestión del catálogo de productos y control de stock')
ON CONFLICT (nombre) DO NOTHING;

-- Asignar permisos al rol ADMIN y ALMACENERO
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE upper(r.nombre) IN ('ADMIN', 'ADMINISTRADOR') AND p.modulo = 'INVENTARIO'
ON CONFLICT DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE upper(r.nombre) = 'ALMACENERO' AND p.modulo = 'INVENTARIO'
ON CONFLICT DO NOTHING;

-- Usuario Leonardo (ALMACENERO) con contraseña password123
INSERT INTO usuarios (username, email, password, fullname, enable, created_by)
VALUES ('leonardo', 'leonardo.stock@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Leonardo Méndez (HU-06 Inventario)', TRUE, 'system')
ON CONFLICT (username) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r 
WHERE u.username = 'leonardo' AND r.nombre = 'ALMACENERO'
ON CONFLICT DO NOTHING;

-- 7. Datos Semilla Oficiales (Almacenes, Categorías, Productos y Stock Inicial)
INSERT INTO almacenes (id, nombre, direccion, es_principal, activo) VALUES
    (1, 'Almacén Central (Casa Matriz)', 'Parque Industrial PI-37', TRUE, TRUE),
    (2, 'Almacén Sucursal Equipetrol', 'Calle 7 Oeste Equipetrol #45', FALSE, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, descripcion, activo) VALUES
    (1, 'Electrónica y Cómputo', 'Equipos portátiles, componentes y periféricos', TRUE),
    (2, 'Servidores y Redes', 'Switches, routers empresariales y accesorios de rack', TRUE),
    (3, 'Suministros de Oficina', 'Material de papelería, impresoras y consumibles', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO productos (id, categoria_id, codigo_sku, codigo_barra, nombre, unidad_medida, precio_venta, costo_promedio, stock_minimo, activo) VALUES
    (1, 1, 'LAP-DELL-G15', '7891011121314', 'Laptop Dell G15 Ryzen 7 16GB RAM 512GB SSD', 'UNIDAD', 1250.00, 980.00, 3, TRUE),
    (2, 1, 'MON-LG-27', '7891011121315', 'Monitor Gamer LG 27" IPS 144Hz 1ms', 'UNIDAD', 280.00, 210.00, 5, TRUE),
    (3, 2, 'SW-CISCO-24P', '7891011121316', 'Switch Cisco Catalyst 24 Puertos Gigabit Mng', 'UNIDAD', 650.00, 490.00, 2, TRUE),
    (4, 2, 'RTR-MIK-RB3011', '7891011121317', 'Routerboard Mikrotik RB3011UiAS-RM Rack', 'UNIDAD', 220.00, 165.00, 4, TRUE),
    (5, 3, 'TTON-HP-05A', '7891011121318', 'Toner HP LaserJet CE505A Negro Original', 'UNIDAD', 85.00, 58.00, 10, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO stock_almacen (producto_id, almacen_id, cantidad_actual) VALUES
    (1, 1, 15.00),
    (1, 2, 3.00),
    (2, 1, 25.00),
    (3, 1, 8.00),
    (4, 1, 12.00),
    (5, 1, 50.00),
    (5, 2, 15.00)
ON CONFLICT (producto_id, almacen_id) DO NOTHING;

-- Sincronizar secuencias
SELECT setval('almacenes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM almacenes));
SELECT setval('categorias_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categorias));
SELECT setval('productos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM productos));
