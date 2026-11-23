# ml-service — Inteligencia de Negocio (CU40)

Servicio interno de Machine Learning de NexoERP (Python 3.12 + FastAPI). Entrena 6 modelos con
los datos de cada empresa y deja los resultados **precalculados** en tablas `ml_*` de la base de
esa empresa. El backend Spring los lee y los muestra en la sección **Inteligencia de Negocio**.

| Modelo | Tipo | Técnica | Tabla |
|---|---|---|---|
| Pronóstico de ventas | Supervisado (series de tiempo) | Holt / Holt-Winters / estacional vs. ingenuo, hold-out temporal | `ml_pronostico` |
| Sugerencia de compra | Prescriptivo | Pronóstico de unidades por sucursal×categoría + stock de seguridad 90 % | `ml_sugerencia_compra` |
| Riesgo de abandono | Supervisado (clasificación) | Random Forest vs. regresión logística, backtesting de 90 días | `ml_riesgo_abandono` |
| Segmentación | No supervisado | K-means sobre RFM (k por silueta) | `ml_segmento_cliente` |
| Canasta de mercado | No supervisado | Apriori (mlxtend) | `ml_asociacion` |
| Anomalías | No supervisado | STL + z robusto + Isolation Forest | `ml_anomalia` |

## Arquitectura (adaptada a NexoERP)

```
Angular ──JWT──▶ Spring /api/inteligencia-negocio ──X-Clave-Interna──▶ ml-service (FastAPI)
                        │ lee ml_*                                         │ resuelve la base en ERP_Master
                        ▼                                                  ▼ lee ventas/clientes/stock, escribe ml_*
                 Base de la empresa (ERP_TENANT_x) ◀───────────────────────┘
```

- **Database-per-tenant:** cada empresa tiene su base; el servicio la resuelve en `ERP_Master` con
  la misma regla que el backend (empresa ACTIVA + suscripción vigente). Por eso las tablas `ml_*`
  no llevan `empresa_id`: el aislamiento lo da la base.
- No hay Data Warehouse: los modelos leen las tablas operativas (`app/datos.py`).
- Las tablas `ml_*` se crean solas (`CREATE TABLE IF NOT EXISTS`, `sql/ml_resultados.sql`).
- Si un modelo falla, los demás siguen y ese conserva sus resultados anteriores.
- Seguridad: clave compartida con comparación en tiempo constante, falla cerrado sin clave,
  sin `/docs`, tablas de escritura en lista blanca, SQL siempre parametrizado.

## API

| Método | Ruta | Clave | Respuesta |
|---|---|---|---|
| GET | `/salud` | No | `{"estado":"ok"}` |
| POST | `/entrenar` | Sí | `202 {"encolado": true\|false}` — body `{"idEmpresa", "codigoEmpresa"}` |
| POST | `/pronostico` | Sí | Pronóstico al momento (horizonte 1–24) |

## Puesta en marcha local

```powershell
cd ml-service
py -3.12 -m venv .venv
.\.venv\Scripts\python -m pip install -e ".[dev]"
copy .env.example .env      # completar; la clave debe ser igual a ML_CLAVE_INTERNA del backend
.\.venv\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

En `backend/.env`: `ML_URL=http://localhost:8000` y `ML_CLAVE_INTERNA=<la misma clave>`.

## Datos de demostración

```powershell
.\.venv\Scripts\python scripts\cargar_datos_demo.py --base ERP_TENANT_12          # cargar
.\.venv\Scripts\python scripts\cargar_datos_demo.py --base ERP_TENANT_12 --borrar # quitar
```

Genera ~3 años de ventas de una tienda de tecnología (19 mil ventas, 600 clientes, 3 sucursales)
con tendencia, estacionalidad, abandono, productos complementarios y días atípicos. Todo queda
marcado (`ML-DEMO`, `MLD-*`) para poder borrarlo.

## Pruebas

```powershell
.\.venv\Scripts\python -m pytest
```
