"""Segmentacion de clientes (no supervisado, clustering K-means sobre RFM).

Se prueba k = 3..6 y se elige el de mayor coeficiente de silueta. Cada grupo recibe un nombre de
negocio segun reglas sobre su centro (recencia, frecuencia, monto y descuento).
"""
from __future__ import annotations

import datetime as dt

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler

MINIMO_CLIENTES = 20
VALORES_K = (3, 4, 5, 6)


def tabla_rfm(compras: pd.DataFrame, hoy: dt.date) -> pd.DataFrame:
    hoy_ts = pd.Timestamp(hoy)
    g = compras.groupby("cliente_id")
    rfm = pd.DataFrame(
        {
            "recencia_dias": (hoy_ts - g["fecha"].max()).dt.days,
            "frecuencia": g["venta_id"].nunique(),
            "monto": g["importe"].sum(),
            "descuento": g["descuento"].sum(),
            "lista": g["importe_lista"].sum(),
        }
    )
    rfm["proporcion_descuento"] = np.where(rfm["lista"] > 0, rfm["descuento"] / rfm["lista"], 0.0)
    return rfm.drop(columns=["descuento", "lista"]).reset_index()


def nombrar_segmento(centro: pd.Series, referencia: pd.DataFrame) -> str:
    mediana_r = referencia["recencia_dias"].median()
    if centro["recencia_dias"] > max(180, 2 * mediana_r):
        return "Inactivos"
    if (
        centro["frecuencia"] >= referencia["frecuencia"].quantile(0.75)
        and centro["monto"] >= referencia["monto"].quantile(0.75)
    ):
        return "Frecuentes de alto gasto"
    if centro["proporcion_descuento"] >= 0.15:
        return "Cazadores de descuentos"
    if centro["recencia_dias"] <= mediana_r and centro["monto"] >= referencia["monto"].median():
        return "Activos de buen gasto"
    if centro["recencia_dias"] <= mediana_r:
        return "Recientes ocasionales"
    return "En riesgo"


def entrenar(compras: pd.DataFrame, hoy: dt.date) -> tuple[list[dict], dict]:
    if compras.empty:
        return [], {"omitido": "pocos datos"}
    rfm = tabla_rfm(compras, hoy)
    if len(rfm) < MINIMO_CLIENTES:
        return [], {"omitido": "pocos datos", "clientes": int(len(rfm))}

    x = np.column_stack(
        [rfm["recencia_dias"], np.log1p(rfm["frecuencia"]), np.log1p(rfm["monto"].clip(lower=0))]
    ).astype(float)
    x = StandardScaler().fit_transform(x)

    mejor = None
    siluetas = {}
    for k in VALORES_K:
        if k >= len(rfm):
            break
        modelo = KMeans(n_clusters=k, n_init=10, random_state=0).fit(x)
        if len(set(modelo.labels_)) < 2:
            continue
        s = float(silhouette_score(x, modelo.labels_))
        siluetas[k] = round(s, 4)
        if mejor is None or s > mejor[1]:
            mejor = (k, s, modelo.labels_)
    if mejor is None:
        return [], {"omitido": "pocos datos", "clientes": int(len(rfm))}

    k, silueta, etiquetas = mejor
    rfm["grupo"] = etiquetas
    centros = rfm.groupby("grupo")[["recencia_dias", "frecuencia", "monto", "proporcion_descuento"]].mean()
    nombres: dict[int, str] = {}
    usados: dict[str, int] = {}
    # Primero los grupos de mayor monto, para que un nombre repetido quede en el grupo mas valioso.
    for grupo in centros.sort_values("monto", ascending=False).index:
        nombre = nombrar_segmento(centros.loc[grupo], rfm)
        usados[nombre] = usados.get(nombre, 0) + 1
        nombres[int(grupo)] = nombre if usados[nombre] == 1 else f"{nombre} ({usados[nombre]})"
    rfm["segmento"] = rfm["grupo"].map(nombres)

    filas = [
        {
            "cliente_id": int(r.cliente_id),
            "segmento": r.segmento,
            "grupo": int(r.grupo),
            "recencia_dias": int(r.recencia_dias),
            "frecuencia": int(r.frecuencia),
            "monto": round(float(r.monto), 2),
        }
        for r in rfm.itertuples()
    ]
    return filas, {
        "k": int(k),
        "silueta": round(silueta, 4),
        "siluetas": siluetas,
        "clientes": len(filas),
        "tamanios": rfm["segmento"].value_counts().to_dict(),
    }
