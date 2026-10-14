export type EstadoCotizacion =
  | 'BORRADOR'
  | 'EMITIDA'
  | 'ACEPTADA'
  | 'CONVERTIDA'
  | 'VENCIDA'
  | 'ANULADA';

export const ESTADOS_COTIZACION: { valor: EstadoCotizacion; etiqueta: string }[] = [
  { valor: 'BORRADOR', etiqueta: 'Borrador' },
  { valor: 'EMITIDA', etiqueta: 'Emitida' },
  { valor: 'ACEPTADA', etiqueta: 'Aceptada' },
  { valor: 'CONVERTIDA', etiqueta: 'Convertida' },
  { valor: 'VENCIDA', etiqueta: 'Vencida' },
  { valor: 'ANULADA', etiqueta: 'Anulada' },
];

export interface CotizacionResumen {
  id: number;
  numeroCotizacion: string;
  fecha: string;
  fechaVencimiento: string;
  clienteId: number;
  clienteRazonSocial: string;
  total: number;
  estado: EstadoCotizacion;
}

export interface CotizacionPage {
  content: CotizacionResumen[];
  totalElements: number;
  totalPages: number;
}

export interface DetalleCotizacion {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
}

export interface Cotizacion {
  id: number;
  numeroCotizacion: string;
  fecha: string;
  validezDias: number;
  fechaVencimiento: string;
  clienteId: number;
  clienteRazonSocial: string;
  clienteNitCi: string;
  vendedorId: number;
  vendedor: string;
  subtotal: number;
  descuento: number;
  descuentoGlobal: number;
  total: number;
  estado: EstadoCotizacion;
  observaciones: string | null;
  modificable: boolean;
  convertible: boolean;
  items: DetalleCotizacion[];
}

export interface ItemCotizacionRequest {
  productoId: number;
  cantidad: number;
  descuento: number | null;
}

export interface CotizacionRequest {
  clienteId: number;
  validezDias: number;
  descuentoGlobal: number | null;
  observaciones: string | null;
  borrador: boolean;
  items: ItemCotizacionRequest[];
}

export interface ProductoCotizable {
  id: number;
  codigo: string;
  nombre: string;
  precio: number;
}

export interface SucursalOpcion {
  id: number;
  nombre: string;
}

export interface OpcionesCotizacion {
  productos: ProductoCotizable[];
  sucursales: SucursalOpcion[];
  descuentoMaximoPorcentaje: number;
}

export interface PedidoGenerado {
  pedidoId: number;
  numeroPedido: string;
  estado: string;
  total: number;
  cotizacionId: number;
  numeroCotizacion: string;
}

/** Renglón del formulario mientras se arma la cotización. */
export interface RenglonFormulario {
  productoId: number | null;
  cantidad: number;
  descuento: number;
}
