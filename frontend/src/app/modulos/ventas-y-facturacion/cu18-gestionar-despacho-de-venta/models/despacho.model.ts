export type EstadoDespacho = 'PREPARACION' | 'DESPACHADO' | 'ENTREGADO' | 'ANULADO';

export const ESTADOS_DESPACHO: { valor: EstadoDespacho; etiqueta: string }[] = [
  { valor: 'PREPARACION', etiqueta: 'En preparación' },
  { valor: 'DESPACHADO', etiqueta: 'Despachado' },
  { valor: 'ENTREGADO', etiqueta: 'Entregado' },
  { valor: 'ANULADO', etiqueta: 'Anulado' },
];

export interface PendienteDespacho {
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  pedido: number;
  comprometido: number;
  pendiente: number;
  stockDisponible: number;
}

export interface PedidoPorDespachar {
  pedidoId: number;
  numeroPedido: string;
  fecha: string;
  estado: string;
  clienteId: number;
  clienteRazonSocial: string;
  sucursalId: number;
  sucursalNombre: string;
  items: PendienteDespacho[];
}

export interface DespachoResumen {
  id: number;
  numeroDespacho: string;
  fecha: string;
  pedidoId: number;
  numeroPedido: string;
  clienteRazonSocial: string;
  sucursalNombre: string;
  estado: EstadoDespacho;
}

export interface DespachoPage {
  content: DespachoResumen[];
  totalElements: number;
  totalPages: number;
}

export interface DetalleDespacho {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  cantidad: number;
}

export interface Despacho {
  id: number;
  numeroDespacho: string;
  fecha: string;
  estado: EstadoDespacho;
  pedidoId: number;
  numeroPedido: string;
  estadoPedido: string;
  clienteId: number;
  clienteRazonSocial: string;
  sucursalId: number;
  sucursalNombre: string;
  despachadoPorId: number;
  despachadoPor: string;
  conductor: string | null;
  placa: string | null;
  observaciones: string | null;
  confirmable: boolean;
  entregable: boolean;
  anulable: boolean;
  items: DetalleDespacho[];
}

export interface DespachoRequest {
  pedidoId: number;
  sucursalId: number;
  conductor: string | null;
  placa: string | null;
  observaciones: string | null;
  items: { productoId: number; cantidad: number }[];
}

export interface FaltanteStock {
  productoId: number;
  productoNombre: string;
  requerido: number;
  disponible: number;
}

export interface Sucursal {
  id: number;
  nombre: string;
}

/** Provisional hasta la CU17: pedido esperando la validación comercial. */
export interface PedidoPendienteAprobacion {
  id: number;
  numeroPedido: string;
  fecha: string;
  clienteRazonSocial: string;
  sucursalNombre: string;
  total: number;
  estado: string;
  observaciones: string | null;
}
