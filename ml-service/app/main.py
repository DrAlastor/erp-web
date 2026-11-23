"""API interna del servicio de ML (solo la llama el backend Spring de NexoERP).

- GET  /salud       sin clave.
- POST /entrenar    encola el entrenamiento de una empresa (202).
- POST /pronostico  pronostico al momento para horizontes mas largos que los precalculados.

Sin /docs ni /openapi.json: el servicio no se publica a internet.
"""
from __future__ import annotations

import logging
import threading
from concurrent.futures import ThreadPoolExecutor
from typing import Literal

from fastapi import Depends, FastAPI, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field

from app import datos, db, entrenamiento
from app.modelos.pronostico import pronosticar_serie, serie_completa
from app.seguridad import verificar_clave

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ml.api")

app = FastAPI(title="NexoERP ML", docs_url=None, redoc_url=None, openapi_url=None)

_ejecutor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="entrenador")  # una empresa a la vez
_en_cola: set[int] = set()
_candado = threading.Lock()


class Empresa(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id_empresa: int = Field(alias="idEmpresa", gt=0)
    codigo_empresa: str = Field(alias="codigoEmpresa", min_length=1, max_length=30, pattern=r"^[A-Za-z0-9_-]+$")


class PedidoPronostico(Empresa):
    ambito: Literal["TOTAL", "SUCURSAL", "CATEGORIA"]
    clave: str = Field(default="TOTAL", max_length=150)
    frecuencia: Literal["MENSUAL", "SEMANAL"]
    horizonte: int = Field(ge=1, le=24)


@app.get("/salud")
def salud() -> dict:
    return {"estado": "ok"}


def _trabajo(empresa: Empresa) -> None:
    with _candado:
        # Se libera al EMPEZAR: un aviso que llegue durante el entrenamiento encola otro.
        _en_cola.discard(empresa.id_empresa)
    try:
        base = db.base_de_empresa(empresa.id_empresa, empresa.codigo_empresa)
        resultado = entrenamiento.entrenar(base)
        log.info("Empresa %s entrenada: %s", empresa.codigo_empresa, resultado["estado"])
    except Exception:
        log.exception("Fallo el entrenamiento de la empresa %s", empresa.codigo_empresa)


@app.post("/entrenar", status_code=status.HTTP_202_ACCEPTED, dependencies=[Depends(verificar_clave)])
def entrenar(empresa: Empresa) -> dict:
    with _candado:
        if empresa.id_empresa in _en_cola:
            return {"encolado": False}
        _en_cola.add(empresa.id_empresa)
    _ejecutor.submit(_trabajo, empresa)
    return {"encolado": True}


@app.post("/pronostico", dependencies=[Depends(verificar_clave)])
def pronostico(pedido: PedidoPronostico) -> dict:
    try:
        base = db.base_de_empresa(pedido.id_empresa, pedido.codigo_empresa)
    except db.TenantNoDisponible as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e)) from e
    hoy = datos.hoy(base)
    ventas = datos.ventas_diarias(base, hoy)
    if pedido.ambito == "SUCURSAL":
        try:
            ventas = ventas[ventas["sucursal_id"] == int(pedido.clave)]
        except ValueError:
            raise HTTPException(status_code=422, detail="La clave de sucursal debe ser numerica")
    elif pedido.ambito == "CATEGORIA":
        ventas = ventas[ventas["categoria"] == pedido.clave]
    resultado = pronosticar_serie(serie_completa(ventas, pedido.frecuencia, hoy), pedido.frecuencia, pedido.horizonte)
    if resultado is None:
        return {"disponible": False, "motivo": "No hay suficientes periodos completos para pronosticar"}
    return {
        "disponible": True,
        "modelo": resultado.modelo,
        "mape": resultado.mape,
        "mapeIngenuo": resultado.mape_ingenuo,
        "puntos": [
            {"periodo": p.date().isoformat(), "valor": round(float(v), 2), "bajo": round(float(b), 2), "alto": round(float(a), 2)}
            for p, v, b, a in zip(resultado.periodos, resultado.valores, resultado.bajo, resultado.alto)
        ],
    }
