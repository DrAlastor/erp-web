-- ===================================================================
-- CU25 Plan de Cuentas / CU26 Periodos Contables
--
-- 1. Completa el plan de cuentas base: agrega los grupos intermedios que
--    faltaban en la semilla (2.1.1, 3.1, 3.1.1, 4.1, 4.1.1, 5.1, 5.1.1...)
--    y las cuentas de la contabilizacion automatica de ventas.
-- 2. Recalcula NIVEL (cantidad de segmentos) y PADRE_ID (la cuenta cuyo
--    codigo es el prefijo mas largo) de todas las cuentas.
-- 3. Indices para el arbol y para la consulta de periodos por gestion.
--
-- Idempotente: se puede ejecutar a mano en cualquier base de empresa.
-- No borra nada (las FK de contabilidad son ON DELETE CASCADE).
-- ===================================================================

INSERT INTO CUENTAS_CONTABLES (CODIGO, NOMBRE, NIVEL, TIPO, NATURALEZA, PERMITE_MOVIMIENTO, PADRE_ID, ACTIVO)
SELECT v.codigo, v.nombre, 0, v.tipo, v.naturaleza, v.movimiento, NULL, TRUE
FROM (VALUES
    ('1',        'ACTIVO',                          'ACTIVO',     'DEUDORA',   FALSE),
    ('1.1',      'ACTIVO CORRIENTE',                'ACTIVO',     'DEUDORA',   FALSE),
    ('1.1.1',    'DISPONIBILIDADES',                'ACTIVO',     'DEUDORA',   FALSE),
    ('1.1.1.01', 'Caja Moneda Nacional',            'ACTIVO',     'DEUDORA',   TRUE),
    ('1.1.1.02', 'Bancos Cuenta Corriente',         'ACTIVO',     'DEUDORA',   TRUE),
    ('1.1.2',    'EXIGIBLE',                        'ACTIVO',     'DEUDORA',   FALSE),
    ('1.1.2.01', 'Cuentas por Cobrar Clientes',     'ACTIVO',     'DEUDORA',   TRUE),
    ('1.1.3',    'INVENTARIOS',                     'ACTIVO',     'DEUDORA',   FALSE),
    ('1.1.3.01', 'Inventario de Mercaderias',       'ACTIVO',     'DEUDORA',   TRUE),
    ('2',        'PASIVO',                          'PASIVO',     'ACREEDORA', FALSE),
    ('2.1',      'PASIVO CORRIENTE',                'PASIVO',     'ACREEDORA', FALSE),
    ('2.1.1',    'CUENTAS POR PAGAR',               'PASIVO',     'ACREEDORA', FALSE),
    ('2.1.1.01', 'Cuentas por Pagar Proveedores',   'PASIVO',     'ACREEDORA', TRUE),
    ('2.1.2',    'OBLIGACIONES FISCALES',           'PASIVO',     'ACREEDORA', FALSE),
    ('2.1.2.01', 'Debito Fiscal IVA',               'PASIVO',     'ACREEDORA', TRUE),
    ('2.1.2.02', 'IT por Pagar',                    'PASIVO',     'ACREEDORA', TRUE),
    ('3',        'PATRIMONIO',                      'PATRIMONIO', 'ACREEDORA', FALSE),
    ('3.1',      'CAPITAL',                         'PATRIMONIO', 'ACREEDORA', FALSE),
    ('3.1.1',    'CAPITAL SOCIAL',                  'PATRIMONIO', 'ACREEDORA', FALSE),
    ('3.1.1.01', 'Capital Social Aportado',         'PATRIMONIO', 'ACREEDORA', TRUE),
    ('4',        'INGRESOS',                        'INGRESO',    'ACREEDORA', FALSE),
    ('4.1',      'INGRESOS OPERATIVOS',             'INGRESO',    'ACREEDORA', FALSE),
    ('4.1.1',    'VENTAS',                          'INGRESO',    'ACREEDORA', FALSE),
    ('4.1.1.01', 'Ventas de Productos',             'INGRESO',    'ACREEDORA', TRUE),
    ('5',        'EGRESOS',                         'EGRESO',     'DEUDORA',   FALSE),
    ('5.1',      'COSTOS',                          'EGRESO',     'DEUDORA',   FALSE),
    ('5.1.1',    'COSTO DE VENTAS',                 'EGRESO',     'DEUDORA',   FALSE),
    ('5.1.1.01', 'Costo de Mercaderia Vendida',     'EGRESO',     'DEUDORA',   TRUE),
    ('5.2',      'GASTOS OPERATIVOS',               'EGRESO',     'DEUDORA',   FALSE),
    ('5.2.1',    'GASTOS TRIBUTARIOS',              'EGRESO',     'DEUDORA',   FALSE),
    ('5.2.1.01', 'Impuesto a las Transacciones',    'EGRESO',     'DEUDORA',   TRUE)
) AS v(codigo, nombre, tipo, naturaleza, movimiento)
WHERE NOT EXISTS (SELECT 1 FROM CUENTAS_CONTABLES c WHERE c.CODIGO = v.codigo);

UPDATE CUENTAS_CONTABLES h
SET NIVEL = array_length(string_to_array(h.CODIGO, '.'), 1),
    PADRE_ID = (SELECT p.ID FROM CUENTAS_CONTABLES p
                WHERE h.CODIGO LIKE p.CODIGO || '.%'
                ORDER BY length(p.CODIGO) DESC
                LIMIT 1);

-- Una cuenta con subcuentas es de agrupacion (salvo que ya tenga asientos).
UPDATE CUENTAS_CONTABLES c
SET PERMITE_MOVIMIENTO = FALSE
WHERE c.PERMITE_MOVIMIENTO = TRUE
  AND EXISTS (SELECT 1 FROM CUENTAS_CONTABLES h WHERE h.PADRE_ID = c.ID)
  AND NOT EXISTS (SELECT 1 FROM ASIENTO_DETALLES d WHERE d.CUENTA_CONTABLE_ID = c.ID);

CREATE INDEX IF NOT EXISTS IDX_CUENTAS_CONTABLES_PADRE ON CUENTAS_CONTABLES (PADRE_ID);
CREATE INDEX IF NOT EXISTS IDX_PERIODOS_CONTABLES_GESTION ON PERIODOS_CONTABLES (GESTION);
