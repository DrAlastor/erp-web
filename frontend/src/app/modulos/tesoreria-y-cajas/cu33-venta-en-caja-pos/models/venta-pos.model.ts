export type ComprobantePos = 'FACTURA' | 'NOTA_VENTA';

export interface ProductoPos {
  id: number;
  codigo: string;
  nombre: string;
  precioVenta: number;
  stockDisponible: number;
}

export interface ClientePos {
  id: number;
  razonSocial: string;
  nitCi: string;
}

export interface LineaPos {
  productoId: number;
  codigo: string;
  nombre: string;
  precio: number;
  stock: number;
  cantidad: number;
}

export interface VentaPosRequest {
  clienteId: number | null;
  tipoComprobante: ComprobantePos;
  formaPagoId: number;
  montoRecibido: number | null;
  detalles: { productoId: number; cantidad: number }[];
}

export interface VentaPosResponse {
  ventaId: number;
  tipoComprobante: ComprobantePos;
  numero: number;
  fecha: string;
  turnoCajaId: number;
  razonSocialCliente: string;
  nitCliente: string;
  formaPago: string;
  total: number;
  montoRecibido: number;
  cambio: number;
}
