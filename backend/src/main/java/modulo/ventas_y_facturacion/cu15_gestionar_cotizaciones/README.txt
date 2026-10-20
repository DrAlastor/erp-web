CU15 - Gestionar Cotizaciones
MÓDULO 3 — Ventas & Facturación
Responsable: Santiago

API: /api/cotizaciones (permisos COMERCIAL_*)
  GET    /                  listado paginado (search, estado, clienteId)
  GET    /opciones          productos activos con precio, sucursales y tope de descuento
  GET    /{id}              detalle con renglones
  POST   /                  registrar (borrador=true la deja en BORRADOR)      COMERCIAL_CREAR
  PUT    /{id}              modificar (sólo BORRADOR o EMITIDA)                COMERCIAL_MODIFICAR
  POST   /{id}/emitir       emitir un borrador                                 COMERCIAL_CREAR
  POST   /{id}/aceptar      el cliente aceptó                                  COMERCIAL_CREAR
  POST   /{id}/anular       con motivo                                         COMERCIAL_ANULAR
  POST   /{id}/convertir    crea el pedido PENDIENTE (tabla PEDIDOS)           COMERCIAL_CREAR

Reglas: el precio lo pone el servidor desde el catálogo; los descuentos no superan
erp.ventas.descuento-maximo-porcentaje (15 por defecto); vencida = fecha + validez_dias
pasada (se actualiza sola al consultar); la cotización no toca el stock.

Tablas: COTIZACIONES, COTIZACION_DETALLES (esquema ERP_Asus, sin cambios).
El pedido que genera vive en ventas_y_facturacion/compartido (lo usan CU15 a CU18).
