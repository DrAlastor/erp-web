-- ===================================================================
-- CU-06 — Personalizar Apariencia
-- Proyecto: ERP Comercial y Contable (UAGRM — Sistemas de Información 2)
--
-- En las bases de empresa (ERP_Asus) la tabla ya existe por el script DDL;
-- acá se crea solo si falta, para las bases locales que arman Flyway.
-- Las columnas nuevas son las mismas de "Base de Datos/CU06_apariencia.sql".
-- ===================================================================

CREATE TABLE IF NOT EXISTS configuracion_empresa (
    id                     SERIAL PRIMARY KEY,
    tema                   VARCHAR(30),
    color_primario         VARCHAR(20),
    color_secundario       VARCHAR(20),
    logo_personalizado_url TEXT,
    encabezado_factura     TEXT,
    pie_factura            TEXT,
    fecha_modificacion     TIMESTAMP
);

ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS color_acento   VARCHAR(20);
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS tamano_fuente  VARCHAR(10);
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS tipografia     VARCHAR(15);
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS bordes         VARCHAR(15);
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS logo           BYTEA;
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS logo_tipo      VARCHAR(30);
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS logo_version   INTEGER NOT NULL DEFAULT 0;
ALTER TABLE configuracion_empresa ADD COLUMN IF NOT EXISTS modificado_por VARCHAR(100);
