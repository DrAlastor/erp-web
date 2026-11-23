-- ============================================================================
-- INTELIGENCIA DE NEGOCIO (CU40): tablas de resultados de Machine Learning.
-- Viven en la base de CADA empresa (database-per-tenant), por eso no llevan
-- empresa_id: el aislamiento lo da la propia base de datos.
-- El servicio ml-service las crea solo (IF NOT EXISTS) antes de entrenar.
-- ============================================================================

-- Bitacora de entrenamientos
CREATE TABLE IF NOT EXISTS ml_ejecucion (
    id        SERIAL PRIMARY KEY,
    estado    VARCHAR(12) NOT NULL CHECK (estado IN ('EN_CURSO', 'OK', 'PARCIAL', 'ERROR')),
    inicio    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fin       TIMESTAMP,
    metricas  JSONB,
    error     TEXT
);
CREATE INDEX IF NOT EXISTS idx_ml_ejecucion_inicio ON ml_ejecucion (inicio DESC);

-- Pronostico de ventas (Bs) por total, sucursal y categoria
CREATE TABLE IF NOT EXISTS ml_pronostico (
    ambito      VARCHAR(10)   NOT NULL CHECK (ambito IN ('TOTAL', 'SUCURSAL', 'CATEGORIA')),
    clave       TEXT          NOT NULL,
    frecuencia  VARCHAR(8)    NOT NULL CHECK (frecuencia IN ('MENSUAL', 'SEMANAL')),
    periodo     DATE          NOT NULL,
    valor       NUMERIC(14,2) NOT NULL,
    bajo        NUMERIC(14,2) NOT NULL,
    alto        NUMERIC(14,2) NOT NULL,
    modelo      TEXT          NOT NULL,
    PRIMARY KEY (ambito, clave, frecuencia, periodo)
);

-- Sugerencia de compra (reposicion) por producto y sucursal
CREATE TABLE IF NOT EXISTS ml_sugerencia_compra (
    producto_id       INTEGER       NOT NULL,
    sucursal_id       INTEGER       NOT NULL,
    demanda_esperada  NUMERIC(12,2) NOT NULL,
    stock_actual      NUMERIC(12,2) NOT NULL,
    stock_seguridad   NUMERIC(12,2) NOT NULL,
    cantidad_sugerida INTEGER       NOT NULL,
    motivo            TEXT          NOT NULL,
    PRIMARY KEY (producto_id, sucursal_id)
);

-- Riesgo de abandono por cliente
CREATE TABLE IF NOT EXISTS ml_riesgo_abandono (
    cliente_id   INTEGER      NOT NULL PRIMARY KEY,
    probabilidad NUMERIC(5,4) NOT NULL,
    nivel        VARCHAR(5)   NOT NULL CHECK (nivel IN ('ALTO', 'MEDIO', 'BAJO')),
    factores     JSONB        NOT NULL
);

-- Segmentacion RFM de clientes
CREATE TABLE IF NOT EXISTS ml_segmento_cliente (
    cliente_id    INTEGER       NOT NULL PRIMARY KEY,
    segmento      TEXT          NOT NULL,
    grupo         INTEGER       NOT NULL,
    recencia_dias INTEGER       NOT NULL,
    frecuencia    INTEGER       NOT NULL,
    monto         NUMERIC(14,2) NOT NULL
);

-- Canasta de mercado: reglas A -> B
CREATE TABLE IF NOT EXISTS ml_asociacion (
    producto_a_id INTEGER       NOT NULL,
    producto_a    TEXT          NOT NULL,
    producto_b_id INTEGER       NOT NULL,
    producto_b    TEXT          NOT NULL,
    soporte       NUMERIC(6,4)  NOT NULL,
    confianza     NUMERIC(6,4)  NOT NULL,
    lift          NUMERIC(10,4) NOT NULL,
    ventas        INTEGER       NOT NULL,
    PRIMARY KEY (producto_a_id, producto_b_id)
);

-- Dias atipicos por sucursal
CREATE TABLE IF NOT EXISTS ml_anomalia (
    fecha          DATE          NOT NULL,
    sucursal_id    INTEGER       NOT NULL,
    metrica        VARCHAR(20)   NOT NULL,
    valor_real     NUMERIC(14,2) NOT NULL,
    valor_esperado NUMERIC(14,2) NOT NULL,
    desviacion     NUMERIC(10,2) NOT NULL,
    gravedad       VARCHAR(5)    NOT NULL CHECK (gravedad IN ('ALTA', 'MEDIA')),
    PRIMARY KEY (fecha, sucursal_id, metrica)
);
