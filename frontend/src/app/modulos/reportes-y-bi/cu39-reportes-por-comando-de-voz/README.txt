CU39 - Reportes por Comando de Voz
MÓDULO 7 — Reportes & BI

Descripcion: Pantalla "Gestionar Reportes por Voz". El usuario conversa con el asistente de
reportes: graba la pregunta con el microfono (Web Speech API del navegador, es-BO), confirma
la transcripcion y la envia a /api/reportes/voz/conversar con el historial de la conversacion.
La respuesta se muestra en el chat, se lee en voz alta (speechSynthesis del navegador) y, si
trae un reporte, se presenta como KPI, barras, lineas o tabla, con opcion de refinar los
filtros y exportar a CSV.

- services/reconocimiento-voz.service.ts  Speech-to-Text e indicador de grabacion.
- services/sintesis-voz.service.ts        Lectura en voz alta de las respuestas.
- services/reporte-voz.service.ts         Cliente HTTP del backend.
- components/reportes-por-voz.component   Pantalla principal (chat + reporte).
