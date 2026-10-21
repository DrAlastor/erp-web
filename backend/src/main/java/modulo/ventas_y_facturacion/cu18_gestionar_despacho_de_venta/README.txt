CU18 - Gestionar Despacho de Venta
MÓDULO 3 — Ventas & Facturación (con Inventario)
Responsable: Santiago

OJO numeración: en el documento del sprint CU18 es "Gestionar Despacho de Venta";
la carpeta cu18_gestionar_facturacion es anterior y quedó sin tocar.

API: /api/despachos (permisos INVENTARIO_*)
  GET    /pedidos-pendientes   pedidos aprobados con lo pendiente y el stock      INVENTARIO_CONSULTAR
  GET    /                     listado de notas (search, estado, pedidoId)        INVENTARIO_CONSULTAR
  GET    /{id}                 detalle                                            INVENTARIO_CONSULTAR
  POST   /                     registrar nota en PREPARACION (no mueve stock)     INVENTARIO_CREAR
  POST   /{id}/confirmar       descuenta stock + SALIDA en Kardex (vía CU11)      INVENTARIO_MODIFICAR
  POST   /{id}/entregar        DESPACHADO -> ENTREGADO                            INVENTARIO_MODIFICAR
  POST   /{id}/anular          con motivo; si ya salió, ENTRADA de reversión      INVENTARIO_ANULAR

Si falta stock responde 409 con la lista "faltantes" y no despacha nada.
Al confirmar publica el evento DespachoConfirmado (y DespachoAnulado al anular): es el
enganche para el asiento de costo de venta cuando exista la CU26.

Tablas: NOTAS_DESPACHO, DESPACHO_DETALLES, INVENTARIO_SUCURSAL, KARDEX_MOVIMIENTOS
(esquema ERP_Asus, sin cambios).

PROVISIONAL: compartido/controller/PedidoProvisionalController permite aprobar pedidos
(POST /api/pedidos/{id}/aprobar) mientras no exista la CU17. Se borra cuando llegue.
