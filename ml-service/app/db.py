"""Acceso a datos.

NexoERP es SaaS con una base de datos por empresa (database-per-tenant): la base de cada
empresa se resuelve en ERP_Master con la misma regla que usa el backend Spring
(``TenantDataSourceProvider``). El aislamiento entre empresas lo da la propia base: el servicio
nunca lee ni escribe en una base distinta de la resuelta para el ``idEmpresa`` + ``codigoEmpresa``
recibidos, y ambos deben coincidir con una empresa ACTIVA con suscripcion vigente.
"""
from __future__ import annotations

import datetime as dt
import json
import math
import threading
from decimal import Decimal
from typing import Any, Iterable

import numpy as np
import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine, URL

from app.config import configuracion

# Unicas tablas que el servicio puede escribir. El nombre de tabla nunca viene del exterior.
TABLAS_RESULTADO: frozenset[str] = frozenset(
    {
        "ml_pronostico",
        "ml_sugerencia_compra",
        "ml_riesgo_abandono",
        "ml_segmento_cliente",
        "ml_asociacion",
        "ml_anomalia",
    }
)


class TenantNoDisponible(Exception):
    """La empresa no existe, no esta activa o no tiene base aprovisionada."""


def nativo(valor: Any) -> Any:
    """Convierte tipos de NumPy/pandas a tipos que entiende el driver de PostgreSQL."""
    if valor is None:
        return None
    if isinstance(valor, (dict, list)):
        return json.dumps(valor, ensure_ascii=False, default=nativo)
    if isinstance(valor, (np.integer,)):
        return int(valor)
    if isinstance(valor, (np.floating, float)):
        f = float(valor)
        return None if math.isnan(f) or math.isinf(f) else f
    if isinstance(valor, (np.bool_,)):
        return bool(valor)
    if isinstance(valor, pd.Timestamp):
        return None if pd.isna(valor) else valor.date()
    if isinstance(valor, dt.datetime):
        return valor.date()
    if valor is pd.NaT:
        return None
    if isinstance(valor, Decimal):
        return float(valor)
    return valor


class BaseEmpresa:
    """Conexion a la base de UNA empresa."""

    def __init__(self, motor: Engine, nombre: str = ""):
        self.motor = motor
        self.nombre = nombre

    def leer(self, sql: str, **parametros: Any) -> pd.DataFrame:
        with self.motor.connect() as con:
            resultado = con.execute(text(sql), parametros)
            filas = resultado.fetchall()
            columnas = list(resultado.keys())
        df = pd.DataFrame(filas, columns=columnas)
        for c in df.columns:
            if len(df) and isinstance(df[c].iloc[0], Decimal):
                df[c] = df[c].astype(float)
        return df

    def escalar(self, sql: str, **parametros: Any) -> Any:
        with self.motor.connect() as con:
            return con.execute(text(sql), parametros).scalar()

    def ejecutar(self, sql: str, **parametros: Any) -> Any:
        with self.motor.begin() as con:
            resultado = con.execute(text(sql), parametros)
            try:
                return resultado.scalar()
            except Exception:  # sentencias sin RETURNING
                return None

    def reemplazar_resultados(self, tabla: str, filas: Iterable[dict[str, Any]]) -> int:
        """Reemplaza todo el contenido de una tabla de resultados en UNA transaccion.

        O quedan todos los resultados nuevos o quedan los anteriores.
        """
        if tabla not in TABLAS_RESULTADO:
            raise ValueError(f"Tabla no permitida: {tabla}")
        filas = [{k: nativo(v) for k, v in f.items()} for f in filas]
        with self.motor.begin() as con:
            con.execute(text(f"DELETE FROM {tabla}"))
            if filas:
                columnas = list(filas[0].keys())
                for c in columnas:
                    if not c.isidentifier():
                        raise ValueError(f"Columna invalida: {c}")
                marcadores = ", ".join(
                    f"CAST(:{c} AS jsonb)" if c == "factores" else f":{c}" for c in columnas
                )
                con.execute(
                    text(f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES ({marcadores})"),
                    filas,
                )
        return len(filas)


_candado = threading.Lock()
_motores: dict[str, Engine] = {}
_motor_master: Engine | None = None


def _crear_motor(url: str | URL) -> Engine:
    zona = configuracion().zona_horaria
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_size=2,
        max_overflow=2,
        connect_args={"options": f"-c TimeZone={zona}", "connect_timeout": 15},
    )


def _master() -> Engine:
    global _motor_master
    with _candado:
        if _motor_master is None:
            url = configuracion().master_database_url
            if not url:
                raise TenantNoDisponible("ML_MASTER_DATABASE_URL no esta configurada")
            _motor_master = _crear_motor(url)
        return _motor_master


SQL_TENANT = """
SELECT td.nombre_database, td.host, td.port
FROM empresa e
JOIN tenant_database td ON td.empresa_id = e.id
WHERE e.id = :id_empresa
  AND UPPER(e.codigo_empresa) = UPPER(:codigo)
  AND UPPER(e.estado) = 'ACTIVA'
  AND UPPER(td.estado) IN ('ACTIVA', 'READY')
  AND EXISTS (
      SELECT 1 FROM suscripcion s
      WHERE s.empresa_id = e.id
        AND UPPER(s.estado) = 'ACTIVA'
        AND (s.fecha_fin IS NULL OR s.fecha_fin > CURRENT_TIMESTAMP))
"""


def base_de_empresa(id_empresa: int, codigo_empresa: str) -> BaseEmpresa:
    """Resuelve y devuelve la base de la empresa (mismas reglas que el backend)."""
    with _master().connect() as con:
        fila = con.execute(text(SQL_TENANT), {"id_empresa": id_empresa, "codigo": codigo_empresa}).first()
    if fila is None:
        raise TenantNoDisponible("La empresa no esta activa o no tiene base aprovisionada")
    nombre, host, puerto = fila
    cfg = configuracion()
    if not host or host.lower() == "erp-database":
        host = cfg.tenant_db_host_por_defecto
    url = URL.create(
        "postgresql+psycopg",
        username=cfg.tenant_db_usuario,
        password=cfg.tenant_db_clave,
        host=host,
        port=int(puerto or 5432),
        database=nombre,
        query={"sslmode": cfg.tenant_db_sslmode} if cfg.tenant_db_sslmode else {},
    )
    clave = f"{id_empresa}|{nombre}|{host}|{puerto}"
    with _candado:
        motor = _motores.get(clave)
        if motor is None:
            motor = _crear_motor(url)
            _motores[clave] = motor
    return BaseEmpresa(motor, nombre)
