import numpy as np
import pandas as pd

from app.modelos import abandono, anomalias, canasta, reposicion, segmentacion


# ----------------------------------------------------------------------------- abandono

def test_abandono_aprende_y_puntua_a_todos(compras, devoluciones, hoy):
    filas, m = abandono.entrenar(compras, devoluciones, hoy)
    assert m["auc"] > 0.7, m
    assert m["modelo"] in ("RANDOM_FOREST", "REGRESION_LOGISTICA")
    assert len(filas) == compras["cliente_id"].nunique()
    assert all(0 <= f["probabilidad"] <= 1 for f in filas)
    assert all(len(f["factores"]) == 2 for f in filas)
    assert {f["nivel"] for f in filas} <= {"ALTO", "MEDIO", "BAJO"}


def test_abandono_los_que_dejaron_de_comprar_tienen_mas_riesgo(compras, devoluciones, hoy):
    filas, _ = abandono.entrenar(compras, devoluciones, hoy)
    p = pd.DataFrame(filas).set_index("cliente_id")["probabilidad"]
    leales = p[p.index % 2 == 0].mean()
    perdidos = p[p.index % 2 == 1].mean()
    assert perdidos > leales


def test_abandono_sin_mirar_el_futuro(compras, devoluciones):
    corte = pd.Timestamp("2025-01-01")
    x = abandono.variables(compras, devoluciones, corte)
    previas = compras[compras["fecha"] < corte]
    assert set(x["cliente_id"]) == set(previas["cliente_id"])
    assert (x["recencia_dias"] > 0).all()


def test_abandono_con_pocos_datos_se_omite(compras, devoluciones, hoy):
    filas, m = abandono.entrenar(compras.head(30), devoluciones, hoy)
    assert filas == [] and m["omitido"] == "pocos datos"


def test_nivel():
    assert abandono.nivel(0.9) == "ALTO" and abandono.nivel(0.5) == "MEDIO" and abandono.nivel(0.1) == "BAJO"


# ----------------------------------------------------------------------------- segmentacion

def test_segmentacion_asigna_todos_y_nombra(compras, hoy):
    filas, m = segmentacion.entrenar(compras, hoy)
    assert len(filas) == compras["cliente_id"].nunique()
    assert 3 <= m["k"] <= 6
    assert -1 <= m["silueta"] <= 1
    assert "Inactivos" in {f["segmento"] for f in filas}
    assert sum(m["tamanios"].values()) == len(filas)


def test_segmentacion_pocos_clientes(compras, hoy):
    pocos = compras[compras["cliente_id"] <= 10]
    filas, m = segmentacion.entrenar(pocos, hoy)
    assert filas == [] and m["omitido"] == "pocos datos"


def test_nombrar_segmento_reglas():
    ref = pd.DataFrame({"recencia_dias": [10, 20, 30, 40], "frecuencia": [1, 2, 3, 10], "monto": [10, 20, 30, 500]})
    centro = lambda r, f, m, d=0.0: pd.Series({"recencia_dias": r, "frecuencia": f, "monto": m, "proporcion_descuento": d})
    assert segmentacion.nombrar_segmento(centro(400, 1, 10), ref) == "Inactivos"
    assert segmentacion.nombrar_segmento(centro(10, 10, 500), ref) == "Frecuentes de alto gasto"
    assert segmentacion.nombrar_segmento(centro(30, 2, 20, 0.3), ref) == "Cazadores de descuentos"
    assert segmentacion.nombrar_segmento(centro(10, 2, 40), ref) == "Activos de buen gasto"
    assert segmentacion.nombrar_segmento(centro(10, 1, 5), ref) == "Recientes ocasionales"
    assert segmentacion.nombrar_segmento(centro(60, 1, 5), ref) == "En riesgo"


# ----------------------------------------------------------------------------- canasta

def _lineas(n_ventas=400, seed=4):
    rng = np.random.default_rng(seed)
    filas = []
    for v in range(1, n_ventas + 1):
        if rng.random() < 0.4:  # laptop + mouse muchas veces juntos
            filas += [(v, 1, "Laptop"), (v, 2, "Mouse")]
        else:
            filas.append((v, int(rng.integers(3, 9)), f"P{rng.integers(3, 9)}"))
            if rng.random() < 0.3:
                filas.append((v, int(rng.integers(3, 9)), "X"))
    df = pd.DataFrame(filas, columns=["venta_id", "producto_id", "producto"]).drop_duplicates(["venta_id", "producto_id"])
    return df


def test_canasta_encuentra_el_par_fuerte():
    filas, m = canasta.entrenar(_lineas())
    assert m["reglas"] >= 2
    pares = {(f["producto_a_id"], f["producto_b_id"]) for f in filas}
    assert (1, 2) in pares and (2, 1) in pares
    regla = next(f for f in filas if (f["producto_a_id"], f["producto_b_id"]) == (1, 2))
    assert regla["lift"] > 1.2 and regla["confianza"] > 0.9 and regla["ventas"] >= 5
    assert filas == sorted(filas, key=lambda f: -f["lift"])


def test_canasta_pocas_ventas():
    filas, m = canasta.entrenar(_lineas(n_ventas=10))
    assert filas == [] and m["omitido"] == "pocos datos"


# ----------------------------------------------------------------------------- anomalias

def test_anomalias_detecta_dia_con_triple_de_tickets(hoy):
    rng = np.random.default_rng(5)
    fechas = pd.date_range("2025-06-01", pd.Timestamp(hoy) - pd.Timedelta(days=1), freq="D")
    tickets = rng.poisson(np.where(fechas.weekday == 5, 40, 25)).astype(float)
    dia_raro = pd.Timestamp("2026-03-11")
    tickets[fechas.get_loc(dia_raro)] = 120
    df = pd.DataFrame({"fecha": fechas, "sucursal_id": 1, "tickets": tickets})
    filas, m = anomalias.entrenar(df, hoy)
    fechas_anomalas = {pd.Timestamp(f["fecha"]) for f in filas}
    assert dia_raro in fechas_anomalas
    assert m["anomalias"] <= 6  # no llena la serie de falsos positivos
    raro = next(f for f in filas if pd.Timestamp(f["fecha"]) == dia_raro)
    assert raro["gravedad"] == "ALTA" and raro["valor_real"] > raro["valor_esperado"]


def test_anomalias_pocos_dias(hoy):
    df = pd.DataFrame({"fecha": pd.date_range(end=pd.Timestamp(hoy) - pd.Timedelta(days=1), periods=20), "sucursal_id": 1, "tickets": 5})
    filas, m = anomalias.entrenar(df, hoy)
    assert filas == [] and m["omitido"] == "pocos datos"


# ----------------------------------------------------------------------------- reposicion

def test_reposicion_sugiere_comprar_sin_stock(ventas_diarias, hoy):
    stock = pd.DataFrame({"sucursal_id": [1, 1, 2], "producto_id": [10, 11, 20], "stock": [0.0, 1000.0, 2.0]})
    recientes = pd.DataFrame(
        {"sucursal_id": [1, 1, 2], "producto_id": [10, 11, 20], "categoria": ["Laptops", "Laptops", "Accesorios"], "unidades": [50.0, 5.0, 30.0]}
    )
    categorias = pd.DataFrame({"producto_id": [10, 11, 20], "categoria": ["Laptops", "Laptops", "Accesorios"]})
    filas, m = reposicion.entrenar(ventas_diarias, stock, recientes, categorias, hoy)
    por_producto = {f["producto_id"]: f for f in filas}
    assert 10 in por_producto and por_producto[10]["cantidad_sugerida"] > 0
    assert 11 not in por_producto  # tiene stock de sobra
    assert all(f["cantidad_sugerida"] > 0 for f in filas)
    assert "unidades" in por_producto[10]["motivo"]
    assert m["filas"] == len(filas)
