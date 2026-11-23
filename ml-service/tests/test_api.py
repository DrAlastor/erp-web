import threading
import time

import pandas as pd
import pytest
from fastapi.testclient import TestClient

from app import main
from app.config import configuracion
from app.db import BaseEmpresa, TABLAS_RESULTADO, nativo

CLAVE = {"X-Clave-Interna": "clave-de-prueba"}
EMPRESA = {"idEmpresa": 12, "codigoEmpresa": "ASUSTE-FCF5A4"}


@pytest.fixture
def cliente(monkeypatch):
    configuracion.cache_clear()
    monkeypatch.setenv("ML_CLAVE_INTERNA", "clave-de-prueba")
    main._en_cola.clear()
    yield TestClient(main.app)
    configuracion.cache_clear()


def test_salud_sin_clave(cliente):
    assert cliente.get("/salud").json() == {"estado": "ok"}


def test_sin_documentacion_publica(cliente):
    for ruta in ("/docs", "/redoc", "/openapi.json"):
        assert cliente.get(ruta).status_code == 404


@pytest.mark.parametrize("cabeceras", [{}, {"X-Clave-Interna": "otra"}])
def test_entrenar_rechaza_sin_clave_valida(cliente, cabeceras):
    assert cliente.post("/entrenar", json=EMPRESA, headers=cabeceras).status_code == 401


def test_sin_clave_configurada_falla_cerrado(cliente, monkeypatch):
    monkeypatch.setenv("ML_CLAVE_INTERNA", "")
    configuracion.cache_clear()
    assert cliente.post("/entrenar", json=EMPRESA, headers=CLAVE).status_code == 401
    assert cliente.post("/entrenar", json=EMPRESA, headers={"X-Clave-Interna": ""}).status_code == 401


@pytest.mark.parametrize(
    "cuerpo",
    [{"idEmpresa": 0, "codigoEmpresa": "X"}, {"idEmpresa": 1}, {"idEmpresa": 1, "codigoEmpresa": "x'; DROP TABLE--"}],
)
def test_entrenar_valida_el_cuerpo(cliente, cuerpo):
    assert cliente.post("/entrenar", json=cuerpo, headers=CLAVE).status_code == 422


def test_entrenar_responde_202_y_no_encola_dos_veces(cliente, monkeypatch):
    liberar = threading.Event()
    llamadas = []

    def lento(id_empresa, codigo):
        llamadas.append(id_empresa)
        liberar.wait(5)
        raise RuntimeError("sin base en el test")

    monkeypatch.setattr(main.db, "base_de_empresa", lento)
    # Ocupa el hilo con otra empresa para que la 12 quede esperando en cola.
    r0 = cliente.post("/entrenar", json={"idEmpresa": 99, "codigoEmpresa": "OTRA"}, headers=CLAVE)
    time.sleep(0.2)
    r1 = cliente.post("/entrenar", json=EMPRESA, headers=CLAVE)
    r2 = cliente.post("/entrenar", json=EMPRESA, headers=CLAVE)
    assert (r0.status_code, r1.status_code, r2.status_code) == (202, 202, 202)
    assert r1.json() == {"encolado": True} and r2.json() == {"encolado": False}
    liberar.set()
    # Un fallo no rompe el hilo: la empresa 12 tambien se procesa.
    for _ in range(50):
        if 12 in llamadas:
            break
        time.sleep(0.1)
    assert llamadas == [99, 12]
    assert 12 not in main._en_cola


class BaseFalsa(BaseEmpresa):
    def __init__(self, ventas):
        self.ventas = ventas
        self.nombre = "falsa"

    def escalar(self, sql, **p):
        return pd.Timestamp("2026-11-23").date()

    def leer(self, sql, **p):
        return self.ventas.copy()


def test_pronostico_filtra_por_ambito(cliente, monkeypatch, ventas_diarias):
    vistas = {}

    def resolver(id_empresa, codigo):
        vistas["empresa"] = (id_empresa, codigo)
        return BaseFalsa(ventas_diarias)

    monkeypatch.setattr(main.db, "base_de_empresa", resolver)
    cuerpo = {**EMPRESA, "ambito": "SUCURSAL", "clave": "1", "frecuencia": "MENSUAL", "horizonte": 18}
    r = cliente.post("/pronostico", json=cuerpo, headers=CLAVE)
    assert r.status_code == 200, r.text
    datos = r.json()
    assert datos["disponible"] and len(datos["puntos"]) == 18
    assert vistas["empresa"] == (12, "ASUSTE-FCF5A4")

    total = cliente.post("/pronostico", json={**cuerpo, "ambito": "TOTAL"}, headers=CLAVE).json()
    assert total["puntos"][0]["valor"] > datos["puntos"][0]["valor"]

    nada = cliente.post("/pronostico", json={**cuerpo, "ambito": "CATEGORIA", "clave": "No existe"}, headers=CLAVE)
    assert nada.json() == {"disponible": False, "motivo": "No hay suficientes periodos completos para pronosticar"}


def test_pronostico_valida_horizonte(cliente):
    cuerpo = {**EMPRESA, "ambito": "TOTAL", "frecuencia": "MENSUAL", "horizonte": 25}
    assert cliente.post("/pronostico", json=cuerpo, headers=CLAVE).status_code == 422


def test_reemplazar_resultados_rechaza_tablas_fuera_de_la_lista():
    base = BaseEmpresa(motor=None)
    for tabla in ("ventas", "ml_ejecucion", "ml_pronostico; DROP TABLE ventas"):
        with pytest.raises(ValueError):
            base.reemplazar_resultados(tabla, [])
    assert "ml_pronostico" in TABLAS_RESULTADO


def test_nativo_convierte_tipos():
    import numpy as np

    assert nativo(np.int64(3)) == 3 and type(nativo(np.int64(3))) is int
    assert nativo(float("nan")) is None
    assert nativo(pd.Timestamp("2026-01-02")).isoformat() == "2026-01-02"
    assert nativo([{"a": 1}]) == '[{"a": 1}]'
