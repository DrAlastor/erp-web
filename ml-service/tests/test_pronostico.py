import datetime as dt

import numpy as np
import pandas as pd

from app.modelos import pronostico
from app.modelos.pronostico import FRECUENCIAS, mape, pronosticar_serie, serie_completa


def _serie_mensual(meses: int, estacional: bool = True) -> pd.Series:
    indice = pd.date_range("2023-01-01", periods=meses, freq="MS")
    t = np.arange(meses)
    valores = 1000 + 20 * t + (400 * np.sin(2 * np.pi * t / 12) if estacional else 0)
    return pd.Series(valores, index=indice, dtype=float)


def test_serie_completa_descarta_periodo_en_curso_y_primero_incompleto():
    df = pd.DataFrame(
        {"fecha": pd.to_datetime(["2026-01-15", "2026-02-10", "2026-03-05", "2026-04-02"]), "importe": [1, 2, 3, 4]}
    )
    s = serie_completa(df, "MENSUAL", dt.date(2026, 4, 20))
    # enero empezo a mitad de mes (se descarta) y abril esta en curso (se descarta)
    assert list(s.index) == [pd.Timestamp("2026-02-01"), pd.Timestamp("2026-03-01")]
    assert list(s.values) == [2.0, 3.0]


def test_serie_completa_rellena_con_ceros():
    df = pd.DataFrame({"fecha": pd.to_datetime(["2026-01-01", "2026-04-01"]), "importe": [5, 7]})
    s = serie_completa(df, "MENSUAL", dt.date(2026, 5, 3))
    assert list(s.values) == [5.0, 0.0, 0.0, 7.0]


def test_serie_semanal_usa_lunes():
    # 9 semanas completas (lunes 5-ene a domingo 8-mar); hoy cae en la semana siguiente.
    df = pd.DataFrame({"fecha": pd.date_range("2026-01-05", periods=63, freq="D"), "importe": 1.0})
    s = serie_completa(df, "SEMANAL", dt.date(2026, 3, 10))
    assert all(p.weekday() == 0 for p in s.index)
    assert (s == 7.0).all()


def test_pocos_datos_devuelve_none():
    assert pronosticar_serie(_serie_mensual(5), "MENSUAL") is None


def test_holt_winters_gana_al_ingenuo_con_estacionalidad_y_tendencia():
    r = pronosticar_serie(_serie_mensual(36), "MENSUAL")
    assert r is not None
    assert r.modelo != "INGENUO"
    assert r.mape < r.mape_ingenuo
    assert len(r.valores) == FRECUENCIAS["MENSUAL"].horizonte
    assert r.periodos[0] == pd.Timestamp("2026-01-01")


def test_intervalo_contiene_valor_y_se_ensancha():
    r = pronosticar_serie(_serie_mensual(30), "MENSUAL", horizonte=12)
    assert np.all(r.bajo <= r.valores) and np.all(r.valores <= r.alto)
    anchos = r.alto - r.bajo
    assert anchos[-1] >= anchos[0]
    assert np.all(r.bajo >= 0)


def test_mape_ignora_periodos_sin_ventas():
    assert mape(np.array([0.0, 100.0]), np.array([50.0, 110.0])) == 0.1
    assert mape(np.array([0.0]), np.array([1.0])) is None


def test_entrenar_genera_total_sucursal_y_categoria(ventas_diarias, hoy):
    filas, metricas = pronostico.entrenar(ventas_diarias, hoy)
    ambitos = {(f["ambito"], f["frecuencia"]) for f in filas}
    assert ("TOTAL", "MENSUAL") in ambitos and ("SUCURSAL", "SEMANAL") in ambitos and ("CATEGORIA", "MENSUAL") in ambitos
    # 1 total + 2 sucursales + 2 categorias, 6 meses y 12 semanas cada una
    assert metricas["filas"] == 5 * 6 + 5 * 12
    assert metricas["mape_total_mensual"] is not None
    assert all(f["valor"] >= 0 and f["bajo"] <= f["valor"] <= f["alto"] for f in filas)
