-- =====================================================================
-- Sprint 4 (CU25-CU35) - lo que falta en las bases de empresa de AWS RDS
-- Revisado en solo lectura el 2026-11-18 contra main a97192b.
--
-- ESQUEMA: no falta ninguna tabla ni columna del Sprint 4 en ERP_Asus ni en
-- ERP_TENANT_12 (cuentas_contables, periodos_contables, asientos_contables,
-- asiento_detalles, turnos_caja, movimientos_caja, cobros, cuentas_por_cobrar...).
-- FALTAN DATOS E INDICES: los grupos intermedios del plan de cuentas
-- (1.1.2, 1.1.2.01, 2.1.1, 2.1.2, 3.1, 3.1.1, 4.1, 4.1.1, 5.1, 5.1.1, 5.2, 5.2.1
-- en ERP_Asus), el recalculo de NIVEL/PADRE_ID y los indices de CU25/CU30/CU31.
--
-- Donde correrlo: en CADA base de empresa (ERP_Asus primero, luego
-- ERP_Farmacia, ERP_TENANT_*). NUNCA en ERP_Master ni en postgres.
-- Idempotente: se puede correr varias veces. NO probado en copia local.
-- =====================================================================
BEGIN;
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

CREATE INDEX IF NOT EXISTS idx_asiento_detalles_cuenta ON asiento_detalles(cuenta_contable_id);
CREATE INDEX IF NOT EXISTS idx_asiento_detalles_asiento ON asiento_detalles(asiento_id);
CREATE INDEX IF NOT EXISTS idx_asientos_contables_estado_fecha ON asientos_contables(estado, fecha);
CREATE INDEX IF NOT EXISTS idx_asientos_contables_periodo ON asientos_contables(periodo_contable_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_contables_tipo_padre ON cuentas_contables(tipo, padre_id, permite_movimiento);

COMMIT;

-- OPCIONAL (solo ERP_Asus, decidirlo antes de presentar): el usuario id 1 tiene
-- 6 turnos de caja ABIERTOS desde el 22-oct (ids 4-9). Mientras sigan abiertos,
-- "Abrir caja" (CU32) con ese usuario responde "Ya tienes abierto el turno #9".
-- Alternativa sin SQL: cerrarlos desde CU35 en la app.
-- UPDATE turnos_caja SET estado = 'CERRADO', fecha_cierre = now()
--  WHERE estado = 'ABIERTO' AND fecha_apertura < DATE '2026-11-01';
