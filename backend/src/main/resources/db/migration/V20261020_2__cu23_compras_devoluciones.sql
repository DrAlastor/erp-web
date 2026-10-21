-- CU23 Registrar Compra y Devolucion de Proveedores.
-- Idempotente. Flyway esta desactivado: se aplica a mano sobre ERP_Asus.
-- Las tablas proveedores, compras y compra_detalles ya existen en el esquema base
-- (Base de Datos/ERP_Asus.txt, seccion 4). Aqui solo se agrega lo que falta.

-- Vinculo opcional con la orden de compra (CU22) y numero de referencia
ALTER TABLE compras ADD COLUMN IF NOT EXISTS orden_compra_id INTEGER;

-- Devoluciones a proveedor
CREATE TABLE IF NOT EXISTS devoluciones_proveedor (
    id                SERIAL PRIMARY KEY,
    compra_id         INTEGER       NOT NULL REFERENCES compras(id),
    usuario_id        INTEGER       NOT NULL,
    numero_devolucion VARCHAR(50)   NOT NULL UNIQUE,
    fecha             DATE          NOT NULL,
    motivo            VARCHAR(255)  NOT NULL,
    subtotal          NUMERIC(12,2) NOT NULL,
    iva               NUMERIC(12,2) NOT NULL,
    total             NUMERIC(12,2) NOT NULL,
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devolucion_proveedor_detalles (
    id                     SERIAL PRIMARY KEY,
    devolucion_id          INTEGER       NOT NULL REFERENCES devoluciones_proveedor(id) ON DELETE CASCADE,
    compra_detalle_id      INTEGER       NOT NULL REFERENCES compra_detalles(id),
    producto_id            INTEGER       NOT NULL,
    cantidad               NUMERIC(12,2) NOT NULL,
    costo_unitario         NUMERIC(12,2) NOT NULL,
    subtotal               NUMERIC(12,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_devprov_compra ON devoluciones_proveedor (compra_id);
CREATE INDEX IF NOT EXISTS idx_compras_proveedor ON compras (proveedor_id);
