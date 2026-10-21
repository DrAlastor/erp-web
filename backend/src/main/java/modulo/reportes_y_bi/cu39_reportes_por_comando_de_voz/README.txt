CU39 - Reportes por Comando de Voz
MÓDULO 7 — Reportes & BI

Descripcion: El usuario dicta una consulta ("ventas de la sucursal Central del mes pasado"),
el cliente la transcribe (Speech-to-Text), el backend la hace interpretar por N8N y devuelve
el reporte listo para graficar o mostrar en tabla.

Endpoints (permiso REPORTES_CONSULTAR: Administrador y Vendedor)
  GET  /api/reportes/voz/opciones     Metricas, agrupaciones, sucursales y frases de ejemplo.
  POST /api/reportes/voz/interpretar  { "texto": "..." } -> interpreta y ejecuta.
  POST /api/reportes/voz/ejecutar     ConsultaReporteVoz -> ejecuta (refinar el reporte).
  POST /api/reportes/voz/exportar     ConsultaReporteVoz -> CSV para Excel.

Flujo
  1. El frontend transcribe la voz y el usuario confirma o corrige el texto.
  2. ReporteVozService envia el texto al webhook de N8N (n8n.reportes-voz.webhook-url) con el
     contexto: fechaReferencia, sucursales, productos y catalogo de metricas.
  3. N8N responde una ConsultaReporteVoz en JSON:
       { "metrica": "VENTAS", "agrupacion": "SUCURSAL", "fechaInicio": "2026-09-01",
         "fechaFin": "2026-09-30", "sucursalId": 1, "productoId": null,
         "soloCriticos": null, "limite": 10 }
     Si N8N no esta configurado, falla o no entiende la frase, se usa InterpreteLocalVoz.
  4. La consulta se valida contra el catalogo (MetricaVoz / AgrupacionVoz) y se ejecuta con
     SQL fijo y parametros (ReporteVozRepository): nada del texto dictado llega al SQL.
  5. La respuesta sugiere la visualizacion: KPI (total), LINEAS (por dia/mes),
     BARRAS (hasta 15 categorias) o TABLA.

Metricas: VENTAS, PRODUCTOS_VENDIDOS, STOCK, MOVIMIENTOS_KARDEX, CUENTAS_POR_COBRAR, COMPRAS.

Tablas: solo lectura de ventas, venta_detalles, productos, categorias, inventario_sucursal,
kardex_movimientos, cuentas_por_cobrar, compras, proveedores, clientes, sucursal, ciudad,
usuario y empleados. El CU no crea tablas: la auditoria de cada comando se escribe en el log
de la aplicacion (logger AUDITORIA.CU39) con usuario, frase, consulta y filas devueltas.

Flujo N8N de referencia: docs/n8n/cu39-reportes-por-voz.workflow.json
