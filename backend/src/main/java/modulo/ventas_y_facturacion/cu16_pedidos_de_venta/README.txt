CU16 - Gestionar Pedidos de Venta (HU-16)
MÓDULO 3 — Ventas & Facturación

Tablas: pedidos, pedido_detalles (entidades JPA "PedidoVenta" / "PedidoVentaDetalle").
Lecturas de clientes, sucursal, productos, inventario_sucursal, usuario y empleados con
SQL propio (PedidoVentaConsultaRepository), sin importar entidades de otros modulos.

Endpoints (/api/pedidos, permisos COMERCIAL):
  GET    /api/pedidos?search=&estado=&page=&size=   CONSULTAR  (Page, id desc)
  GET    /api/pedidos/{id}                          CONSULTAR
  POST   /api/pedidos                               CREAR      -> 201, estado PENDIENTE
  PUT    /api/pedidos/{id}                          MODIFICAR  (solo PENDIENTE)
  PATCH  /api/pedidos/{id}/anular  {motivo}         ANULAR     (PENDIENTE|APROBADO -> ANULADO)
  GET    /api/pedidos/opciones/clientes?search=     CONSULTAR
  GET    /api/pedidos/opciones/sucursales           CONSULTAR
  GET    /api/pedidos/opciones/productos?search=&sucursalId=  CONSULTAR

Reglas: importes recalculados en servidor (CalculadoraPedido, HALF_UP 2 decimales);
stock validado contra inventario_sucursal al crear y modificar, sin descontarlo
(eso corresponde a despacho/facturacion). Numero PED-<yyyy>-<0001>. Sin borrado fisico.
Estados: los de compartido.entity.EstadoPedido (dev-santiago). Aprobar/rechazar = CU17,
despachar = CU18; aqui solo se crea, edita (PENDIENTE) y anula.
Vendedor = usuario autenticado (VendedorActualResolver), nunca un id fijo.
