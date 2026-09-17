-- ============================================================================
-- PROYECTO ERP - FASE INCREMENTAL 1
-- POBLACIÓN DE DATOS SEMILLA Y CONSULTAS DE VERIFICACIÓN (PostgreSQL 16)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. ESTRUCTURA BASE (DDL)
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullname VARCHAR(150) NOT NULL,
    enable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE IF NOT EXISTS permisos (
    id SERIAL PRIMARY KEY,
    modulo VARCHAR(50) NOT NULL,
    pantalla VARCHAR(50) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT uq_permisos_modulo_pantalla_accion UNIQUE (modulo, pantalla, accion)
);

CREATE TABLE IF NOT EXISTS rol_permisos (
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id INTEGER NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE IF NOT EXISTS sesion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_origen VARCHAR(45),
    user_agent TEXT,
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP,
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    nit_ci VARCHAR(30) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categorias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS almacenes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255),
    es_principal BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    categoria_id INTEGER REFERENCES categorias(id) ON DELETE SET NULL,
    codigo_sku VARCHAR(50) UNIQUE,
    codigo_barra VARCHAR(50) UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    unidad_medida VARCHAR(20) DEFAULT 'UNIDAD',
    precio_venta NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    costo_promedio NUMERIC(12,2) DEFAULT 0.00,
    stock_minimo INTEGER DEFAULT 5,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_almacen (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    almacen_id INTEGER NOT NULL REFERENCES almacenes(id) ON DELETE CASCADE,
    cantidad_actual NUMERIC(12,2) DEFAULT 0.00,
    CONSTRAINT uq_stock_producto_almacen UNIQUE (producto_id, almacen_id)
);

-- ÍNDICES
CREATE INDEX IF NOT EXISTS idx_sesion_usuario_id ON sesion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_stock_producto_almacen ON stock_almacen(producto_id, almacen_id);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_clientes_nit_ci ON clientes(nit_ci);

-- ----------------------------------------------------------------------------
-- 2. POBLACIÓN DE DATOS (DML)
-- ----------------------------------------------------------------------------

-- 2.1 Roles del Sistema
INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN', 'Administrador global del sistema con acceso completo'),
    ('VENDEDOR', 'Gestión de clientes, cotizaciones y ventas'),
    ('ALMACENERO', 'Gestión del catálogo de productos y control de stock')
ON CONFLICT (nombre) DO NOTHING;

-- 2.2 Permisos Granulares por Pantalla
INSERT INTO permisos (modulo, pantalla, accion, descripcion) VALUES
    ('SEGURIDAD', 'USUARIOS', 'LECTURA', 'Ver lista de usuarios'),
    ('SEGURIDAD', 'USUARIOS', 'ESCRITURA', 'Crear y editar usuarios'),
    ('SEGURIDAD', 'ROLES', 'ESCRITURA', 'Gestionar roles y permisos'),
    ('COMERCIAL', 'CLIENTES', 'LECTURA', 'Ver lista de clientes'),
    ('COMERCIAL', 'CLIENTES', 'ESCRITURA', 'Crear y modificar clientes'),
    ('INVENTARIO', 'PRODUCTOS', 'LECTURA', 'Ver catálogo de productos'),
    ('INVENTARIO', 'PRODUCTOS', 'ESCRITURA', 'Crear y actualizar productos'),
    ('INVENTARIO', 'STOCK', 'ESCRITURA', 'Ajustar existencias de stock')
ON CONFLICT DO NOTHING;

-- 2.3 Asignación de Permisos a Roles
-- ADMIN -> Todos los permisos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p WHERE r.nombre = 'ADMIN'
ON CONFLICT DO NOTHING;

-- VENDEDOR -> Permisos comerciales y lectura de productos
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p 
WHERE r.nombre = 'VENDEDOR' AND p.modulo IN ('COMERCIAL', 'INVENTARIO') AND p.accion = 'LECTURA'
ON CONFLICT DO NOTHING;

-- ALMACENERO -> Permisos de inventario
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p 
WHERE r.nombre = 'ALMACENERO' AND p.modulo = 'INVENTARIO'
ON CONFLICT DO NOTHING;

-- 2.4 Usuarios (Passwords en BCrypt = 'password123')
INSERT INTO usuarios (username, email, password, fullname, enable) VALUES
    ('admin', 'admin@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Administrador General', TRUE),
    ('nicolas', 'nicolas.seguridad@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Nicolás Arnez (HU-01 Login)', TRUE),
    ('alessandro', 'alessandro.user@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Alessandro Vaca (HU-02 Usuarios)', TRUE),
    ('santi', 'santi.roles@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Santiago Justiniano (HU-03 Roles)', TRUE),
    ('javier', 'javier.comercial@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Javier López (HU-04/05 Comercial)', TRUE),
    ('leonardo', 'leonardo.stock@erp.com', '$2a$10$e8wE4z48oR.6T2Gq/6LqPuvz4r4rP4u2eD3aK6g9.L9R.2b5mD9hK', 'Leonardo Méndez (HU-06 Inventario)', TRUE)
ON CONFLICT (username) DO NOTHING;

-- 2.5 Asignación de Roles a Usuarios
INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r WHERE u.username = 'admin' AND r.nombre = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r WHERE u.username IN ('nicolas', 'alessandro', 'santi') AND r.nombre = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r WHERE u.username = 'javier' AND r.nombre = 'VENDEDOR'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r WHERE u.username = 'leonardo' AND r.nombre = 'ALMACENERO'
ON CONFLICT DO NOTHING;

-- 2.6 Sesiones de Usuario (Simulación de HU-01 - Control de Refresh Tokens)
INSERT INTO sesion (usuario_id, refresh_token_hash, ip_origen, user_agent, fecha_inicio, fecha_expiracion, fecha_cierre)
SELECT 
    id, 
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', -- Hash SHA-256 simulado
    '192.168.1.45', 
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/128.0.0.0', 
    CURRENT_TIMESTAMP - INTERVAL '2 hours', 
    CURRENT_TIMESTAMP + INTERVAL '1 day', 
    NULL
FROM usuarios WHERE username = 'nicolas';

-- 2.7 Clientes (HU-04 - Javier)
INSERT INTO clientes (razon_social, nit_ci, telefono, direccion, activo) VALUES
    ('Importadora & Distribuidora Oriente S.R.L.', '1029384029', '77012345', 'Av. Cristo Redentor 3er Anillo #320', TRUE),
    ('Comercial Bazoalto & Asociados', '4920192011', '3345900', 'Calle René Moreno #145', TRUE),
    ('Farmacias Cruz Verde S.A.', '8930219018', '71098234', 'Av. Banzer Km 5', TRUE),
    ('Supermercado El Dorado', '3029102015', '78511223', 'Av. San Martín Equipetrol #880', TRUE),
    ('Juan Pérez Cabrera (Persona Natural)', '5839201', '76044332', 'Barrio Sirari Calle 4 #12', TRUE)
ON CONFLICT DO NOTHING;

-- 2.8 Categorías de Productos
INSERT INTO categorias (nombre, descripcion, activo) VALUES
    ('Electrónica y Cómputo', 'Equipos portátiles, componentes y periféricos', TRUE),
    ('Servidores y Redes', 'Switches, routers empresariales y accesorios de rack', TRUE),
    ('Suministros de Oficina', 'Material de papelería, impresoras y consumibles', TRUE)
ON CONFLICT DO NOTHING;

-- 2.9 Almacenes
INSERT INTO almacenes (nombre, direccion, es_principal, activo) VALUES
    ('Almacén Central (Casa Matriz)', 'Parque Industrial PI-37', TRUE, TRUE),
    ('Almacén Sucursal Equipetrol', 'Calle 7 Oeste Equipetrol #45', FALSE, TRUE)
ON CONFLICT DO NOTHING;

-- 2.10 Catálogo de Productos (HU-05 - Javier)
INSERT INTO productos (categoria_id, codigo_sku, codigo_barra, nombre, unidad_medida, precio_venta, costo_promedio, stock_minimo, activo) VALUES
    (1, 'LAP-DELL-G15', '7891011121314', 'Laptop Dell G15 Ryzen 7 16GB RAM 512GB SSD', 'UNIDAD', 1250.00, 980.00, 3, TRUE),
    (1, 'MON-LG-27', '7891011121315', 'Monitor Gamer LG 27" IPS 144Hz 1ms', 'UNIDAD', 280.00, 210.00, 5, TRUE),
    (2, 'SW-CISCO-24P', '7891011121316', 'Switch Cisco Catalyst 24 Puertos Gigabit Mng', 'UNIDAD', 650.00, 490.00, 2, TRUE),
    (2, 'RTR-MIK-RB3011', '7891011121317', 'Routerboard Mikrotik RB3011UiAS-RM Rack', 'UNIDAD', 220.00, 165.00, 4, TRUE),
    (3, 'TTON-HP-05A', '7891011121318', 'Toner HP LaserJet CE505A Negro Original', 'UNIDAD', 85.00, 58.00, 10, TRUE)
ON CONFLICT DO NOTHING;

-- 2.11 Existencias de Productos por Almacén (HU-06 - Leonardo)
INSERT INTO stock_almacen (producto_id, almacen_id, cantidad_actual) VALUES
    (1, 1, 15.00), -- 15 Laptops en Almacén Central
    (1, 2, 3.00),  -- 3 Laptops en Sucursal
    (2, 1, 25.00), -- 25 Monitores en Almacén Central
    (3, 1, 8.00),  -- 8 Switches en Almacén Central
    (4, 1, 12.00), -- 12 Routers en Almacén Central
    (5, 1, 50.00), -- 50 Toners en Almacén Central
    (5, 2, 15.00)  -- 15 Toners en Sucursal
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3. CONSULTAS SELECT DE VERIFICACIÓN (MOSTRAR ATRIBUTOS)
-- ============================================================================

-- 3.1 Ver Usuarios, sus Roles y Estado
SELECT 
    u.id AS usuario_id,
    u.username,
    u.email,
    u.fullname,
    u.enable AS activo,
    STRING_AGG(r.nombre, ', ') AS roles_asignados
FROM usuarios u
LEFT JOIN usuario_roles ur ON u.id = ur.usuario_id
LEFT JOIN roles r ON ur.rol_id = r.id
GROUP BY u.id, u.username, u.email, u.fullname, u.enable
ORDER BY u.id;

-- 3.2 Ver Sesiones Activas y Tokens (HU-01)
SELECT 
    s.id AS sesion_id,
    u.username,
    s.ip_origen,
    s.user_agent,
    s.fecha_inicio,
    s.fecha_expiracion,
    CASE WHEN s.fecha_cierre IS NULL THEN 'ACTIVA' ELSE 'CERRADA' END AS estado_sesion
FROM sesion s
JOIN usuarios u ON s.usuario_id = u.id;

-- 3.3 Ver Roles y sus Permisos Asignados (HU-03)
SELECT 
    r.nombre AS rol,
    p.modulo,
    p.pantalla,
    p.accion,
    p.descripcion
FROM roles r
JOIN rol_permisos rp ON r.id = rp.rol_id
JOIN permisos p ON rp.permiso_id = p.id
ORDER BY r.nombre, p.modulo;

-- 3.4 Ver Directorio de Clientes (HU-04)
SELECT 
    id AS cliente_id,
    razon_social,
    nit_ci,
    telefono,
    direccion,
    activo
FROM clientes
ORDER BY id;

-- 3.5 Ver Catálogo de Productos con Categoría (HU-05)
SELECT 
    p.id AS producto_id,
    p.codigo_sku,
    p.codigo_barra,
    p.nombre AS producto,
    c.nombre AS categoria,
    p.precio_venta,
    p.costo_promedio,
    p.stock_minimo,
    p.unidad_medida,
    p.activo
FROM productos p
LEFT JOIN categorias c ON p.categoria_id = c.id
ORDER BY p.id;

-- 3.6 Ver Stock Consolidado por Almacén (HU-06)
SELECT 
    a.nombre AS almacen,
    p.codigo_sku,
    p.nombre AS producto,
    sa.cantidad_actual AS stock_disponible,
    p.unidad_medida,
    CASE 
        WHEN sa.cantidad_actual <= p.stock_minimo THEN 'ALERTA: STOCK BAJO'
        ELSE 'NORMAL'
    END AS estado_stock
FROM stock_almacen sa
JOIN productos p ON sa.producto_id = p.id
JOIN almacenes a ON sa.almacen_id = a.id
ORDER BY a.nombre, p.nombre;