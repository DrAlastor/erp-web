export type EstadoPedido = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'COMPLETADO';

export interface PedidoResumen {
  id: number;
  numeroPedido: string;
  fecha: string;
  clienteId: number;
  clienteNombre: string | null;
  vendedorId: number;
  sucursalId: number;
  subtotal: number;
  descuento: number;
  total: number;
  estado: EstadoPedido | string;
}

export interface LineaPedido {
  productoId: number;
  productoNombre: string;
  productoSku: string | null;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
  stockFisico: number;
  reservado: number;
  disponible: number;
  cubierta: boolean;
}

export interface ReglaValidacion {
  codigo: string;
  nombre: string;
  cumple: boolean;
  /** Si es verdadera y la regla no se cumple, el pedido no se puede aprobar. */
  bloqueante: boolean;
  detalle: string;
}

export interface ValidacionPedido {
  pedido: PedidoResumen;
  observaciones: string | null;
  condicionPago: string | null;
  limiteCredito: number;
  saldoPendiente: number;
  saldoVencido: number;
  creditoDisponible: number;
  lineas: LineaPedido[];
  reglas: ReglaValidacion[];
  puedeAprobar: boolean;
  requiereJustificacion: boolean;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
