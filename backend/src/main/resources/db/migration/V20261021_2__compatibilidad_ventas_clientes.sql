-- ===================================================================
-- Compatibilidad con el SQL que ejecutan los inicializadores al arrancar
-- (no lo detecta la comparación de entidades porque es SQL escrito a mano).
--
-- CU19 InicializadorVentas: crea el cliente comodín "Sin Nombre (NIT 0)" sin
-- enviar fecha_creacion, que en ERP_Asus es NOT NULL sin valor por defecto.
-- Idempotente: se puede ejecutar a mano en una base de empresa existente.
-- ===================================================================

ALTER TABLE clientes ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP;

-- Columnas de anulación que hoy agrega el inicializador de la CU19.
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS anulado_por INTEGER;
