-- ============================================================================
-- CU41 - AGENTE DE IA (chat de Inteligencia de Negocio).
-- Viven en la base de CADA empresa (database-per-tenant): no llevan empresa_id.
-- El backend las crea solo (IF NOT EXISTS) la primera vez que se usa el agente.
-- ============================================================================
CREATE TABLE IF NOT EXISTS bi_conversacion (
    id                  SERIAL PRIMARY KEY,
    usuario_id          INTEGER      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    titulo              VARCHAR(100) NOT NULL,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bi_conversacion_usuario ON bi_conversacion (usuario_id, fecha_actualizacion DESC);

CREATE TABLE IF NOT EXISTS bi_mensaje (
    id              SERIAL PRIMARY KEY,
    conversacion_id INTEGER     NOT NULL REFERENCES bi_conversacion(id) ON DELETE CASCADE,
    rol             VARCHAR(10) NOT NULL CHECK (rol IN ('USUARIO', 'ASISTENTE')),
    contenido       TEXT        NOT NULL,
    herramientas    JSONB,
    tokens_entrada  INTEGER,
    tokens_salida   INTEGER,
    fecha           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bi_mensaje_conversacion ON bi_mensaje (conversacion_id, id);
CREATE INDEX IF NOT EXISTS idx_bi_mensaje_fecha ON bi_mensaje (fecha, rol);

CREATE TABLE IF NOT EXISTS bi_grafico (
    id             SERIAL PRIMARY KEY,
    usuario_id     INTEGER      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    titulo         VARCHAR(100) NOT NULL,
    herramienta    VARCHAR(60)  NOT NULL,
    parametros     JSONB        NOT NULL,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bi_grafico_usuario ON bi_grafico (usuario_id, fecha_creacion DESC);
