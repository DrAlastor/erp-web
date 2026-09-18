export interface MovimientoInventarioRequest {
  productoId: number;
  almacenId: number;
  tipoMovimiento: 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'AJUSTE_POSITIVO' | 'AJUSTE_NEGATIVO';
  cantidad: number;
  costoUnitario?: number;
  referenciaDoc?: string;
  motivo?: string;
  fecha?: string;
}

export interface MovimientoInventarioResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  productoSku: string;
  unidadMedida: string;
  almacenId: number;
  almacenNombre: string;
  tipoMovimiento: string;
  cantidad: number;
  costoUnitario: number;
  saldoCantidad: number;
  saldoValorado: number;
  referenciaDoc: string | null;
  motivo: string | null;
  fecha: string;
  creadoPor: string;
}

export interface StockAlmacenResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  productoSku: string;
  codigoBarra: string | null;
  unidadMedida: string;
  precioVenta: number;
  costoPromedio: number;
  stockMinimo: number;
  almacenId: number;
  almacenNombre: string;
  cantidadActual: number;
  bajoStockMinimo: boolean;
}

export interface AlertaStockMinimoResponse {
  productoId: number;
  productoNombre: string;
  productoSku: string;
  unidadMedida: string;
  almacenId: number;
  almacenNombre: string;
  cantidadActual: number;
  stockMinimo: number;
  deficit: number;
  nivelCriticidad: 'AGOTADO' | 'CRITICO' | 'ALERTA';
}

export interface ProductoSimple {
  id: number;
  nombre: string;
  codigoSku: string;
  codigoBarra: string | null;
  unidadMedida: string;
  costoPromedio: number;
  stockMinimo: number;
  categoriaNombre: string;
}

export interface AlmacenSimple {
  id: number;
  nombre: string;
  direccion: string | null;
  esPrincipal: boolean;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
