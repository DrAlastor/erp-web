CU39 - Reportes por Comando de Voz
MÓDULO 7 — Reportes & BI

Descripcion: Pantalla "Gestionar Reportes por Voz". Graba la consulta con el microfono
(Web Speech API del navegador, es-BO), muestra la transcripcion para confirmarla, la envia
al backend (/api/reportes/voz) y presenta el resultado como KPI, barras, lineas o tabla, con
opcion de refinar los filtros y exportar a CSV.

- services/reconocimiento-voz.service.ts  Speech-to-Text e indicador de grabacion.
- services/reporte-voz.service.ts         Cliente HTTP del backend.
- components/reportes-por-voz.component   Pantalla principal.
