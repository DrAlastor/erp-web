"""Orquestador: entrena los 6 modelos de una empresa y registra la ejecucion en ml_ejecucion.

Tolerancia a fallos: si un modelo falla, los demas siguen y ese modelo conserva sus resultados
anteriores (su tabla no se toca). Estado final: OK (ninguno fallo), PARCIAL (alguno fallo) o
ERROR (todos fallaron o fallo la carga de datos).
"""
from __future__ import annotations

import json
import logging
import time
from typing import Callable

from app import datos
from app.db import BaseEmpresa
from app.esquema import asegurar_esquema
from app.modelos import abandono, anomalias, canasta, pronostico, reposicion, segmentacion

log = logging.getLogger("ml.entrenamiento")


def entrenar(base: BaseEmpresa) -> dict:
    asegurar_esquema(base)
    id_ejecucion = base.ejecutar(
        "INSERT INTO ml_ejecucion (estado, inicio) VALUES ('EN_CURSO', CURRENT_TIMESTAMP) RETURNING id"
    )
    metricas: dict = {}
    try:
        hoy = datos.hoy(base)
        ventas_diarias = datos.ventas_diarias(base, hoy)
        compras = datos.compras_de_clientes(base, hoy)
        devoluciones = datos.devoluciones_de_clientes(base, hoy)
    except Exception as e:  # sin datos no hay modelos
        log.exception("Fallo la carga de datos de %s", base.nombre)
        _cerrar(base, id_ejecucion, "ERROR", metricas, type(e).__name__)
        return {"estado": "ERROR", "metricas": metricas}

    pasos: list[tuple[str, str, Callable[[], tuple[list[dict], dict]]]] = [
        ("pronostico", "ml_pronostico", lambda: pronostico.entrenar(ventas_diarias, hoy)),
        (
            "reposicion",
            "ml_sugerencia_compra",
            lambda: reposicion.entrenar(
                ventas_diarias,
                datos.stock_actual(base),
                datos.ventas_recientes_por_producto(base, hoy),
                datos.categorias_de_productos(base),
                hoy,
            ),
        ),
        ("abandono", "ml_riesgo_abandono", lambda: abandono.entrenar(compras, devoluciones, hoy)),
        ("segmentacion", "ml_segmento_cliente", lambda: segmentacion.entrenar(compras, hoy)),
        ("canasta", "ml_asociacion", lambda: canasta.entrenar(datos.lineas_de_ventas(base, hoy))),
        ("anomalias", "ml_anomalia", lambda: anomalias.entrenar(datos.tickets_diarios(base, hoy), hoy)),
    ]
    fallidos = 0
    for nombre, tabla, paso in pasos:
        inicio = time.perf_counter()
        try:
            filas, m = paso()
            base.reemplazar_resultados(tabla, filas)
            m["segundos"] = round(time.perf_counter() - inicio, 2)
            metricas[nombre] = m
            log.info("%s: modelo %s OK (%s filas)", base.nombre, nombre, len(filas))
        except Exception as e:
            fallidos += 1
            metricas[nombre] = {"error": type(e).__name__}
            log.exception("%s: fallo el modelo %s", base.nombre, nombre)

    estado = "OK" if fallidos == 0 else ("ERROR" if fallidos == len(pasos) else "PARCIAL")
    _cerrar(base, id_ejecucion, estado, metricas, None)
    return {"estado": estado, "metricas": metricas, "hoy": str(hoy)}


def _cerrar(base: BaseEmpresa, id_ejecucion: int, estado: str, metricas: dict, error: str | None) -> None:
    base.ejecutar(
        "UPDATE ml_ejecucion SET estado = :estado, fin = CURRENT_TIMESTAMP,"
        " metricas = CAST(:metricas AS jsonb), error = :error WHERE id = :id",
        estado=estado,
        metricas=json.dumps(metricas, ensure_ascii=False, default=str),
        error=error,
        id=id_ejecucion,
    )
