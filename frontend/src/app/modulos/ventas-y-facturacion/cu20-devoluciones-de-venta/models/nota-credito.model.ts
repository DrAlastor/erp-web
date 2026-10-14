export type MotivoNotaCredito =
  | 'DEFECTUOSO'
  | 'CAMBIO_PRODUCTO'
  | 'ERROR_FACTURACION'
  | 'DESISTIMIENTO'
  | 'OTRO';

export const MOTIVOS_NOTA_CREDITO: { valor: MotivoNotaCredito; etiqueta: string }[] = [
  { valor: 'DEFECTUOSO', etiqueta: 'Producto defectuoso' },
  { valor: 'CAMBIO_PRODUCTO', etiqueta: 'Cambio de producto' },
  { valor: 'ERROR_FACTURACION', etiqueta: 'Error de facturación' },
  { valor: 'DESISTIMIENTO', etiqueta: 'Desistimiento del cliente' },
  { valor: 'OTRO', etiqueta: 'Otro motivo' },
];

export interface LineaDevolucion {
  ventaDetalleId: number;
  productoId: number;
  codigo: string;
  nombre: string;
  cantidadVendida: number;
  cantidadDevuelta: number;
  cantidadDisponible: number;
  precioUnitarioNeto: number;
}

export type TipoComprobante = 'FACTURA' | 'NOTA_VENTA';

export interface VentaDevolucion {
  ventaId: number;
  tipoComprobante: TipoComprobante;
  numeroFactura: number;
  numeroAutorizacion: string | null;
  fecha: string;
  nitCliente: string;
  razonSocialCliente: string;
  sucursalNombre: string;
  tipoVenta: 'CONTADO' | 'CREDITO';
  total: number;
  lineas: LineaDevolucion[];
}

export interface NotaCreditoRequest {
  ventaId: number;
  motivo: MotivoNotaCredito;
  observaciones?: string | null;
  items: { ventaDetalleId: number; cantidad: number }[];
}

export interface NotaCreditoLinea {
  id: number;
  ventaDetalleId: number | null;
  productoId: number;
  codigo: string;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface NotaCredito {
  id: number;
  numeroNota: string;
  fecha: string;
  motivo: string;
  estado: string;
  total: number;
  observaciones: string | null;
  ventaId: number;
  tipoComprobante: TipoComprobante;
  numeroFactura: number;
  nitCliente: string;
  razonSocialCliente: string;
  sucursalNombre: string;
  tipoVenta: string;
  emitidoPor: number;
  detalles: NotaCreditoLinea[];
  montoAplicadoCxc: number | null;
  montoReembolsado: number | null;
  asientoContable: string | null;
}

export interface NotaCreditoResumen {
  id: number;
  numeroNota: string;
  fecha: string;
  tipoComprobante: TipoComprobante;
  numeroFactura: number;
  razonSocialCliente: string;
  nitCliente: string;
  motivo: string;
  total: number;
  estado: string;
}

export interface NotaCreditoPage {
  content: NotaCreditoResumen[];
  totalElements: number;
  totalPages: number;
}
