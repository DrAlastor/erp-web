"""Carga de datos de demostracion para Inteligencia de Negocio (CU40).

Genera ~3 anios de historia comercial realista de una tienda de tecnologia en la base de UNA
empresa (por defecto ERP_TENANT_12, "ASUS Tech Bolivia") para que los 6 modelos tengan datos:

- 2 sucursales nuevas (la 3ra abre en sep-2024), 6 categorias y 34 productos ASUS.
- ~600 clientes con perfiles (corporativo, gamer, estudiante, cazador de ofertas, ocasional);
  una parte deja de comprar (abandono aprendible) + el consumidor final "Sin Nombre".
- Ventas diarias con tendencia (+18 %/anio), estacionalidad (pico en diciembre y en febrero por
  inicio de clases, Black Friday en noviembre) y patron semanal (sabado alto, domingo bajo).
- Productos que se compran juntos (laptop + mouse/mochila, monitor + HDMI, router + cable...).
- Algunos dias atipicos inyectados (ferias, Black Friday) y ~2 % de devoluciones.
- Stock por sucursal (algunos productos bajos para que haya sugerencias de compra).

Todo queda MARCADO para poder borrarlo:
  ventas.numero_autorizacion = 'ML-DEMO', productos.codigo 'MLD-*', clientes.nit_ci 'MLD-*',
  categorias.descripcion = 'ML-DEMO', sucursal.telefono = 'ML-DEMO', notas_credito 'MLD-NC-*'.

Uso (desde ml-service/, con el .env configurado):
  .venv\\Scripts\\python scripts\\cargar_datos_demo.py --base ERP_TENANT_12
  .venv\\Scripts\\python scripts\\cargar_datos_demo.py --base ERP_TENANT_12 --borrar
"""
from __future__ import annotations

import argparse
import datetime as dt
import io
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import psycopg

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from app.config import configuracion  # noqa: E402

MARCA = "ML-DEMO"
INICIO = dt.date(2024, 1, 1)
SEMILLA = 2026

CATEGORIAS = ["Laptops", "Monitores", "Accesorios", "Componentes", "Redes", "Audio"]
POPULARIDAD = {"Laptops": 0.20, "Monitores": 0.12, "Accesorios": 0.30, "Componentes": 0.14, "Redes": 0.13, "Audio": 0.11}

# clave, nombre, categoria, precio Bs, gamer
PRODUCTOS = [
    ("VIVO15", "Laptop ASUS VivoBook 15", "Laptops", 4900, False),
    ("ZEN14", "Laptop ASUS ZenBook 14 OLED", "Laptops", 8200, False),
    ("TUF15", "Laptop ASUS TUF Gaming F15", "Laptops", 9500, True),
    ("ROG16", "Laptop ASUS ROG Strix G16", "Laptops", 14800, True),
    ("EXPB1", "Laptop ASUS ExpertBook B1", "Laptops", 6300, False),
    ("CHR14", "Chromebook ASUS C424", "Laptops", 2900, False),
    ("VA24", "Monitor ASUS VA24 24 pulgadas", "Monitores", 1350, False),
    ("VG27", "Monitor ASUS TUF VG27 27 pulgadas", "Monitores", 2600, True),
    ("PA278", "Monitor ASUS ProArt PA278", "Monitores", 4100, False),
    ("PG27", "Monitor ASUS ROG Swift 27", "Monitores", 5900, True),
    ("M3", "Mouse ASUS TUF M3", "Accesorios", 180, True),
    ("MW201", "Mouse inalambrico ASUS MW201", "Accesorios", 150, False),
    ("K1", "Teclado ASUS TUF K1", "Accesorios", 420, True),
    ("MOCHI", "Mochila ASUS ROG Ranger", "Accesorios", 380, True),
    ("FUNDA", "Funda para laptop 15 pulgadas", "Accesorios", 160, False),
    ("COOL", "Base enfriadora para laptop", "Accesorios", 210, True),
    ("HUB7", "Hub USB-C 7 en 1", "Accesorios", 290, False),
    ("HDMI", "Cable HDMI 2 m", "Accesorios", 60, False),
    ("CAM3", "Webcam ASUS C3", "Accesorios", 420, False),
    ("PAD", "Pad mouse ROG Sheath", "Accesorios", 120, True),
    ("SSD1T", "SSD NVMe 1TB", "Componentes", 620, False),
    ("RAM16", "Memoria RAM 16GB DDR5", "Componentes", 520, False),
    ("RTX46", "Tarjeta grafica ASUS Dual RTX 4060", "Componentes", 3100, True),
    ("B650", "Placa madre ASUS Prime B650", "Componentes", 1450, True),
    ("PSU85", "Fuente ASUS ROG 850W", "Componentes", 1150, True),
    ("AX55", "Router ASUS RT-AX55", "Redes", 690, False),
    ("GT6K", "Router ROG Rapture GT-AX6000", "Redes", 2800, True),
    ("RP56", "Repetidor ASUS RP-AX56", "Redes", 480, False),
    ("USB56", "Adaptador WiFi USB-AX56", "Redes", 330, False),
    ("CAT6", "Cable de red Cat6 5 m", "Redes", 45, False),
    ("DELTA", "Audifonos ROG Delta S", "Audio", 1050, True),
    ("H3", "Audifonos ASUS TUF H3", "Audio", 420, True),
    ("PARL", "Parlante bluetooth ASUS", "Audio", 350, False),
    ("CARNX", "Microfono ROG Carnyx", "Audio", 1300, True),
]

# Si se compra la clave, con probabilidad p tambien se compran estas.
ASOCIACIONES = {
    "VIVO15": [(("MW201",), 0.45), (("FUNDA",), 0.35)],
    "ZEN14": [(("MW201",), 0.40), (("FUNDA",), 0.30), (("HUB7",), 0.30)],
    "EXPB1": [(("MW201",), 0.40), (("FUNDA",), 0.35), (("HUB7",), 0.20)],
    "CHR14": [(("FUNDA",), 0.40), (("MW201",), 0.25)],
    "TUF15": [(("M3",), 0.50), (("MOCHI",), 0.35), (("COOL",), 0.20)],
    "ROG16": [(("M3",), 0.45), (("MOCHI",), 0.40), (("DELTA",), 0.20)],
    "VA24": [(("HDMI",), 0.55)],
    "VG27": [(("HDMI",), 0.50), (("K1",), 0.20)],
    "PA278": [(("HDMI",), 0.45)],
    "PG27": [(("HDMI",), 0.40), (("K1",), 0.25)],
    "K1": [(("M3",), 0.40), (("PAD",), 0.30)],
    "AX55": [(("CAT6",), 0.50), (("RP56",), 0.20)],
    "GT6K": [(("CAT6",), 0.45)],
    "RTX46": [(("PSU85",), 0.40)],
    "B650": [(("RAM16",), 0.60), (("SSD1T",), 0.40)],
    "DELTA": [(("CARNX",), 0.15)],
}

MESES = {1: 0.85, 2: 1.15, 3: 1.05, 4: 0.90, 5: 0.95, 6: 0.90, 7: 1.00, 8: 0.95, 9: 0.95, 10: 1.00, 11: 1.20, 12: 1.45}
SEMANA = {0: 0.90, 1: 0.95, 2: 1.00, 3: 1.00, 4: 1.15, 5: 1.35, 6: 0.55}
# (sucursal_indice, fecha, multiplicador): dias atipicos inyectados
DIAS_RAROS = [
    (0, dt.date(2025, 11, 28), 4.0),  # Black Friday
    (1, dt.date(2026, 2, 7), 4.0),  # feria de regreso a clases
    (0, dt.date(2026, 7, 15), 3.5),  # liquidacion
    (2, dt.date(2026, 9, 20), 5.0),  # feria tecnologica
]

NOMBRES = ["Juan", "Maria", "Carlos", "Ana", "Luis", "Sofia", "Jorge", "Lucia", "Diego", "Valeria", "Miguel",
           "Camila", "Andres", "Daniela", "Fernando", "Gabriela", "Ricardo", "Paola", "Sergio", "Natalia",
           "Alejandro", "Carla", "Mauricio", "Fabiola", "Rodrigo", "Veronica", "Marcelo", "Patricia"]
APELLIDOS = ["Vargas", "Rojas", "Gutierrez", "Mamani", "Quispe", "Fernandez", "Lopez", "Choque", "Flores",
             "Justiniano", "Suarez", "Arteaga", "Paz", "Rivero", "Moreno", "Salvatierra", "Banegas", "Roca",
             "Aguilera", "Vaca", "Montano", "Cuellar", "Saavedra", "Zabala"]
EMPRESAS = ["Soluciones", "Tecnologias", "Consultora", "Inversiones", "Distribuidora", "Servicios", "Estudio"]


@dataclass
class Perfil:
    nombre: str
    proporcion: float
    prob_diaria: float
    prob_abandono: float
    cantidad: tuple[int, int]
    gamer: float
    descuento: float


PERFILES = [
    Perfil("corporativo", 0.08, 1 / 18, 0.15, (2, 4), 0.2, 0.05),
    Perfil("gamer", 0.22, 1 / 40, 0.45, (1, 1), 0.85, 0.0),
    Perfil("estudiante", 0.30, 1 / 80, 0.45, (1, 1), 0.3, 0.0),
    Perfil("cazador", 0.10, 1 / 150, 0.40, (1, 2), 0.4, 0.15),
    Perfil("ocasional", 0.30, 1 / 250, 0.50, (1, 1), 0.4, 0.0),
]


def conectar(base: str) -> psycopg.Connection:
    cfg = configuracion()
    return psycopg.connect(
        host=cfg.tenant_db_host_por_defecto,
        port=5432,
        dbname=base,
        user=cfg.tenant_db_usuario,
        password=cfg.tenant_db_clave,
        sslmode=cfg.tenant_db_sslmode or "prefer",
        options=f"-c TimeZone={cfg.zona_horaria}",
        connect_timeout=20,
    )


def borrar(con: psycopg.Connection) -> None:
    with con.transaction(), con.cursor() as cur:
        cur.execute("DELETE FROM notas_credito_detalles WHERE nota_credito_id IN (SELECT id FROM notas_credito WHERE numero_nota LIKE 'MLD-NC-%')")
        cur.execute("DELETE FROM notas_credito WHERE numero_nota LIKE 'MLD-NC-%'")
        cur.execute("DELETE FROM venta_detalles WHERE venta_id IN (SELECT id FROM ventas WHERE numero_autorizacion = %s)", (MARCA,))
        cur.execute("DELETE FROM ventas WHERE numero_autorizacion = %s", (MARCA,))
        cur.execute("DELETE FROM inventario_sucursal WHERE producto_id IN (SELECT id FROM productos WHERE codigo LIKE 'MLD-%')")
        cur.execute("DELETE FROM productos WHERE codigo LIKE 'MLD-%'")
        cur.execute("DELETE FROM categorias WHERE descripcion = %s", (MARCA,))
        cur.execute("DELETE FROM clientes WHERE nit_ci LIKE 'MLD-%' OR email = 'ml-demo@nexoerp.local'")
        cur.execute("DELETE FROM inventario_sucursal WHERE sucursal_id IN (SELECT id FROM sucursal WHERE telefono = %s)", (MARCA,))
        cur.execute("DELETE FROM sucursal WHERE telefono = %s", (MARCA,))
    print("Datos de demostracion borrados.")


def _copiar(cur, tabla: str, columnas: list[str], filas: list[tuple]) -> None:
    with cur.copy(f"COPY {tabla} ({', '.join(columnas)}) FROM STDIN") as copia:
        for fila in filas:
            copia.write_row(fila)


def _max_id(cur, tabla: str) -> int:
    cur.execute(f"SELECT COALESCE(MAX(id), 0) FROM {tabla}")
    return int(cur.fetchone()[0])


def cargar(con: psycopg.Connection) -> None:
    rng = np.random.default_rng(SEMILLA)
    with con.cursor() as cur:
        cur.execute("SELECT count(*) FROM ventas WHERE numero_autorizacion = %s", (MARCA,))
        if cur.fetchone()[0]:
            raise SystemExit("Ya hay datos de demostracion. Ejecuta primero con --borrar.")
        cur.execute("SELECT CURRENT_DATE")
        hoy = cur.fetchone()[0]
        cur.execute("SELECT id FROM usuario WHERE activo = TRUE ORDER BY id LIMIT 1")
        fila = cur.fetchone()
        if not fila:
            raise SystemExit("La empresa no tiene usuarios activos (se necesita un cajero).")
        cajero = fila[0]
        cur.execute("SELECT id, ciudad_id FROM sucursal WHERE activo = TRUE ORDER BY id LIMIT 1")
        fila = cur.fetchone()
        if not fila:
            raise SystemExit("La empresa no tiene una sucursal activa.")
        sucursal_base, ciudad = fila
        cur.execute("SELECT id FROM unidades_medida ORDER BY id LIMIT 1")
        unidad = cur.fetchone()[0]

    fin = hoy - dt.timedelta(days=1)
    dias = [INICIO + dt.timedelta(days=i) for i in range((fin - INICIO).days + 1)]
    ahora = dt.datetime.now()

    with con.transaction(), con.cursor() as cur:
        # --- sucursales ----------------------------------------------------------------
        cur.execute(
            "INSERT INTO sucursal (ciudad_id, nombre, direccion, telefono, hora_apertura, hora_cierre, activo) VALUES"
            " (%s, 'Sucursal Equipetrol', 'Av. San Martin #1450', %s, '09:00', '21:00', TRUE),"
            " (%s, 'Sucursal Plan 3000', 'Av. Paurito, 3er anillo', %s, '09:00', '20:00', TRUE) RETURNING id",
            (ciudad, MARCA, ciudad, MARCA),
        )
        nuevas = [r[0] for r in cur.fetchall()]
        sucursales = [sucursal_base, *nuevas]
        apertura = {sucursales[0]: INICIO, sucursales[1]: INICIO, sucursales[2]: dt.date(2024, 9, 1)}
        base_cf = {sucursales[0]: 4.2, sucursales[1]: 3.0, sucursales[2]: 2.2}  # consumidor final/dia
        peso_suc = np.array([0.5, 0.3, 0.2])

        # --- categorias y productos --------------------------------------------------------
        cat_id = {}
        for c in CATEGORIAS:
            cur.execute(
                "INSERT INTO categorias (nombre, descripcion, activo, fecha_creacion) VALUES (%s, %s, TRUE, %s) RETURNING id",
                (c, MARCA, ahora),
            )
            cat_id[c] = cur.fetchone()[0]
        prod = {}
        for clave, nombre, cat, precio, gamer in PRODUCTOS:
            cur.execute(
                "INSERT INTO productos (categoria_id, unidad_medida_id, codigo, nombre, descripcion, precio_venta,"
                " costo_compra, stock_minimo, stock_maximo, activo, fecha_creacion)"
                " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, TRUE, %s) RETURNING id",
                (cat_id[cat], unidad, f"MLD-{clave}", nombre, f"{nombre} ({MARCA})", precio,
                 round(precio * 0.72, 2), 3 if precio > 1000 else 8, 60, ahora),
            )
            prod[clave] = {"id": cur.fetchone()[0], "precio": float(precio), "cat": cat, "gamer": gamer, "nombre": nombre}
        por_cat = {c: [k for k, v in prod.items() if v["cat"] == c] for c in CATEGORIAS}

        # --- clientes -----------------------------------------------------------------------
        cur.execute(
            "INSERT INTO clientes (razon_social, nit_ci, email, ciudad, condicion_pago, activo, fecha_creacion)"
            " VALUES ('Cliente Pasante / Sin Nombre', '0', 'ml-demo@nexoerp.local', 'Santa Cruz', 'CONTADO', TRUE, %s)"
            " ON CONFLICT (nit_ci) DO NOTHING RETURNING id",
            (ahora,),
        )
        r = cur.fetchone()
        if r is None:
            cur.execute("SELECT id FROM clientes WHERE nit_ci = '0'")
            r = cur.fetchone()
        consumidor_final = r[0]

        n_clientes = 600
        perfiles = rng.choice(len(PERFILES), size=n_clientes, p=[p.proporcion for p in PERFILES])
        clientes = []
        for i in range(n_clientes):
            perfil = PERFILES[perfiles[i]]
            corporativo = perfil.nombre == "corporativo"
            if corporativo:
                razon = f"{rng.choice(EMPRESAS)} {rng.choice(APELLIDOS)} S.R.L."
            else:
                razon = f"{rng.choice(NOMBRES)} {rng.choice(APELLIDOS)} {rng.choice(APELLIDOS)}"
            suc = sucursales[int(rng.choice(3, p=peso_suc))]
            desde = max(apertura[suc], INICIO + dt.timedelta(days=int(rng.integers(0, 60))) if rng.random() < 0.4
                        else INICIO + dt.timedelta(days=int(rng.integers(0, (fin - INICIO).days - 90))))
            hasta = fin
            if rng.random() < perfil.prob_abandono:
                hasta = min(fin, desde + dt.timedelta(days=int(rng.exponential(330)) + 60))
            clientes.append({"razon": razon, "nit": f"MLD-{100000 + i}", "perfil": perfil, "suc": suc,
                             "desde": desde, "hasta": hasta})
        filas = [(c["razon"], c["nit"], f"70{rng.integers(100000, 999999)}", "Santa Cruz", "CONTADO", True,
                  dt.datetime.combine(c["desde"], dt.time(9))) for c in clientes]
        _copiar(cur, "clientes", ["razon_social", "nit_ci", "telefono", "ciudad", "condicion_pago", "activo", "fecha_creacion"], filas)
        cur.execute("SELECT nit_ci, id FROM clientes WHERE nit_ci LIKE 'MLD-%'")
        ids = dict(cur.fetchall())
        for c in clientes:
            c["id"] = ids[c["nit"]]

        # --- ventas ------------------------------------------------------------------------
        def factor(d: dt.date) -> float:
            tendencia = 1 + 0.18 * (d - INICIO).days / 365
            promo = 1.6 if (d.month == 11 and 20 <= d.day <= 30) else 1.0
            return MESES[d.month] * SEMANA[d.weekday()] * tendencia * promo

        tickets = []  # (fecha, sucursal, cliente_dict|None)
        factores = np.array([factor(d) for d in dias])
        for c in clientes:
            p = c["perfil"]
            activo = np.array([(c["desde"] <= d <= c["hasta"]) for d in dias])
            prob = p.prob_diaria * factores
            if p.nombre == "cazador":
                prob = prob * np.array([6.0 if d.month in (11, 12) else 0.5 for d in dias])
            compra = (rng.random(len(dias)) < prob) & activo
            for i in np.flatnonzero(compra):
                tickets.append((dias[i], c["suc"], c))
        for suc in sucursales:
            for d, f in zip(dias, factores):
                if d < apertura[suc]:
                    continue
                for _ in range(int(rng.poisson(base_cf[suc] * f))):
                    tickets.append((d, suc, None))
        for indice, d, mult in DIAS_RAROS:
            suc = sucursales[indice]
            if d <= fin:
                extra = int(round((base_cf[suc] * factor(d) + 2.0) * (mult - 1)))
                tickets += [(d, suc, None)] * extra
        tickets.sort(key=lambda t: t[0])

        id_venta = _max_id(cur, "ventas")
        id_detalle = _max_id(cur, "venta_detalles")
        cur.execute("SELECT COALESCE(MAX(numero_factura), 0) FROM ventas WHERE numero_autorizacion = %s", (MARCA,))
        factura = int(cur.fetchone()[0])
        ventas, detalles, devolver = [], [], []
        claves = list(prod)
        pesos_cat = np.array([POPULARIDAD[c] for c in CATEGORIAS])
        for d, suc, cliente in tickets:
            perfil = cliente["perfil"] if cliente else None
            gamer = rng.random() < (perfil.gamer if perfil else 0.4)
            cat = CATEGORIAS[int(rng.choice(len(CATEGORIAS), p=pesos_cat))]
            candidatos = [k for k in por_cat[cat] if prod[k]["gamer"] == gamer] or por_cat[cat]
            principal = str(rng.choice(candidatos))
            items = {principal: 1}
            for extras, prob in ASOCIACIONES.get(principal, []):
                if rng.random() < prob:
                    for e in extras:
                        items[e] = 1
            if rng.random() < 0.10:
                items[str(rng.choice(claves))] = 1
            if perfil:
                lo, hi = perfil.cantidad
                multiplicador = int(rng.integers(lo, hi + 1))
                items = {k: v * multiplicador for k, v in items.items()}
            for k in items:
                if prod[k]["cat"] == "Accesorios" and rng.random() < 0.25:
                    items[k] += 1

            id_venta += 1
            factura += 1
            hora = dt.datetime.combine(d, dt.time(int(rng.integers(9, 21)), int(rng.integers(0, 60))))
            subtotal = 0.0
            lineas = []
            for k, cantidad in items.items():
                id_detalle += 1
                precio = prod[k]["precio"]
                linea = round(precio * cantidad, 2)
                subtotal += linea
                lineas.append((id_detalle, id_venta, prod[k]["id"], cantidad, precio, 0, linea))
            tasa = perfil.descuento if perfil else 0.0
            if d.month == 11 and 20 <= d.day <= 30:
                tasa = max(tasa, 0.10)
            descuento = round(subtotal * tasa, 2)
            total = round(subtotal - descuento, 2)
            anulada = rng.random() < 0.01
            cli_id = cliente["id"] if cliente else consumidor_final
            nit = cliente["nit"] if cliente else "0"
            razon = cliente["razon"] if cliente else "Cliente Pasante / Sin Nombre"
            ventas.append((
                id_venta, suc, cli_id, cajero, factura, MARCA, nit, razon, hora, subtotal, descuento,
                round(total * 0.13, 2), round(total * 0.03, 2), total, "CONTADO",
                "ANULADA" if anulada else "EMITIDA", hora, "FACTURA",
                "Anulacion de prueba" if anulada else None, hora if anulada else None, cajero if anulada else None,
            ))
            detalles += lineas
            if cliente and not anulada and rng.random() < 0.02:
                devolver.append((id_venta, cli_id, suc, d, lineas[0]))

        _copiar(cur, "ventas", [
            "id", "sucursal_id", "cliente_id", "cajero_id", "numero_factura", "numero_autorizacion", "nit_cliente",
            "razon_social_cliente", "fecha", "subtotal", "descuento", "iva", "it", "total", "tipo_venta", "estado",
            "fecha_creacion", "tipo_comprobante", "motivo_anulacion", "fecha_anulacion", "anulado_por"], ventas)
        _copiar(cur, "venta_detalles", ["id", "venta_id", "producto_id", "cantidad", "precio_unitario", "descuento", "subtotal"], detalles)

        # --- devoluciones (notas de credito) ---------------------------------------------------
        id_nc = _max_id(cur, "notas_credito")
        id_ncd = _max_id(cur, "notas_credito_detalles")
        notas, notas_det = [], []
        for n, (venta, cli, suc, d, linea) in enumerate(devolver, start=1):
            fecha = min(fin, d + dt.timedelta(days=int(rng.integers(1, 10))))
            id_nc += 1
            id_ncd += 1
            _, _, producto, _, precio, _, _ = linea
            notas.append((id_nc, venta, cli, suc, cajero, f"MLD-NC-{n:05d}", fecha, "DEVOLUCION", precio, "EMITIDA",
                          MARCA, dt.datetime.combine(fecha, dt.time(12))))
            notas_det.append((id_ncd, id_nc, producto, linea[0], 1, precio, precio))
        _copiar(cur, "notas_credito", ["id", "venta_id", "cliente_id", "sucursal_id", "emitido_por", "numero_nota", "fecha",
                                       "motivo", "total", "estado", "observaciones", "fecha_creacion"], notas)
        _copiar(cur, "notas_credito_detalles", ["id", "nota_credito_id", "producto_id", "venta_detalle_id", "cantidad",
                                                "precio_unitario", "subtotal"], notas_det)

        # --- stock por sucursal ---------------------------------------------------------------
        recientes = {}
        limite = fin - dt.timedelta(days=60)
        detalle_por_venta = {}
        for det in detalles:
            detalle_por_venta.setdefault(det[1], []).append(det)
        for v in ventas:
            if v[8].date() >= limite and v[15] == "EMITIDA":
                for det in detalle_por_venta[v[0]]:
                    recientes[(v[1], det[2])] = recientes.get((v[1], det[2]), 0) + det[3]
        inventario = []
        for suc in sucursales:
            for k, p in prod.items():
                mensual = recientes.get((suc, p["id"]), 0) / 2
                stock = int(round(mensual * rng.uniform(0.0, 2.2)))
                inventario.append((suc, p["id"], stock, 3 if p["precio"] > 1000 else 8, ahora))
        _copiar(cur, "inventario_sucursal", ["sucursal_id", "producto_id", "stock_disponible", "stock_minimo", "ultima_actualizacion"], inventario)

        for tabla in ("ventas", "venta_detalles", "notas_credito", "notas_credito_detalles"):
            cur.execute(f"SELECT setval(pg_get_serial_sequence('{tabla}', 'id'), (SELECT MAX(id) FROM {tabla}))")

    print(f"Sucursales nuevas: {len(nuevas)} | Categorias: {len(cat_id)} | Productos: {len(prod)}")
    print(f"Clientes: {len(clientes)} + consumidor final | Ventas: {len(ventas)} | Lineas: {len(detalles)}")
    print(f"Notas de credito: {len(notas)} | Filas de inventario: {len(inventario)} | Rango: {INICIO} a {fin}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", required=True, help="Nombre de la base de la empresa (ej. ERP_TENANT_12)")
    parser.add_argument("--borrar", action="store_true", help="Borra los datos de demostracion")
    args = parser.parse_args()
    if args.base in ("ERP_Master", "postgres"):
        raise SystemExit("Esa no es la base de una empresa.")
    with conectar(args.base) as con:
        if args.borrar:
            borrar(con)
        else:
            cargar(con)


if __name__ == "__main__":
    main()
