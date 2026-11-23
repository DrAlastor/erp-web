"""Deteccion de anomalias (no supervisado): dias con actividad rara por sucursal.

Se mide la cantidad de ventas (tickets) por dia y sucursal. Pasos: estabilizacion de varianza
(Anscombe), descomposicion STL (tendencia + patron semanal + residuo), z robusto del residuo e
Isolation Forest. Un dia es anomalia si Isolation Forest lo marca Y |z| >= Z_MINIMA.
"""
from __future__ import annotations

import datetime as dt

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from statsmodels.tsa.seasonal import STL

MINIMO_DIAS = 56
Z_MINIMA = 4.5
Z_ALTA = 5.5
CONTAMINACION = 0.03


def detectar(serie: pd.Series) -> pd.DataFrame:
    """Recibe tickets diarios (indice = fecha, sin huecos). Devuelve los dias anomalos."""
    y = np.sqrt(serie.to_numpy(dtype=float) + 3.0 / 8.0)
    descomposicion = STL(y, period=7, seasonal=35, robust=True).fit()
    residuo = descomposicion.resid
    mediana = np.median(residuo)
    mad = np.median(np.abs(residuo - mediana))
    escala = 1.4826 * mad if mad > 0 else (np.std(residuo) or 1.0)
    z = (residuo - mediana) / escala

    bosque = IsolationForest(contamination=CONTAMINACION, random_state=0).fit(z.reshape(-1, 1))
    marcados = bosque.predict(z.reshape(-1, 1)) == -1
    esperado = np.clip((descomposicion.trend + descomposicion.seasonal) ** 2 - 3.0 / 8.0, 0, None)

    anomalos = marcados & (np.abs(z) >= Z_MINIMA)
    return pd.DataFrame(
        {
            "fecha": serie.index[anomalos],
            "valor_real": serie.to_numpy(dtype=float)[anomalos],
            "valor_esperado": esperado[anomalos],
            "desviacion": z[anomalos],
        }
    )


def entrenar(tickets: pd.DataFrame, hoy: dt.date) -> tuple[list[dict], dict]:
    if tickets.empty:
        return [], {"omitido": "pocos datos"}
    ayer = pd.Timestamp(hoy) - pd.Timedelta(days=1)
    filas: list[dict] = []
    analizadas = 0
    for sucursal, datos in tickets.groupby("sucursal_id"):
        diaria = datos.groupby("fecha")["tickets"].sum()
        indice = pd.date_range(diaria.index.min(), ayer, freq="D")
        serie = diaria.reindex(indice, fill_value=0).astype(float)
        if len(serie) < MINIMO_DIAS:
            continue
        analizadas += 1
        for r in detectar(serie).itertuples():
            filas.append(
                {
                    "fecha": r.fecha.date(),
                    "sucursal_id": int(sucursal),
                    "metrica": "TICKETS",
                    "valor_real": round(float(r.valor_real), 2),
                    "valor_esperado": round(float(r.valor_esperado), 2),
                    "desviacion": round(float(r.desviacion), 2),
                    "gravedad": "ALTA" if abs(r.desviacion) >= Z_ALTA else "MEDIA",
                }
            )
    if analizadas == 0:
        return [], {"omitido": "pocos datos"}
    return filas, {"sucursales": analizadas, "anomalias": len(filas)}
