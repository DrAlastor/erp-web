"""Riesgo de abandono (supervisado, clasificacion).

Un cliente "abandona" si no compra en los VENTANA_DIAS dias siguientes a una fecha de corte.
Se valida sin mirar el futuro (backtesting con varios cortes) y compiten regresion logistica y
Random Forest; gana el de mayor AUC ROC en un corte que el modelo no vio.
"""
from __future__ import annotations

import datetime as dt

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import precision_score, recall_score, roc_auc_score
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

VENTANA_DIAS = 90
CORTES_ENTRENAMIENTO = 6
MINIMO_ENTRENAMIENTO = 50
MINIMO_PRUEBA = 20

VARIABLES = [
    "recencia_dias",
    "frecuencia",
    "monto",
    "ticket_promedio",
    "antiguedad_dias",
    "proporcion_descuento",
    "proporcion_credito",
    "devoluciones",
]
ETIQUETAS = {
    "recencia_dias": "Dias desde su ultima compra",
    "frecuencia": "Cantidad de compras",
    "monto": "Monto total comprado (Bs)",
    "ticket_promedio": "Ticket promedio (Bs)",
    "antiguedad_dias": "Antiguedad como cliente (dias)",
    "proporcion_descuento": "Proporcion de descuento",
    "proporcion_credito": "Proporcion de compras a credito",
    "devoluciones": "Cantidad de devoluciones",
}


def variables(compras: pd.DataFrame, devoluciones: pd.DataFrame, corte: pd.Timestamp) -> pd.DataFrame:
    """Variables por cliente con lo que se sabia ANTES de la fecha de corte."""
    previas = compras[compras["fecha"] < corte]
    if previas.empty:
        return pd.DataFrame(columns=["cliente_id", *VARIABLES])
    g = previas.groupby("cliente_id")
    tabla = pd.DataFrame(
        {
            "ultima": g["fecha"].max(),
            "primera": g["fecha"].min(),
            "frecuencia": g["venta_id"].nunique(),
            "monto": g["importe"].sum(),
            "descuento": g["descuento"].sum(),
            "lista": g["importe_lista"].sum(),
            "credito": g["credito"].mean(),
        }
    )
    tabla["recencia_dias"] = (corte - tabla["ultima"]).dt.days
    tabla["antiguedad_dias"] = (corte - tabla["primera"]).dt.days
    tabla["ticket_promedio"] = tabla["monto"] / tabla["frecuencia"]
    tabla["proporcion_descuento"] = np.where(tabla["lista"] > 0, tabla["descuento"] / tabla["lista"], 0.0)
    tabla["proporcion_credito"] = tabla["credito"].astype(float)
    if devoluciones is not None and not devoluciones.empty:
        devs = devoluciones[devoluciones["fecha"] < corte].groupby("cliente_id").size()
    else:
        devs = pd.Series(dtype=float)
    tabla["devoluciones"] = devs.reindex(tabla.index).fillna(0).astype(float)
    return tabla.reset_index()[["cliente_id", *VARIABLES]]


def tabla_etiquetada(compras: pd.DataFrame, devoluciones: pd.DataFrame, corte: pd.Timestamp) -> pd.DataFrame:
    x = variables(compras, devoluciones, corte)
    if x.empty:
        return x.assign(abandona=pd.Series(dtype=int))
    fin = corte + pd.Timedelta(days=VENTANA_DIAS)
    vuelven = set(compras[(compras["fecha"] >= corte) & (compras["fecha"] < fin)]["cliente_id"])
    x["abandona"] = (~x["cliente_id"].isin(vuelven)).astype(int)
    return x


def _candidatos() -> dict:
    return {
        "REGRESION_LOGISTICA": make_pipeline(
            StandardScaler(), LogisticRegression(max_iter=1000, class_weight="balanced")
        ),
        "RANDOM_FOREST": RandomForestClassifier(
            n_estimators=300, min_samples_leaf=5, class_weight="balanced", random_state=0, n_jobs=1
        ),
    }


def _importancias(nombre: str, modelo) -> np.ndarray:
    if nombre == "RANDOM_FOREST":
        return np.asarray(modelo.feature_importances_, dtype=float)
    return np.abs(np.asarray(modelo[-1].coef_[0], dtype=float))


def nivel(probabilidad: float) -> str:
    if probabilidad >= 0.7:
        return "ALTO"
    if probabilidad >= 0.4:
        return "MEDIO"
    return "BAJO"


def entrenar(compras: pd.DataFrame, devoluciones: pd.DataFrame, hoy: dt.date) -> tuple[list[dict], dict]:
    if compras.empty:
        return [], {"omitido": "pocos datos"}
    hoy_ts = pd.Timestamp(hoy)
    primera = compras["fecha"].min()
    corte_prueba = hoy_ts - pd.Timedelta(days=VENTANA_DIAS)
    prueba = tabla_etiquetada(compras, devoluciones, corte_prueba)

    partes = []
    for i in range(1, CORTES_ENTRENAMIENTO + 1):
        corte = corte_prueba - pd.Timedelta(days=VENTANA_DIAS * i)
        if corte <= primera:
            break
        partes.append(tabla_etiquetada(compras, devoluciones, corte))
    entrenamiento = pd.concat(partes, ignore_index=True) if partes else pd.DataFrame()

    if (
        len(entrenamiento) < MINIMO_ENTRENAMIENTO
        or len(prueba) < MINIMO_PRUEBA
        or entrenamiento["abandona"].nunique() < 2
        or prueba["abandona"].nunique() < 2
    ):
        return [], {
            "omitido": "pocos datos",
            "filas_entrenamiento": int(len(entrenamiento)),
            "filas_prueba": int(len(prueba)),
        }

    x_ent, y_ent = entrenamiento[VARIABLES].to_numpy(float), entrenamiento["abandona"].to_numpy()
    x_pru, y_pru = prueba[VARIABLES].to_numpy(float), prueba["abandona"].to_numpy()

    resultados = {}
    for nombre, modelo in _candidatos().items():
        modelo.fit(x_ent, y_ent)
        probabilidad = modelo.predict_proba(x_pru)[:, 1]
        prediccion = (probabilidad >= 0.5).astype(int)
        resultados[nombre] = {
            "auc": float(roc_auc_score(y_pru, probabilidad)),
            "precision": float(precision_score(y_pru, prediccion, zero_division=0)),
            "exhaustividad": float(recall_score(y_pru, prediccion, zero_division=0)),
        }
    ganador = max(resultados, key=lambda n: resultados[n]["auc"])

    # Reentrenar con todo lo etiquetado y puntuar a todos los clientes con datos al dia de hoy.
    modelo = _candidatos()[ganador]
    todo = pd.concat([entrenamiento, prueba], ignore_index=True)
    modelo.fit(todo[VARIABLES].to_numpy(float), todo["abandona"].to_numpy())
    actuales = variables(compras, devoluciones, hoy_ts)
    x_hoy = actuales[VARIABLES].to_numpy(float)
    probabilidades = modelo.predict_proba(x_hoy)[:, 1]

    importancia = _importancias(ganador, modelo)
    media = x_hoy.mean(axis=0)
    desvio = x_hoy.std(axis=0)
    desvio[desvio == 0] = 1.0
    contribucion = np.abs((x_hoy - media) / desvio) * importancia

    filas = []
    for i, cliente in enumerate(actuales["cliente_id"]):
        principales = np.argsort(-contribucion[i])[:2]
        factores = [
            {"variable": ETIQUETAS[VARIABLES[j]], "valor": _legible(VARIABLES[j], x_hoy[i, j])}
            for j in principales
        ]
        p = float(probabilidades[i])
        filas.append(
            {"cliente_id": int(cliente), "probabilidad": round(p, 4), "nivel": nivel(p), "factores": factores}
        )

    m = resultados[ganador]
    metricas = {
        "modelo": ganador,
        "auc": round(m["auc"], 4),
        "precision": round(m["precision"], 4),
        "exhaustividad": round(m["exhaustividad"], 4),
        "auc_candidatos": {k: round(v["auc"], 4) for k, v in resultados.items()},
        "filas_entrenamiento": int(len(entrenamiento)),
        "filas_prueba": int(len(prueba)),
        "tasa_abandono_prueba": round(float(y_pru.mean()), 4),
        "clientes_puntuados": len(filas),
        "por_nivel": {n: sum(1 for f in filas if f["nivel"] == n) for n in ("ALTO", "MEDIO", "BAJO")},
    }
    return filas, metricas


def _legible(variable: str, valor: float) -> float:
    if variable.startswith("proporcion"):
        return round(float(valor), 2)
    if variable in ("monto", "ticket_promedio"):
        return round(float(valor), 2)
    return int(round(float(valor)))
