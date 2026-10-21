-- ============================================================================
-- DEMO HU-24 / CU34 - Gestionar Cuentas por Pagar
-- Proveedores, compras a crédito, cuotas en distintos estados y vencimientos,
-- y algunos pagos, para probar listado, resumen, antigüedad, pagos y la
-- generación de cuotas.
--
-- Supone los datos semilla de ERP_Asus.txt (proveedores ASUS y TECH DATA,
-- formas de pago EFECTIVO/TRANSFERENCIA, al menos un usuario y una sucursal).
-- Las fechas son relativas a CURRENT_DATE, así que la demo "envejece" bien.
-- Es idempotente: se puede ejecutar varias veces sin duplicar nada.
-- NO se ejecuta solo: córrelo a mano, de preferencia en una base local y no
-- en la RDS compartida del equipo.
--
--   FC-DEMO-0001 ASUS        3 cuotas: PAGADA / PARCIAL vencida 31-60 / PENDIENTE vencida 0-30
--   FC-DEMO-0002 DEMO NORTE  2 cuotas: PENDIENTE vencida 90+ / PARCIAL vencida 61-90
--   FC-DEMO-0003 DEMO SUR    2 cuotas: PENDIENTE por vencer en 5 días / PENDIENTE a 35 días
--   FC-DEMO-0004 TECH DATA   crédito SIN cuotas  -> aparece en "compras sin cuenta"
--   FC-DEMO-0005 DEMO SUR    contado             -> NO aparece (no es a crédito)
--   FC-DEMO-0006 DEMO NORTE  crédito ANULADA     -> NO aparece (anulada)
-- ============================================================================

INSERT INTO PROVEEDORES (RAZON_SOCIAL, NIT_CI, CONTACTO, TELEFONO, EMAIL, DIRECCION, CIUDAD, CONDICION_PAGO, DIAS_CREDITO, ACTIVO, FECHA_CREACION) VALUES
('DISTRIBUIDORA DEMO NORTE S.R.L.', '9000000001', 'Mariela Rojas', '33221100', 'ventas@demonorte.bo', 'Av. Banzer 4to anillo #55', 'Santa Cruz de la Sierra', 'CREDITO', 45, TRUE, NOW()),
('IMPORTADORA DEMO SUR LTDA.',      '9000000002', 'Jorge Vaca',    '33887766', 'pedidos@demosur.bo', 'Av. Santos Dumont #310',   'Santa Cruz de la Sierra', 'CREDITO', NULL, TRUE, NOW())
ON CONFLICT (NIT_CI) DO NOTHING;

INSERT INTO COMPRAS (PROVEEDOR_ID, SUCURSAL_ID, USUARIO_ID, NUMERO_FACTURA, NUMERO_AUTORIZACION, FECHA, SUBTOTAL, IVA, TOTAL, TIPO_PAGO, ESTADO, OBSERVACIONES, FECHA_CREACION)
SELECT pv.ID, (SELECT MIN(ID) FROM SUCURSAL), (SELECT MIN(ID) FROM USUARIO),
       c.numero, c.autorizacion, CURRENT_DATE + c.dias, c.total, ROUND(c.total * 0.13, 2), c.total,
       c.tipo_pago, c.estado, 'Demo HU-24', NOW()
FROM (VALUES
    ('FC-DEMO-0001', '9081726354', '49000000001', -100, 30000.00, 'CREDITO', 'COMPLETADA'),
    ('FC-DEMO-0002', '9000000001', '49000000002', -150, 12000.00, 'CREDITO', 'COMPLETADA'),
    ('FC-DEMO-0003', '9000000002', '49000000003',  -20,  9000.00, 'CREDITO', 'COMPLETADA'),
    ('FC-DEMO-0004', '1029384756', '49000000004',   -5, 15600.00, 'CREDITO', 'COMPLETADA'),
    ('FC-DEMO-0005', '9000000002', '49000000005',   -3,  2500.00, 'CONTADO', 'COMPLETADA'),
    ('FC-DEMO-0006', '9000000001', '49000000006',  -30,  4800.00, 'CREDITO', 'ANULADA')
) AS c(numero, nit, autorizacion, dias, total, tipo_pago, estado)
JOIN PROVEEDORES pv ON pv.NIT_CI = c.nit
WHERE NOT EXISTS (SELECT 1 FROM COMPRAS x WHERE x.NUMERO_FACTURA = c.numero AND x.PROVEEDOR_ID = pv.ID);

INSERT INTO CUENTAS_POR_PAGAR (COMPRA_ID, PROVEEDOR_ID, CREADO_POR, NUMERO_CUOTA, MONTO_TOTAL, MONTO_PAGADO, SALDO, FECHA_VENCIMIENTO, ESTADO, FECHA_CREACION)
SELECT co.ID, co.PROVEEDOR_ID, co.USUARIO_ID, q.cuota, q.monto, q.pagado, q.monto - q.pagado,
       CURRENT_DATE + q.dias, q.estado, NOW()
FROM (VALUES
    ('FC-DEMO-0001', 1, 10000.00, 10000.00,  -70, 'PAGADA'),
    ('FC-DEMO-0001', 2, 10000.00,  4000.00,  -40, 'PARCIAL'),
    ('FC-DEMO-0001', 3, 10000.00,     0.00,  -10, 'PENDIENTE'),
    ('FC-DEMO-0002', 1,  6000.00,     0.00, -120, 'PENDIENTE'),
    ('FC-DEMO-0002', 2,  6000.00,  1500.00,  -75, 'PARCIAL'),
    ('FC-DEMO-0003', 1,  4500.00,     0.00,    5, 'PENDIENTE'),
    ('FC-DEMO-0003', 2,  4500.00,     0.00,   35, 'PENDIENTE')
) AS q(numero, cuota, monto, pagado, dias, estado)
JOIN COMPRAS co ON co.NUMERO_FACTURA = q.numero AND co.OBSERVACIONES = 'Demo HU-24'
WHERE NOT EXISTS (
    SELECT 1 FROM CUENTAS_POR_PAGAR x WHERE x.COMPRA_ID = co.ID AND x.NUMERO_CUOTA = q.cuota);

-- Los pagos cuadran con MONTO_PAGADO de cada cuota (REC-DEMO-004 cae en el mes actual).
INSERT INTO PAGOS_PROVEEDOR (CUENTA_POR_PAGAR_ID, FORMA_PAGO_ID, USUARIO_ID, MONTO, FECHA_PAGO, NUMERO_RECIBO, TRANSACCION_EXTERNA, OBSERVACIONES, FECHA_CREACION)
SELECT cx.ID, fp.ID, co.USUARIO_ID, p.monto, CURRENT_DATE + p.dias, p.recibo, p.transaccion, 'Demo HU-24', NOW()
FROM (VALUES
    ('FC-DEMO-0001', 1, 'REC-DEMO-001', 'TRANSFERENCIA', 6000.00, -72, 'TRANSF-DEMO-0001'),
    ('FC-DEMO-0001', 1, 'REC-DEMO-002', 'EFECTIVO',      4000.00, -70, NULL),
    ('FC-DEMO-0001', 2, 'REC-DEMO-003', 'TRANSFERENCIA', 4000.00, -38, 'TRANSF-DEMO-0002'),
    ('FC-DEMO-0002', 2, 'REC-DEMO-004', 'EFECTIVO',      1500.00,   0, NULL)
) AS p(numero, cuota, recibo, forma, monto, dias, transaccion)
JOIN COMPRAS co ON co.NUMERO_FACTURA = p.numero AND co.OBSERVACIONES = 'Demo HU-24'
JOIN CUENTAS_POR_PAGAR cx ON cx.COMPRA_ID = co.ID AND cx.NUMERO_CUOTA = p.cuota
JOIN FORMAS_PAGO fp ON fp.CODIGO = p.forma
WHERE NOT EXISTS (SELECT 1 FROM PAGOS_PROVEEDOR x WHERE x.NUMERO_RECIBO = p.recibo);

-- Para volver a empezar:
--   DELETE FROM PAGOS_PROVEEDOR WHERE NUMERO_RECIBO LIKE 'REC-DEMO-%';
--   DELETE FROM CUENTAS_POR_PAGAR WHERE COMPRA_ID IN (SELECT ID FROM COMPRAS WHERE OBSERVACIONES = 'Demo HU-24');
--   DELETE FROM COMPRAS WHERE OBSERVACIONES = 'Demo HU-24';
--   DELETE FROM PROVEEDORES WHERE NIT_CI IN ('9000000001', '9000000002');
