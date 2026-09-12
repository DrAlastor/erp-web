-- ===================================================================
-- Migración Inicial de Línea Base
-- Proyecto: ERP Comercial y Contable (UAGRM - Sistemas de Información 2)
-- ===================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS sys_baseline_info (
    id SERIAL PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255) NOT NULL
);

INSERT INTO sys_baseline_info (description) 
VALUES ('Inicialización de arquitectura base ERP - SI2 UAGRM');
