"""Tablas de resultados ``ml_*`` dentro de la base de cada empresa.

Se crean con ``CREATE TABLE IF NOT EXISTS`` al inicio de cada entrenamiento: asi funcionan en
empresas ya aprovisionadas y en las nuevas sin tocar el aprovisionamiento. El mismo DDL esta en
``ml-service/sql/ml_resultados.sql`` para crearlas a mano.
"""
from pathlib import Path

from sqlalchemy import text

from app.db import BaseEmpresa

ARCHIVO_DDL = Path(__file__).resolve().parent.parent / "sql" / "ml_resultados.sql"


def sentencias_ddl() -> list[str]:
    lineas = [
        linea
        for linea in ARCHIVO_DDL.read_text(encoding="utf-8").splitlines()
        if not linea.strip().startswith("--")
    ]
    return [s.strip() for s in "\n".join(lineas).split(";") if s.strip()]


def asegurar_esquema(base: BaseEmpresa) -> None:
    with base.motor.begin() as con:
        for s in sentencias_ddl():
            con.execute(text(s))
