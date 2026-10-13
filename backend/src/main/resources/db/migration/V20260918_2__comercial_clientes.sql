-- ===================================================================
-- HU-04 / CU-04: Directorio de clientes (Javier)
-- El esquema lo administra Flyway: la entidad Cliente mapea 'clientes' y
-- la aplicacion arranca con ddl-auto=validate, asi que la tabla tiene que
-- existir aqui y no crearla Hibernate.
-- ===================================================================

CREATE TABLE IF NOT EXISTS clientes (
    id             BIGSERIAL PRIMARY KEY,
    razon_social   VARCHAR(150) NOT NULL,
    nit_ci         VARCHAR(30)  NOT NULL,
    telefono       VARCHAR(30),
    direccion      VARCHAR(255),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Nulo para conservar clientes historicos que todavia no tienen cuenta.
    usuario_id     BIGINT       UNIQUE REFERENCES usuarios (id)
);

CREATE INDEX IF NOT EXISTS idx_clientes_nit_ci ON clientes (nit_ci);
