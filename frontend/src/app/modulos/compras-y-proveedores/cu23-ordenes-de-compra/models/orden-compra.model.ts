import { ProveedorOpcion } from '../../cu21-gestionar-proveedores/models/proveedor.model';

export type EstadoOrdenCompra = 'BORRADOR' | 'PENDIENTE' | 'APROBADA' | 'ENVIADA' | 'RECIBIDA' | 'ANULADA';

export const ESTADOS_ORDEN: { valor: EstadoOrdenCompra; etiqueta: string }[] = [
  { valor: 'BORRADOR', etiqueta: 'Borrador' },
  { valor: 'PENDIENTE', etiqueta: 'Pendiente de aprobación' },
  { valor: 'APROBADA', etiqueta: 'Aprobada' },
  { valor: 'ENVIADA', etiqueta: 'Enviada' },
  { valor: 'RECIBIDA', etiqueta: 'Recibida' },
  { valor: 'ANULADA', etiqueta: 'Anulada' },
];

export interface OrdenCompraResumen {
  id: number;
  numeroOrden: string;
  fecha: string;
  fechaEntrega: string | null;
  proveedorId: number;
  proveedor: string;
  total: number;
  estado: EstadoOrdenCompra;
  creadoPorId: number;
}

export interface OrdenCompraPage {
  content: OrdenCompraResumen[];
  totalElements: number;
  totalPages: number;
}

export interface DetalleOrdenCompra {
  id: number;
  productoId: number;
  codigo: string;
  producto: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
}

export interface OrdenCompra {
  id: number;
  numeroOrden: string;
  fecha: string;
  fechaEntrega: string | null;
  proveedorId: number;
  proveedor: string;
  proveedorNit: string;
  proveedorCondicionPago: string | null;
  proveedorDiasCredito: number | null;
  creadoPorId: number;
  creadoPor: string | null;
  subtotal: number;
  descuento: number;
  total: number;
  estado: EstadoOrdenCompra;
  observaciones: string | null;
  fechaCreacion: string;
  registradaPorMi: boolean;
  detalles: DetalleOrdenCompra[];
}

export interface ItemOrdenCompraRequest {
  productoId: number;
  cantidad: number;
  costoUnitario: number;
}

export interface OrdenCompraRequest {
  proveedorId: number;
  fechaEntrega: string | null;
  descuento: number;
  observaciones: string | null;
  borrador: boolean;
  items: ItemOrdenCompraRequest[];
}

export interface ProductoComprable {
  id: number;
  codigo: string;
  nombre: string;
  ultimoCosto: number;
}

export interface OpcionesOrdenCompra {
  proveedores: ProveedorOpcion[];
  productos: ProductoComprable[];
  sucursales: { id: number; nombre: string }[];
}

export interface SugerenciaReposicion {
  productoId: number;
  codigo: string;
  producto: string;
  stockDisponible: number;
  stockMinimo: number;
  cantidadSugerida: number;
  ultimoCosto: number;
}

/** Renglón mientras se edita: puede no tener producto todavía. */
export interface RenglonOrden {
  productoId: number | null;
  cantidad: number;
  costoUnitario: number;
}
