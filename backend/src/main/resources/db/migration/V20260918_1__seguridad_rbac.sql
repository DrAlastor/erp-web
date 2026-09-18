-- ===================================================================
-- HU-03 / CU-03 — Gestión de Roles y Permisos (RBAC)
-- Proyecto: ERP Comercial y Contable (UAGRM — Sistemas de Información 2)
-- Sprint 1 — Responsable: Arteaga Silva Geimbert Santiago
--
-- La versión de esta migración lleva fecha a propósito: varios integrantes
-- trabajan en paralelo y una "V2__" compartida haría que Flyway abortara
-- por versión repetida.
--
-- Acá va SOLO el esquema. El catálogo de 24 permisos y la matriz de los 7
-- roles viven en Java (CatalogoPermisos y MatrizRolesDeSistema) porque los
-- roles son por empresa y las empresas nacen en tiempo de ejecución: el
-- aprovisionamiento los siembra en cada empresa a partir de esa definición.
-- ===================================================================

-- -------------------------------------------------------------------
-- Empresa (inquilino) — mínima. Territorio compartido con la CU-02.
-- -------------------------------------------------------------------
CREATE TABLE empresa (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre      VARCHAR(160) NOT NULL,
    nit         VARCHAR(20)  NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_empresa_nit UNIQUE (nit)
);

-- -------------------------------------------------------------------
-- Usuario — mínimo. Territorio compartido con la CU-01 y la CU-02.
-- El email es único en todo el sistema porque el login no pide empresa:
-- la empresa se deduce del usuario.
-- -------------------------------------------------------------------
CREATE TABLE usuario (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id    UUID         NOT NULL REFERENCES empresa (id),
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nombre        VARCHAR(160) NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

CREATE INDEX idx_usuario_empresa ON usuario (empresa_id);

-- -------------------------------------------------------------------
-- Permiso — catálogo global, igual para todas las empresas.
-- Código = MODULO_ACCION (por ejemplo COMERCIAL_ANULAR).
-- -------------------------------------------------------------------
CREATE TABLE permiso (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo      VARCHAR(60)  NOT NULL,
    modulo      VARCHAR(20)  NOT NULL,
    accion      VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    CONSTRAINT uq_permiso_codigo UNIQUE (codigo),
    CONSTRAINT uq_permiso_modulo_accion UNIQUE (modulo, accion)
);

-- -------------------------------------------------------------------
-- Rol — por empresa. Los 7 roles del documento se siembran con
-- es_sistema = TRUE: no se borran, se les edita la matriz de permisos
-- y se pueden activar o desactivar.
-- -------------------------------------------------------------------
CREATE TABLE rol (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id  UUID         NOT NULL REFERENCES empresa (id),
    codigo      VARCHAR(40)  NOT NULL,
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    es_sistema  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_rol_empresa_codigo UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_rol_empresa ON rol (empresa_id);

-- -------------------------------------------------------------------
-- Rol-Permiso — la matriz de permisos de cada rol.
-- -------------------------------------------------------------------
CREATE TABLE rol_permiso (
    rol_id     UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    permiso_id UUID NOT NULL REFERENCES permiso (id),
    CONSTRAINT pk_rol_permiso PRIMARY KEY (rol_id, permiso_id)
);

CREATE INDEX idx_rol_permiso_permiso ON rol_permiso (permiso_id);

-- -------------------------------------------------------------------
-- Usuario-Rol — la asignación de roles, con su rastro de quién y cuándo.
-- La clave primaria compuesta es la que impide asignar dos veces el
-- mismo rol al mismo usuario.
-- -------------------------------------------------------------------
CREATE TABLE usuario_rol (
    usuario_id   UUID NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_id       UUID NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    asignado_por UUID REFERENCES usuario (id),
    asignado_en  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_rol PRIMARY KEY (usuario_id, rol_id)
);

CREATE INDEX idx_usuario_rol_rol ON usuario_rol (rol_id);

-- -------------------------------------------------------------------
-- Bitácora de auditoría — mínima. El diagrama de actividad de la HU-03
-- exige dejar el evento registrado al asignar un rol. La CU-04 del
-- Sprint 3 la extiende y le agrega la consulta.
-- -------------------------------------------------------------------
CREATE TABLE bitacora_auditoria (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    UUID         NOT NULL REFERENCES empresa (id),
    usuario_id    UUID REFERENCES usuario (id),
    accion        VARCHAR(60)  NOT NULL,
    detalle       VARCHAR(500),
    registrado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bitacora_empresa_fecha ON bitacora_auditoria (empresa_id, registrado_en DESC);
