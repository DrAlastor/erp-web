-- Índices de alto rendimiento para Libros Contables (CU30) e Información Financiera (CU31)
CREATE INDEX IF NOT EXISTS idx_asiento_detalles_cuenta ON asiento_detalles(cuenta_contable_id);
CREATE INDEX IF NOT EXISTS idx_asiento_detalles_asiento ON asiento_detalles(asiento_id);
CREATE INDEX IF NOT EXISTS idx_asientos_contables_estado_fecha ON asientos_contables(estado, fecha);
CREATE INDEX IF NOT EXISTS idx_asientos_contables_periodo ON asientos_contables(periodo_contable_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_contables_tipo_padre ON cuentas_contables(tipo, padre_id, permite_movimiento);
