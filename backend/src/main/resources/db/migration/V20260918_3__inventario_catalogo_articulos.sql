-- ===================================================================
-- HU-05 / CU-08: Catalogo de articulos (Enrique)
-- La tabla 'categorias' ya la crea V4 (la comparten HU-05 y HU-06): aca solo
-- se crea el catalogo de articulos, referenciando esa misma tabla.
-- ===================================================================

CREATE TABLE IF NOT EXISTS articulos (
    id              BIGSERIAL PRIMARY KEY,
    sku             VARCHAR(255) NOT NULL UNIQUE,
    nombre          VARCHAR(255) NOT NULL,
    descripcion     VARCHAR(1000),
    precio          NUMERIC(19, 2) NOT NULL,
    stock           INTEGER,
    imagen_url      VARCHAR(512),
    categoria_id    INTEGER REFERENCES categorias (id) ON DELETE SET NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_articulos_categoria_id ON articulos (categoria_id);
CREATE INDEX IF NOT EXISTS idx_articulos_activo ON articulos (activo);
