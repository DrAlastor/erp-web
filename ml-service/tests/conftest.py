"""Datos sinteticos con patrones conocidos: los modelos son funciones puras y no necesitan base."""
import datetime as dt
import os
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
os.environ.setdefault("ML_CLAVE_INTERNA", "clave-de-prueba")

HOY = dt.date(2026, 11, 23)


@pytest.fixture
def hoy() -> dt.date:
    return HOY


@pytest.fixture
def ventas_diarias() -> pd.DataFrame:
    """3 anios diarios, 2 sucursales x 2 categorias, con tendencia y pico en diciembre."""
    rng = np.random.default_rng(1)
    fechas = pd.date_range("2023-11-01", pd.Timestamp(HOY) - pd.Timedelta(days=1), freq="D")
    filas = []
    for f in fechas:
        estacion = 1.6 if f.month == 12 else (0.8 if f.month == 2 else 1.0)
        tendencia = 1 + 0.15 * (f - fechas[0]).days / 365
        for suc in (1, 2):
            for cat, base in (("Laptops", 6000.0), ("Accesorios", 900.0)):
                unidades = rng.poisson(3 * estacion * tendencia * (1.2 if suc == 1 else 0.8))
                filas.append((f, suc, cat, unidades * base / 3, float(unidades)))
    return pd.DataFrame(filas, columns=["fecha", "sucursal_id", "categoria", "importe", "unidades"])


@pytest.fixture
def compras() -> pd.DataFrame:
    """300 clientes: la mitad deja de comprar a mitad de la historia (abandono aprendible)."""
    rng = np.random.default_rng(2)
    inicio = pd.Timestamp("2024-01-01")
    fin = pd.Timestamp(HOY) - pd.Timedelta(days=1)
    filas = []
    venta = 1
    for cliente in range(1, 301):
        leal = cliente % 2 == 0
        corte = fin if leal else inicio + pd.Timedelta(days=int(rng.integers(200, 900)))
        cada = int(rng.integers(15, 40)) if leal else int(rng.integers(40, 120))
        f = inicio + pd.Timedelta(days=int(rng.integers(0, 60)))
        while f < min(corte, fin):
            importe = float(rng.gamma(2, 400 if leal else 150))
            descuento = importe * (0.2 if cliente % 7 == 0 else 0.0)
            filas.append((cliente, venta, f, importe - descuento, descuento, importe, bool(cliente % 5 == 0)))
            venta += 1
            f += pd.Timedelta(days=int(rng.integers(max(1, cada - 10), cada + 10)))
    return pd.DataFrame(
        filas, columns=["cliente_id", "venta_id", "fecha", "importe", "descuento", "importe_lista", "credito"]
    )


@pytest.fixture
def devoluciones(compras: pd.DataFrame) -> pd.DataFrame:
    muestra = compras.sample(40, random_state=3)
    return muestra[["cliente_id", "fecha"]].reset_index(drop=True)
