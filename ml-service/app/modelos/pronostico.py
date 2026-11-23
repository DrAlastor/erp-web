"""Pronostico de ventas (supervisado, series de tiempo).

Por cada serie se prueban varios candidatos (ingenuo, Holt, estacional con tendencia y
Holt-Winters) con validacion hold-out temporal; gana el de menor MAPE sobre periodos que no
vio, se reentrena con toda la historia y se pronostica el horizonte con un intervalo del 80 %.
"""
from __future__ import annotations

import datetime as dt
import math
import warnings
from dataclasses import dataclass, field
from typing import Callable

import numpy as np
import pandas as pd
from statsmodels.tsa.holtwinters import ExponentialSmoothing

Z_80 = 1.2816


@dataclass(frozen=True)
class Frecuencia:
    nombre: str
    alias: str  # frecuencia de pandas
    temporada: int
    prueba: int
    minimo: int
    horizonte: int
    tolerancia_dias: int


FRECUENCIAS: dict[str, Frecuencia] = {
    "MENSUAL": Frecuencia("MENSUAL", "MS", temporada=12, prueba=3, minimo=6, horizonte=6, tolerancia_dias=2),
    "SEMANAL": Frecuencia("SEMANAL", "W-MON", temporada=52, prueba=8, minimo=12, horizonte=12, tolerancia_dias=1),
}


@dataclass
class ResultadoSerie:
    modelo: str
    mape: float | None
    mape_ingenuo: float | None
    rmse: float
    periodos: list[pd.Timestamp]
    valores: np.ndarray
    bajo: np.ndarray
    alto: np.ndarray
    candidatos: dict[str, float | None] = field(default_factory=dict)


def inicio_de_periodo(fechas: pd.Series, frecuencia: str) -> pd.Series:
    f = pd.to_datetime(fechas)
    if frecuencia == "MENSUAL":
        return f.dt.to_period("M").dt.start_time
    return (f - pd.to_timedelta(f.dt.day_of_week, unit="D")).dt.normalize()


def serie_completa(df: pd.DataFrame, frecuencia: str, hoy: dt.date, columna: str = "importe") -> pd.Series:
    """Serie por periodo con ceros en periodos sin ventas y solo periodos COMPLETOS.

    Descarta el periodo en curso y el primero si la historia empezo a mitad de periodo.
    """
    cfg = FRECUENCIAS[frecuencia]
    if df is None or df.empty:
        return pd.Series(dtype=float)
    periodos = inicio_de_periodo(df["fecha"], frecuencia)
    agregado = df.assign(periodo=periodos).groupby("periodo")[columna].sum().astype(float)
    actual = inicio_de_periodo(pd.Series([pd.Timestamp(hoy)]), frecuencia).iloc[0]
    primer = agregado.index.min()
    indice = pd.date_range(primer, actual, freq=cfg.alias)
    serie = agregado.reindex(indice, fill_value=0.0)
    serie = serie[serie.index < actual]
    primera_fecha = pd.to_datetime(df["fecha"]).min()
    if len(serie) and (primera_fecha - primer).days > cfg.tolerancia_dias:
        serie = serie.iloc[1:]
    return serie.astype(float)


# ----------------------------------------------------------------------------- candidatos

def _ingenuo(y: np.ndarray, h: int, temporada: int) -> np.ndarray:
    if len(y) >= temporada:
        ultima = y[-temporada:]
        return np.array([ultima[i % temporada] for i in range(h)], dtype=float)
    return np.repeat(float(y[-1]), h)


def _holt(y: np.ndarray, h: int) -> np.ndarray:
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        modelo = ExponentialSmoothing(
            y, trend="add", damped_trend=True, initialization_method="estimated"
        ).fit()
        return np.asarray(modelo.forecast(h), dtype=float)


def _estacional_con_tendencia(y: np.ndarray, h: int, temporada: int, ventana: int) -> np.ndarray:
    recientes = y[-ventana:].sum()
    anio_anterior = y[-ventana - temporada : -temporada].sum()
    crecimiento = recientes / anio_anterior if anio_anterior > 0 else 1.0
    crecimiento = float(np.clip(crecimiento, 0.5, 2.0))
    return _ingenuo(y, h, temporada) * crecimiento


def _holt_winters(y: np.ndarray, h: int, tipo: str) -> np.ndarray:
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        modelo = ExponentialSmoothing(
            y,
            trend="add",
            damped_trend=True,
            seasonal=tipo,
            seasonal_periods=12,
            initialization_method="estimated",
        ).fit()
        return np.asarray(modelo.forecast(h), dtype=float)


def candidatos(y: np.ndarray, cfg: Frecuencia) -> dict[str, Callable[[np.ndarray, int], np.ndarray]]:
    """Candidatos disponibles segun la cantidad de datos de entrenamiento."""
    n = len(y)
    ventana = cfg.prueba
    c: dict[str, Callable[[np.ndarray, int], np.ndarray]] = {
        "INGENUO": lambda s, h: _ingenuo(s, h, cfg.temporada),
    }
    if n >= 10:
        c["HOLT"] = _holt
    if n >= cfg.temporada + ventana:
        c["ESTACIONAL_CON_TENDENCIA"] = lambda s, h: _estacional_con_tendencia(s, h, cfg.temporada, ventana)
    if cfg.nombre == "MENSUAL" and n >= 24:
        c["HOLT_WINTERS_ADITIVO"] = lambda s, h: _holt_winters(s, h, "add")
        if np.all(y > 0):
            c["HOLT_WINTERS_MULTIPLICATIVO"] = lambda s, h: _holt_winters(s, h, "mul")
    return c


def mape(real: np.ndarray, prediccion: np.ndarray) -> float | None:
    """Error porcentual absoluto medio, solo sobre periodos con ventas > 0."""
    mascara = real > 0
    if not mascara.any():
        return None
    return float(np.mean(np.abs(real[mascara] - prediccion[mascara]) / real[mascara]))


# ----------------------------------------------------------------------------- serie

def pronosticar_serie(serie: pd.Series, frecuencia: str, horizonte: int | None = None) -> ResultadoSerie | None:
    """Elige el mejor candidato con hold-out temporal y pronostica ``horizonte`` periodos.

    Devuelve None si la serie no tiene el minimo de periodos completos.
    """
    cfg = FRECUENCIAS[frecuencia]
    h = horizonte or cfg.horizonte
    if serie is None or len(serie) < cfg.minimo:
        return None
    y = serie.to_numpy(dtype=float)
    entrenamiento, prueba = y[: -cfg.prueba], y[-cfg.prueba :]

    errores: dict[str, tuple[float | None, float, np.ndarray]] = {}
    for nombre, funcion in candidatos(entrenamiento, cfg).items():
        try:
            prediccion = np.clip(funcion(entrenamiento, cfg.prueba), 0, None)
            if not np.all(np.isfinite(prediccion)):
                continue
        except Exception:
            continue
        rmse = float(np.sqrt(np.mean((prueba - prediccion) ** 2)))
        errores[nombre] = (mape(prueba, prediccion), rmse, prediccion)

    def puntaje(nombre: str) -> tuple[float, float]:
        m, r, _ = errores[nombre]
        return (m if m is not None else math.inf, r)

    ganador = min(errores, key=puntaje)
    mape_ganador, rmse, _ = errores[ganador]
    mape_ingenuo = errores["INGENUO"][0]

    todos = candidatos(y, cfg)
    try:
        valores = np.clip(todos[ganador](y, h), 0, None)
        if not np.all(np.isfinite(valores)):
            raise ValueError("pronostico no finito")
    except Exception:
        ganador = "INGENUO"
        valores = np.clip(_ingenuo(y, h, cfg.temporada), 0, None)

    pasos = np.arange(h)
    margen = Z_80 * rmse * np.sqrt(1 + pasos / cfg.prueba)
    bajo = np.clip(valores - margen, 0, None)
    alto = valores + margen
    ultimo = serie.index[-1]
    periodos = list(pd.date_range(ultimo, periods=h + 1, freq=cfg.alias)[1:])
    return ResultadoSerie(
        modelo=ganador,
        mape=mape_ganador,
        mape_ingenuo=mape_ingenuo,
        rmse=rmse,
        periodos=periodos,
        valores=valores,
        bajo=bajo,
        alto=alto,
        candidatos={k: (None if v[0] is None else round(v[0], 4)) for k, v in errores.items()},
    )


# ----------------------------------------------------------------------------- todas las series

def series_de_la_empresa(ventas_diarias: pd.DataFrame) -> list[tuple[str, str, pd.DataFrame]]:
    """(ambito, clave, datos) para la serie TOTAL, una por sucursal y una por categoria."""
    series: list[tuple[str, str, pd.DataFrame]] = [("TOTAL", "TOTAL", ventas_diarias)]
    for sucursal, datos in ventas_diarias.groupby("sucursal_id"):
        series.append(("SUCURSAL", str(int(sucursal)), datos))
    for categoria, datos in ventas_diarias.groupby("categoria"):
        series.append(("CATEGORIA", str(categoria), datos))
    return series


def entrenar(ventas_diarias: pd.DataFrame, hoy: dt.date) -> tuple[list[dict], dict]:
    filas: list[dict] = []
    omitidas = 0
    elegidos: dict[str, int] = {}
    metricas: dict = {}
    for frecuencia in ("MENSUAL", "SEMANAL"):
        for ambito, clave, datos in series_de_la_empresa(ventas_diarias):
            resultado = pronosticar_serie(serie_completa(datos, frecuencia, hoy), frecuencia)
            if resultado is None:
                omitidas += 1
                continue
            elegidos[resultado.modelo] = elegidos.get(resultado.modelo, 0) + 1
            if ambito == "TOTAL" and frecuencia == "MENSUAL":
                metricas["mape_total_mensual"] = _redondear(resultado.mape)
                metricas["mape_ingenuo_total_mensual"] = _redondear(resultado.mape_ingenuo)
                metricas["modelo_total_mensual"] = resultado.modelo
                metricas["candidatos_total_mensual"] = resultado.candidatos
            for periodo, valor, bajo, alto in zip(
                resultado.periodos, resultado.valores, resultado.bajo, resultado.alto
            ):
                filas.append(
                    {
                        "ambito": ambito,
                        "clave": clave,
                        "frecuencia": frecuencia,
                        "periodo": periodo.date(),
                        "valor": round(float(valor), 2),
                        "bajo": round(float(bajo), 2),
                        "alto": round(float(alto), 2),
                        "modelo": resultado.modelo,
                    }
                )
    metricas.update(
        {"filas": len(filas), "series_omitidas_por_pocos_datos": omitidas, "modelos_elegidos": elegidos}
    )
    return filas, metricas


def _redondear(valor: float | None) -> float | None:
    return None if valor is None else round(float(valor), 4)
