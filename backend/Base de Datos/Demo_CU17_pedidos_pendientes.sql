-- ============================================================================
-- DEMO CU17 - Validación Comercial y Aprobación
-- Pedidos PENDIENTES de ejemplo para probar la bandeja de validación.
--
-- Supone los datos semilla de ERP_Asus.txt (clientes 1-3, productos 1-8,
-- sucursal 1 con su inventario). Es idempotente: se puede ejecutar más de una
-- vez sin duplicar pedidos. NO se ejecuta solo: córrelo a mano en la base que
-- quieras usar (de preferencia una local, no la compartida).
--
--   PED-DEMO-0001  Cliente a crédito, dentro del límite y con stock   -> aprobable
--   PED-DEMO-0002  Pide 50 laptops y hay 5 en sucursal                -> bloqueado por stock
--   PED-DEMO-0003  Cliente a crédito, el pedido pasa el límite        -> bloqueado por crédito
--   PED-DEMO-0004  Contado con 15% de descuento                       -> aprobable con justificación
-- ============================================================================

INSERT INTO PEDIDOS (CLIENTE_ID, VENDEDOR_ID, SUCURSAL_ID, NUMERO_PEDIDO, FECHA, SUBTOTAL, DESCUENTO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES
(1, 2, 1, 'PED-DEMO-0001', CURRENT_DATE, 17800.00,   0.00, 17800.00, 'PENDIENTE', 'Demo CU17: dentro del límite de crédito', NOW()),
(2, 2, 1, 'PED-DEMO-0002', CURRENT_DATE, 1225000.00, 0.00, 1225000.00, 'PENDIENTE', 'Demo CU17: stock insuficiente', NOW()),
(1, 2, 1, 'PED-DEMO-0003', CURRENT_DATE, 62300.00,   0.00, 62300.00, 'PENDIENTE', 'Demo CU17: supera el límite de crédito', NOW()),
(3, 2, 1, 'PED-DEMO-0004', CURRENT_DATE, 2900.00,  435.00,  2465.00, 'PENDIENTE', 'Demo CU17: descuento sobre el tope', NOW())
ON CONFLICT (NUMERO_PEDIDO) DO NOTHING;

INSERT INTO PEDIDO_DETALLES (PEDIDO_ID, PRODUCTO_ID, CANTIDAD, PRECIO_UNITARIO, DESCUENTO, SUBTOTAL)
SELECT p.ID, d.producto_id, d.cantidad, d.precio, d.descuento, d.subtotal
FROM PEDIDOS p
JOIN (VALUES
    ('PED-DEMO-0001', 6,  2, 8900.00,  0.00, 17800.00),
    ('PED-DEMO-0002', 1, 50, 24500.00, 0.00, 1225000.00),
    ('PED-DEMO-0003', 6,  7, 8900.00,  0.00, 62300.00),
    ('PED-DEMO-0004', 7,  2, 1450.00, 435.00, 2465.00)
) AS d(numero, producto_id, cantidad, precio, descuento, subtotal) ON d.numero = p.NUMERO_PEDIDO
WHERE NOT EXISTS (SELECT 1 FROM PEDIDO_DETALLES x WHERE x.PEDIDO_ID = p.ID);

-- Para volver a empezar:
--   DELETE FROM PEDIDO_DETALLES WHERE PEDIDO_ID IN (SELECT ID FROM PEDIDOS WHERE NUMERO_PEDIDO LIKE 'PED-DEMO-%');
--   DELETE FROM PEDIDOS WHERE NUMERO_PEDIDO LIKE 'PED-DEMO-%';
