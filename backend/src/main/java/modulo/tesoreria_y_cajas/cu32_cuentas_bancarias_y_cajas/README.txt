CU34 - Registrar Movimiento de Caja (HU-34) + apertura mínima de turno (CU32 Apertura de Caja)
MÓDULO 6 — Tesorería & Cajas

Descripcion: Backend de la HU-34. Se reutiliza esta carpeta reservada (la carpeta cu34 ya la ocupa
Cuentas por Pagar por la numeración vieja). Base /api/caja:
GET /opciones, GET /turno-actual (204 si no hay turno), POST /turnos (apertura),
GET y POST /turnos/{id}/movimientos (ingresos/egresos; un egreso en efectivo no supera el disponible).
Reusa las entidades compartidas TurnoCaja, MovimientoCaja y FormaPago de ventas_y_facturacion/compartido.
CalculadoraArqueo (efectivo esperado) y ReglasCaja son clases puras con test unitario; la HU-35
(cu35_arqueos_y_conciliaciones) las reutiliza.
