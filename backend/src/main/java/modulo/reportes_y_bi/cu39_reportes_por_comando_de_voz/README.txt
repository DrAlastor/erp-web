CU39 - Reportes por Comando de Voz
MÓDULO 7 — Reportes & BI

Descripcion: El usuario conversa por voz con un asistente de reportes ("¿cuanto vendimos el
mes pasado en la Central?", "¿y en La Paz?"). El cliente transcribe la voz (Speech-to-Text),
el backend responde con el asistente Gemini y devuelve un texto para leer en voz alta mas el
reporte listo para graficar o mostrar en tabla.

Endpoints (permiso REPORTES_CONSULTAR: Administrador y Vendedor)
  GET  /api/reportes/voz/opciones   Metricas, agrupaciones, sucursales, ejemplos e iaHabilitada.
  POST /api/reportes/voz/conversar  { mensaje, historial: [{rol, texto}], consultaAnterior }
                                    -> { respuesta, reporte, origen: GEMINI|LOCAL, advertencias }
  POST /api/reportes/voz/ejecutar   ConsultaReporteVoz -> reporte (refinar desde la pantalla).
  POST /api/reportes/voz/exportar   ConsultaReporteVoz -> CSV para Excel.

Flujo con Gemini (function calling)
  1. ReporteVozService arma las instrucciones (fecha de hoy, sucursales, productos y el ultimo
     reporte mostrado) y envia la conversacion a Gemini (AsistenteGeminiCliente).
  2. Gemini no ve la base ni escribe SQL: cuando necesita cifras pide la herramienta
     consultar_reporte con una consulta estructurada (metrica, agrupacion, fechas, sucursal...).
  3. ConsultaVozJson descarta todo valor fuera del catalogo; la consulta se valida y se ejecuta
     con SQL fijo y parametros (ReporteVozRepository).
  4. Gemini recibe un resumen de los datos y redacta una respuesta breve para leer en voz alta.
  Si GEMINI_API_KEY esta vacia, se alcanza el limite gratuito o Gemini falla, responde el
  InterpreteLocalVoz (reglas de palabras clave) y la respuesta lo indica.

Configuracion (variables de entorno)
  GEMINI_API_KEY      API key de Google AI Studio. Nunca en el repositorio.
  GEMINI_MODEL        Por defecto gemini-flash-latest (alias que Google mantiene en el Flash vigente).
  GEMINI_TIMEOUT_MS   Por defecto 20000.

Metricas: VENTAS, PRODUCTOS_VENDIDOS, STOCK, MOVIMIENTOS_KARDEX, CUENTAS_POR_COBRAR, COMPRAS.

Tablas: solo lectura de ventas, venta_detalles, productos, categorias, inventario_sucursal,
kardex_movimientos, cuentas_por_cobrar, compras, proveedores, clientes, sucursal, ciudad,
usuario y empleados. El CU no crea tablas: la auditoria (logger AUDITORIA.CU39) registra cada
mensaje (ASISTENTE_VOZ) y cada reporte ejecutado (REPORTE_VOZ) con usuario, frase y consulta.
