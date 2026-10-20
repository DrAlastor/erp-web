-- CU05 Gestionar Empresas, Sucursales y Configuracion.
-- Idempotente: se puede ejecutar mas de una vez. Flyway esta desactivado en este proyecto,
-- asi que este script se aplica a mano sobre la base ERP_Asus.

-- Datos generales de la empresa (la tabla base viene de V20260918_1__seguridad_rbac.sql)
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS razon_social      VARCHAR(200);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS direccion         VARCHAR(255);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS telefono          VARCHAR(30);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS email             VARCHAR(120);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS moneda_principal  VARCHAR(10) NOT NULL DEFAULT 'BOB';
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS moneda_secundaria VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS logo_url          TEXT;

-- Parametros de configuracion general, uno por clave y empresa
CREATE TABLE IF NOT EXISTS parametro_sistema (
    id             SERIAL PRIMARY KEY,
    empresa_id     UUID         NOT NULL REFERENCES empresa(id),
    clave          VARCHAR(60)  NOT NULL,
    valor          VARCHAR(255) NOT NULL,
    descripcion    VARCHAR(255),
    actualizado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_parametro_empresa_clave UNIQUE (empresa_id, clave)
);
