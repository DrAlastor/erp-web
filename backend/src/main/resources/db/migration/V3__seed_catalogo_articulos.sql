-- ===================================================================
-- HU-05: Datos de prueba del catálogo de artículos
-- ===================================================================

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Electrónica', 'Dispositivos y accesorios electrónicos'),
    ('Oficina', 'Útiles y equipamiento de oficina'),
    ('Alimentos', 'Productos alimenticios y bebidas')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO articulos (sku, nombre, descripcion, precio, stock, imagen_url, categoria_id, activo, fecha_creacion)
SELECT
    v.sku,
    v.nombre,
    v.descripcion,
    v.precio,
    v.stock,
    v.imagen_url,
    c.id,
    v.activo,
    CURRENT_TIMESTAMP
FROM (
    VALUES
        ('SKU-ELC-001', 'Mouse inalámbrico', 'Mouse ergonómico con receptor USB', 89.90, 25,
         'https://placehold.co/400x300/dbeafe/1d4ed8?text=Mouse', 'Electrónica', TRUE),
        ('SKU-ELC-002', 'Teclado mecánico', 'Teclado RGB switch blue', 349.00, 8,
         'https://placehold.co/400x300/ede9fe/6d28d9?text=Teclado', 'Electrónica', TRUE),
        ('SKU-ELC-003', 'Audífonos Bluetooth', 'Cancelación de ruido activa', 299.50, 0,
         'https://placehold.co/400x300/cffafe/0e7490?text=Audifonos', 'Electrónica', TRUE),
        ('SKU-OFI-001', 'Resma de papel A4', '500 hojas 75 g/m²', 32.00, 120,
         'https://placehold.co/400x300/ffedd5/c2410c?text=Papel', 'Oficina', TRUE),
        ('SKU-OFI-002', 'Archivador A4', 'Carpeta de 3 anillos', 45.00, 5,
         'https://placehold.co/400x300/f1f5f9/475569?text=Archivador', 'Oficina', TRUE),
        ('SKU-ALI-001', 'Café molido 500g', 'Café torrado medio', 28.50, 40,
         'https://placehold.co/400x300/ecfdf5/047857?text=Cafe', 'Alimentos', TRUE),
        ('SKU-ALI-002', 'Agua mineral 2L', 'Pack individual', 6.50, 3,
         'https://placehold.co/400x300/eff6ff/2563eb?text=Agua', 'Alimentos', FALSE)
) AS v(sku, nombre, descripcion, precio, stock, imagen_url, categoria_nombre, activo)
JOIN categorias c ON c.nombre = v.categoria_nombre
WHERE NOT EXISTS (
    SELECT 1 FROM articulos a WHERE a.sku = v.sku
);
