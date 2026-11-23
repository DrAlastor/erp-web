"""Sugerencia de compra (prescriptivo), de arriba hacia abajo.

Un producto suelto vende pocas unidades por mes para pronosticarlo solo; por eso se pronostican
las UNIDADES mensuales por (sucursal, categoria) con el mismo modelo del pronostico de ventas y
se reparte la demanda entre los productos del grupo segun sus ventas recientes.
"""
from __future__ import annotations

import datetime as dt
import math

import pandas as pd

from app.modelos.pronostico import pronosticar_serie, serie_completa

HORIZONTE_MESES = 2
Z_SERVICIO = 1.28  # ~90 % de nivel de servicio
SUAVIZADO = 0.5


def entrenar(
    ventas_diarias: pd.DataFrame,
    stock: pd.DataFrame,
    ventas_recientes: pd.DataFrame,
    categorias: pd.DataFrame,
    hoy: dt.date,
) -> tuple[list[dict], dict]:
    if ventas_diarias.empty or categorias.empty:
        return [], {"omitido": "pocos datos"}

    # Universo: productos vendidos en los ultimos 180 dias + productos con fila de stock.
    recientes = ventas_recientes[["sucursal_id", "producto_id", "unidades"]].copy()
    con_stock = stock[["sucursal_id", "producto_id"]].copy()
    universo = pd.concat([recientes[["sucursal_id", "producto_id"]], con_stock]).drop_duplicates()
    universo = universo.merge(categorias, on="producto_id", how="inner")
    universo = universo.merge(recientes, on=["sucursal_id", "producto_id"], how="left")
    universo = universo.merge(stock, on=["sucursal_id", "producto_id"], how="left")
    universo["unidades"] = universo["unidades"].fillna(0.0).astype(float)
    universo["stock"] = universo["stock"].fillna(0.0).astype(float)

    filas: list[dict] = []
    grupos_sin_datos = 0
    for (sucursal, categoria), productos in universo.groupby(["sucursal_id", "categoria"]):
        historia = ventas_diarias[
            (ventas_diarias["sucursal_id"] == sucursal) & (ventas_diarias["categoria"] == categoria)
        ]
        resultado = pronosticar_serie(
            serie_completa(historia, "MENSUAL", hoy, columna="unidades"), "MENSUAL", HORIZONTE_MESES
        )
        if resultado is None:
            grupos_sin_datos += 1
            continue
        demanda = float(resultado.valores.sum())
        desvio = resultado.rmse * math.sqrt(HORIZONTE_MESES)
        pesos = productos["unidades"] + SUAVIZADO
        participacion = pesos / pesos.sum()
        for (_, p), parte in zip(productos.iterrows(), participacion):
            demanda_producto = demanda * parte
            seguridad = Z_SERVICIO * desvio * parte
            cantidad = math.ceil(round(demanda_producto + seguridad - p["stock"], 6))
            if cantidad <= 0:
                continue
            filas.append(
                {
                    "producto_id": int(p["producto_id"]),
                    "sucursal_id": int(sucursal),
                    "demanda_esperada": round(demanda_producto, 2),
                    "stock_actual": round(float(p["stock"]), 2),
                    "stock_seguridad": round(seguridad, 2),
                    "cantidad_sugerida": int(cantidad),
                    "motivo": (
                        f"Se esperan {demanda_producto:.1f} unidades en {HORIZONTE_MESES} meses "
                        f"y hay {p['stock']:.0f} en stock"
                    ),
                }
            )
    unidades = sum(f["cantidad_sugerida"] for f in filas)
    return filas, {"filas": len(filas), "unidades": unidades, "grupos_sin_datos": grupos_sin_datos}
