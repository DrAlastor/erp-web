CU35 - Cierre, Arqueo y Consulta de Caja (HU-35)
MÓDULO 6 — Tesorería & Cajas

Descripcion: Backend de la HU-35. Base /api/caja (comparte base con la HU-34 de cu32_cuentas_bancarias_y_cajas):
GET /turnos/{id}/arqueo (vista previa sin guardar), POST /turnos/{id}/cierre (monto contado, diferencia
contado − esperado, observaciones obligatorias si hay diferencia), GET /turnos (historial paginado con
filtros sucursal/estado/fechas; un admin ve todos, el resto solo los suyos) y GET /turnos/{id} (detalle).
ReglasCierreCaja es una clase pura con test unitario. Las conciliaciones bancarias no están implementadas.
