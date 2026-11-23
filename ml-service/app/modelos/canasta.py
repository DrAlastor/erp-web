"""Canasta de mercado (no supervisado, reglas de asociacion con Apriori).

Responde "que productos se compran juntos" a nivel de producto: pares que aparecen juntos en al
menos MINIMO_VENTAS_CON_EL_PAR ventas, con lift >= LIFT_MINIMO.
"""
from __future__ import annotations

import inspect

import pandas as pd
from mlxtend.frequent_patterns import apriori, association_rules

MINIMO_VENTAS = 30
MINIMO_VENTAS_CON_EL_PAR = 5
LIFT_MINIMO = 1.2
CONFIANZA_MINIMA = 0.05


def _reglas(frecuentes: pd.DataFrame, n_transacciones: int) -> pd.DataFrame:
    # mlxtend >= 0.23 pide num_itemsets; versiones anteriores no lo aceptan.
    if "num_itemsets" in inspect.signature(association_rules).parameters:
        return association_rules(
            frecuentes, num_itemsets=n_transacciones, metric="lift", min_threshold=LIFT_MINIMO
        )
    return association_rules(frecuentes, metric="lift", min_threshold=LIFT_MINIMO)


def entrenar(lineas: pd.DataFrame) -> tuple[list[dict], dict]:
    if lineas.empty:
        return [], {"omitido": "pocos datos", "ventas": 0}
    matriz = pd.crosstab(lineas["venta_id"], lineas["producto_id"]).astype(bool)
    n = len(matriz)
    varios = int((matriz.sum(axis=1) > 1).sum())
    if n < MINIMO_VENTAS or varios == 0:
        return [], {"omitido": "pocos datos", "ventas": n, "ventas_con_varios_productos": varios}

    frecuentes = apriori(matriz, min_support=MINIMO_VENTAS_CON_EL_PAR / n, max_len=2, use_colnames=True)
    if frecuentes.empty or frecuentes["itemsets"].map(len).max() < 2:
        return [], {"ventas": n, "ventas_con_varios_productos": varios, "reglas": 0}

    reglas = _reglas(frecuentes, n)
    reglas = reglas[reglas["confidence"] >= CONFIANZA_MINIMA].sort_values("lift", ascending=False)
    nombres = lineas.drop_duplicates("producto_id").set_index("producto_id")["producto"].to_dict()

    filas = []
    for r in reglas.itertuples():
        a = next(iter(r.antecedents))
        b = next(iter(r.consequents))
        filas.append(
            {
                "producto_a_id": int(a),
                "producto_a": str(nombres.get(a, a)),
                "producto_b_id": int(b),
                "producto_b": str(nombres.get(b, b)),
                "soporte": round(float(r.support), 4),
                "confianza": round(float(r.confidence), 4),
                "lift": round(float(r.lift), 4),
                "ventas": int(round(float(r.support) * n)),
            }
        )
    return filas, {"ventas": n, "ventas_con_varios_productos": varios, "reglas": len(filas)}
