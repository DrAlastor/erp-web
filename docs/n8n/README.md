# N8N — CU39 Reportes por comando de voz

`cu39-reportes-por-voz.workflow.json` es el flujo que interpreta las frases dictadas.
El backend le envía la frase y recibe una consulta estructurada que después valida y
ejecuta. N8N nunca toca la base de datos.

## Instalación

1. En N8N: **Workflows → Import from File** y elegir `cu39-reportes-por-voz.workflow.json`.
2. (Recomendado) En el nodo **Webhook ERP**, configurar *Authentication → Header Auth* con
   el encabezado `X-ERP-Token` y un valor secreto.
3. Activar el workflow y copiar la *Production URL* del webhook.
4. En el backend, definir las variables de entorno:

   ```
   N8N_REPORTES_VOZ_URL=https://<tu-n8n>/webhook/cu39-reportes-voz
   N8N_REPORTES_VOZ_TOKEN=<el mismo valor del paso 2>
   N8N_REPORTES_VOZ_TIMEOUT_MS=8000
   ```

Si `N8N_REPORTES_VOZ_URL` está vacía, o N8N falla o tarda más que el timeout, el backend
usa su intérprete local y lo indica en el reporte ("Intérprete local").

## Contrato

Petición (`POST`, JSON):

```json
{
  "texto": "muéstrame las ventas de la sucursal Central del mes pasado",
  "fechaReferencia": "2026-10-21",
  "sucursales": [{ "id": 1, "nombre": "Sucursal Central Equipetrol", "ciudad": "Santa Cruz de la Sierra" }],
  "productos": [{ "id": 2, "nombre": "Laptop ASUS TUF Gaming A15 FA507", "codigo": "LAP-TUF-A15" }],
  "metricas": [{ "codigo": "VENTAS", "usaFechas": true, "admiteProducto": false,
                 "admiteCriticos": false, "agrupaciones": ["NINGUNA", "DIA", "MES", "SUCURSAL", "CLIENTE", "VENDEDOR"] }]
}
```

Respuesta esperada:

```json
{ "metrica": "VENTAS", "agrupacion": null, "fechaInicio": "2026-09-01", "fechaFin": "2026-09-30",
  "sucursalId": 1, "productoId": null, "soloCriticos": null, "limite": null }
```

`metrica: null` significa que no se entendió la frase. El nodo **Interpretar comando** se
puede cambiar por un agente de IA (OpenAI, Gemini, etc.) siempre que devuelva este mismo
JSON: el backend descarta cualquier valor que no esté en su catálogo.
