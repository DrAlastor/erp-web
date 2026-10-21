export interface ProveedorItem {
  id: number;
  razonSocial: string;
  nitCi: string;
  diasCredito?: number;
}

export interface AlmacenItem {
  id: number;
  nombre: string;
  direccion?: string;
}

export interface ProductoItem {
  id: number;
  nombre: string;
  codigoSku: string;
  unidadMedida?: string;
  costoPromedio?: number;
}

export interface Catalogos {
  proveedores: ProveedorItem[];
  almacenes: AlmacenItem[];
  productos: ProductoItem[];
}

export interface CompraDetalle {
  id: number;
  productoId: number;
  productoCodigo?: string;
  productoNombre: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
  cantidadDevuelta: number;
}

export type EstadoCompra = 'REGISTRADA' | 'COMPLETADA' | 'DEVUELTA_PARCIAL' | 'DEVUELTA' | 'ANULADA';

export interface Compra {
  id: number;
  proveedorId: number;
  proveedorNombre?: string;
  sucursalId: number;
  sucursalNombre?: string;
  ordenCompraId?: number;
  numeroFactura: string;
  numeroAutorizacion?: string;
  fecha: string;
  subtotal: number;
  iva: number;
  total: number;
  tipoPago: 'CONTADO' | 'CREDITO';
  estado: EstadoCompra;
  observaciones?: string;
  fechaCreacion: string;
  detalles: CompraDetalle[];
}

export interface LineaCompraDto {
  productoId: number;
  cantidad: number;
  costoUnitario: number;
}

export interface RegistrarCompraDto {
  proveedorId: number;
  sucursalId: number;
  numeroFactura: string;
  numeroAutorizacion?: string;
  fecha: string;
  tipoPago: string;
  ordenCompraId?: number;
  observaciones?: string;
  lineas: LineaCompraDto[];
}

export interface LineaDevolucionDto {
  compraDetalleId: number;
  cantidad: number;
}

export interface RegistrarDevolucionDto {
  motivo: string;
  fecha?: string;
  lineas: LineaDevolucionDto[];
}

export interface DevolucionDetalle {
  id: number;
  compraDetalleId: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
}

export interface Devolucion {
  id: number;
  compraId: number;
  numeroDevolucion: string;
  numeroFactura?: string;
  proveedorNombre?: string;
  fecha: string;
  motivo: string;
  subtotal: number;
  iva: number;
  total: number;
  detalles: DevolucionDetalle[];
}
