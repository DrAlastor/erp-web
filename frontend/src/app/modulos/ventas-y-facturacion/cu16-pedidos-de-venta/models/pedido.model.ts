/** Estados de la tabla PEDIDOS compartidos con CU17 (aprobación) y CU18 (despacho). */
export type EstadoPedido =
  | 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'DESPACHO_PARCIAL' | 'DESPACHADO' | 'COMPLETADO' | 'ANULADO';

export interface PedidoResumen {
  id: number;
  numeroPedido: string;
  fecha: string;
  clienteId: number;
  clienteRazonSocial: string;
  clienteNitCi: string;
  sucursalId: number;
  sucursalNombre: string;
  vendedorId: number | null;
  vendedorNombre: string | null;
  subtotal: number;
  descuento: number;
  total: number;
  estado: EstadoPedido;
  cantidadItems: number;
}

export interface PedidoDetalle {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
}

export interface Pedido extends PedidoResumen {
  observaciones: string | null;
  fechaCreacion: string | null;
  detalles: PedidoDetalle[];
}

export interface PedidoPage {
  content: PedidoResumen[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PedidoDetalleRequest {
  productoId: number;
  cantidad: number;
  precioUnitario?: number;
  descuento?: number;
}

export interface PedidoRequest {
  clienteId: number;
  sucursalId: number;
  fecha?: string;
  observaciones?: string;
  descuento?: number;
  detalles: PedidoDetalleRequest[];
}

export interface ClienteOpcion {
  id: number;
  razonSocial: string;
  nitCi: string;
}

export interface SucursalOpcion {
  id: number;
  nombre: string;
}

export interface ProductoOpcion {
  id: number;
  codigo: string;
  nombre: string;
  precioVenta: number;
  stockDisponible: number;
}

/** Línea editable del formulario. `stock` es null cuando no se conoce para la sucursal actual. */
export interface LineaPedido {
  productoId: number;
  codigo: string;
  nombre: string;
  stock: number | null;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
}

export const ESTADOS_PEDIDO: readonly EstadoPedido[] = [
  'PENDIENTE', 'APROBADO', 'RECHAZADO', 'DESPACHO_PARCIAL', 'DESPACHADO', 'COMPLETADO', 'ANULADO',
];
