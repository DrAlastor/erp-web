"""Consultas de entrada sobre el esquema transaccional de la empresa (base tenant de NexoERP).

NexoERP no tiene Data Warehouse: los modelos leen directamente las tablas operativas
(ventas, venta_detalles, productos, categorias, clientes, inventario_sucursal, notas_credito).
Los modelos reciben DataFrames con nombres de columna fijos, asi que adaptar el servicio a
otro esquema solo requiere cambiar los alias de estas consultas.

Reglas de negocio:
- Una venta cuenta si su estado no es ANULADA.
- El "consumidor final" (venta sin cliente identificado) es el cliente con NIT/CI 0 o
  razon social de tipo "Sin nombre"; se excluye de abandono y segmentacion.
- Los productos sin categoria se agrupan como 'Sin categoria'.
"""
from __future__ import annotations

import datetime as dt

import pandas as pd

from app.db import BaseEmpresa

VENTA_VALIDA = "UPPER(v.estado) <> 'ANULADA'"
CATEGORIA = "COALESCE(NULLIF(TRIM(c.nombre), ''), 'Sin categoria')"
CONSUMIDOR_FINAL = (
    "(TRIM(cl.nit_ci) IN ('0', '00', '000', '0000', '99001', '99002', '99003')"
    " OR cl.razon_social ILIKE '%sin nombre%'"
    " OR cl.razon_social ILIKE '%consumidor final%'"
    " OR UPPER(TRIM(cl.razon_social)) IN ('S/N', 'SN'))"
)


def hoy(base: BaseEmpresa) -> dt.date:
    """Fecha actual segun la base (zona horaria del negocio), no segun el reloj del servidor."""
    return base.escalar("SELECT CURRENT_DATE")


def ventas_diarias(base: BaseEmpresa, hoy_: dt.date) -> pd.DataFrame:
    """fecha, sucursal_id, categoria, importe, unidades (agregado por dia, sucursal y categoria)."""
    df = base.leer(
        f"""
        SELECT CAST(v.fecha AS DATE) AS fecha, v.sucursal_id, {CATEGORIA} AS categoria,
               SUM(d.subtotal) AS importe, SUM(d.cantidad) AS unidades
        FROM ventas v
        JOIN venta_detalles d ON d.venta_id = v.id
        LEFT JOIN productos p ON p.id = d.producto_id
        LEFT JOIN categorias c ON c.id = p.categoria_id
        WHERE {VENTA_VALIDA} AND CAST(v.fecha AS DATE) < :hoy
        GROUP BY 1, 2, 3
        """,
        hoy=hoy_,
    )
    return _fechas(df, ["fecha"], columnas=["fecha", "sucursal_id", "categoria", "importe", "unidades"])


def compras_de_clientes(base: BaseEmpresa, hoy_: dt.date) -> pd.DataFrame:
    """Una fila por venta de cada cliente identificado (sin consumidor final)."""
    df = base.leer(
        f"""
        SELECT v.cliente_id, v.id AS venta_id, CAST(v.fecha AS DATE) AS fecha,
               v.total AS importe, v.descuento AS descuento, v.subtotal AS importe_lista,
               (UPPER(v.tipo_venta) = 'CREDITO') AS credito
        FROM ventas v
        JOIN clientes cl ON cl.id = v.cliente_id
        WHERE {VENTA_VALIDA} AND CAST(v.fecha AS DATE) < :hoy AND NOT {CONSUMIDOR_FINAL}
        """,
        hoy=hoy_,
    )
    return _fechas(
        df,
        ["fecha"],
        columnas=["cliente_id", "venta_id", "fecha", "importe", "descuento", "importe_lista", "credito"],
    )


def devoluciones_de_clientes(base: BaseEmpresa, hoy_: dt.date) -> pd.DataFrame:
    """cliente_id, fecha de cada nota de credito (devolucion) no anulada."""
    df = base.leer(
        """
        SELECT n.cliente_id, n.fecha
        FROM notas_credito n
        WHERE UPPER(n.estado) <> 'ANULADA' AND n.fecha < :hoy
        """,
        hoy=hoy_,
    )
    return _fechas(df, ["fecha"], columnas=["cliente_id", "fecha"])


def lineas_de_ventas(base: BaseEmpresa, hoy_: dt.date) -> pd.DataFrame:
    """venta_id, producto_id, producto (pares distintos) para la canasta de mercado."""
    return base.leer(
        f"""
        SELECT DISTINCT d.venta_id, d.producto_id, p.nombre AS producto
        FROM venta_detalles d
        JOIN ventas v ON v.id = d.venta_id
        JOIN productos p ON p.id = d.producto_id
        WHERE {VENTA_VALIDA} AND CAST(v.fecha AS DATE) < :hoy
        """,
        hoy=hoy_,
    )


def tickets_diarios(base: BaseEmpresa, hoy_: dt.date) -> pd.DataFrame:
    """fecha, sucursal_id, tickets (ventas distintas por dia y sucursal)."""
    df = base.leer(
        f"""
        SELECT CAST(v.fecha AS DATE) AS fecha, v.sucursal_id, COUNT(DISTINCT v.id) AS tickets
        FROM ventas v
        WHERE {VENTA_VALIDA} AND CAST(v.fecha AS DATE) < :hoy
        GROUP BY 1, 2
        """,
        hoy=hoy_,
    )
    return _fechas(df, ["fecha"], columnas=["fecha", "sucursal_id", "tickets"])


def stock_actual(base: BaseEmpresa) -> pd.DataFrame:
    """sucursal_id, producto_id, stock (solo productos y sucursales activos)."""
    return base.leer(
        """
        SELECT i.sucursal_id, i.producto_id, i.stock_disponible AS stock
        FROM inventario_sucursal i
        JOIN productos p ON p.id = i.producto_id AND p.activo = TRUE
        JOIN sucursal s ON s.id = i.sucursal_id AND s.activo = TRUE
        """
    )


def ventas_recientes_por_producto(base: BaseEmpresa, hoy_: dt.date, dias: int = 180) -> pd.DataFrame:
    """sucursal_id, producto_id, categoria, unidades vendidas en los ultimos ``dias``."""
    return base.leer(
        f"""
        SELECT v.sucursal_id, d.producto_id, {CATEGORIA} AS categoria, SUM(d.cantidad) AS unidades
        FROM ventas v
        JOIN venta_detalles d ON d.venta_id = v.id
        JOIN productos p ON p.id = d.producto_id AND p.activo = TRUE
        LEFT JOIN categorias c ON c.id = p.categoria_id
        WHERE {VENTA_VALIDA}
          AND CAST(v.fecha AS DATE) >= CAST(:hoy AS DATE) - CAST(:dias AS INTEGER)
          AND CAST(v.fecha AS DATE) < :hoy
        GROUP BY 1, 2, 3
        """,
        hoy=hoy_,
        dias=dias,
    )


def categorias_de_productos(base: BaseEmpresa) -> pd.DataFrame:
    """producto_id, categoria de los productos activos."""
    return base.leer(
        f"""
        SELECT p.id AS producto_id, {CATEGORIA} AS categoria
        FROM productos p
        LEFT JOIN categorias c ON c.id = p.categoria_id
        WHERE p.activo = TRUE
        """
    )


def _fechas(df: pd.DataFrame, columnas_fecha: list[str], columnas: list[str]) -> pd.DataFrame:
    if df.empty:
        return pd.DataFrame(columns=columnas)
    for c in columnas_fecha:
        df[c] = pd.to_datetime(df[c])
    return df
