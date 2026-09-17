-- ===================================================================
-- Migración: Esquema del Módulo de Acceso (Seguridad y Autenticación)
-- HU-01: Iniciar Sesión | HU-02: Gestionar Usuarios | HU-03: Roles y Permisos
-- ===================================================================

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullname VARCHAR(150) NOT NULL,
    enable BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    created_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE permisos (
    id SERIAL PRIMARY KEY,
    modulo VARCHAR(50) NOT NULL,
    pantalla VARCHAR(50) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT uq_permisos_modulo_pantalla_accion UNIQUE (modulo, pantalla, accion)
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE rol_permisos (
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id INTEGER NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE sesion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_origen VARCHAR(45),
    user_agent TEXT,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP
);

CREATE INDEX idx_sesion_usuario_id ON sesion(usuario_id);
CREATE INDEX idx_sesion_refresh_token_hash ON sesion(refresh_token_hash);
CREATE INDEX idx_usuarios_email ON usuarios(email);
