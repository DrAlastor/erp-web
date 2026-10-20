CU16 - Gestionar Pedidos de Venta
MÓDULO 3 — Ventas & Facturación

Frontend del caso de uso CU16 (HU-16). Se monta en main-layout cuando fn.id === 'cu16-pedidos-de-venta'.

models/pedido.model.ts        DTOs del contrato /api/pedidos (resumen, detalle, request, opciones).
services/pedidos.service.ts   Cliente HTTP: listado paginado, CRUD, anular y opciones.
utils/pedido.utils.ts         Cálculo de subtotales, validaciones espejo del backend y mensajes de error.
components/
  pedidos-venta.component     Listado (búsqueda, filtro por estado, paginación) y orquestación de modales.
  pedido-form.component       Alta / edición (solo PENDIENTE): cliente, sucursal, fecha, líneas, descuentos.
  cliente-selector.component  Buscador de clientes activos.
  pedido-lineas.component     Editor de líneas con búsqueda de productos y stock por sucursal.
  pedido-detalle.component    Detalle con acción Anular (motivo obligatorio). Aprobar/rechazar es de la CU17.
  _pedidos-base.scss          Estilos compartidos (mismos tokens que cu13 / cu11).

Permisos: COMERCIAL_CREAR (nuevo), COMERCIAL_MODIFICAR (editar), COMERCIAL_ANULAR (anular).
