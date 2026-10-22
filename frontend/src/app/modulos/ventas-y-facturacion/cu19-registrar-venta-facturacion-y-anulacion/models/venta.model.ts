export type TipoVenta = 'CONTADO' | 'CREDITO';
/** Factura: comprobante fiscal con datos del SIN. Nota de venta: sin datos del SIN ni impuestos. */
export type TipoComprobante = 'FACTURA' | 'NOTA_VENTA';

export interface VentaLineaRequest {
  productoId: number;
  cantidad: number;
  descuentoId?: number | null;
  descuento?: number | null;
}

export interface VentaPagoRequest {
  formaPagoId: number;
  importe: number;
  transaccionExterna?: string | null;
}

export interface VentaRequest {
  sucursalId: number;
  clienteId: number;
  turnoCajaId?: number | null;
  cotizacionId?: number | null;
  tipoVenta: TipoVenta;
  tipoComprobante: TipoComprobante;
  diasCredito?: number | null;
  detalles: VentaLineaRequest[];
  pagos: VentaPagoRequest[];
}

export interface VentaLinea {
  id: number;
  productoId: number;
  codigo: string;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  subtotal: number;
}

export interface VentaPago {
  id: number;
  formaPagoId: number;
  formaPago: string;
  importe: number;
  transaccionExterna: string | null;
  estado: string;
}

export interface VentaCuota {
  id: number;
  numeroCuota: number;
  montoTotal: number;
  montoCobrado: number;
  saldo: number;
  fechaVencimiento: string;
  estado: string;
}

export interface Venta {
  id: number;
  tipoComprobante: TipoComprobante;
  numeroFactura: number;
  numeroAutorizacion: string | null;
  codigoControl: string | null;
  leyendaSin: string | null;
  fecha: string;
  estado: string;
  tipoVenta: TipoVenta;
  sucursalId: number;
  sucursalNombre: string;
  clienteId: number;
  nitCliente: string;
  razonSocialCliente: string;
  cajeroId: number;
  turnoCajaId: number | null;
  subtotal: number;
  descuento: number;
  iva: number;
  it: number;
  total: number;
  detalles: VentaLinea[];
  pagos: VentaPago[];
  cuentasPorCobrar: VentaCuota[];
  motivoAnulacion: string | null;
  fechaAnulacion: string | null;
  anuladoPor: number | null;
  asientoContable: string | null;
}

export interface VentaResumen {
  id: number;
  tipoComprobante: TipoComprobante;
  numeroFactura: number;
  fecha: string;
  razonSocialCliente: string;
  nitCliente: string;
  sucursalNombre: string;
  tipoVenta: TipoVenta;
  total: number;
  estado: string;
}

export interface VentaPage {
  content: VentaResumen[];
  totalElements: number;
  totalPages: number;
}

export interface CotizacionLinea {
  productoId?: number;
  articuloId?: number;
  codigo?: string;
  sku?: string;
  nombre?: string;
  articuloNombre?: string;
  cantidad: number;
  precioUnitario: number;
  descuento?: number;
  subtotal: number;
}

export interface CotizacionResumen {
  id: number;
  numeroCotizacion?: string;
  numero?: string;
  clienteId: number;
  clienteRazonSocial: string;
  clienteNitCi: string;
  total: number;
  fecha: string;
  lineas?: CotizacionLinea[];
  detalles?: CotizacionLinea[];
}

export interface CatalogosVenta {
  sucursales: { id: number; nombre: string }[];
  formasPago: { id: number; codigo: string; nombre: string }[];
  descuentos: { id: number; nombre: string; tipo: string; valor: number }[];
  turnosAbiertos: { id: number; sucursalId: number; cajeroId: number; fechaApertura: string; nombre?: string }[];
  dosificacion: {
    numeroAutorizacion: string;
    siguienteNumero: number;
    numeroFinal: number;
    fechaLimite: string;
  } | null;
  /** Hay Stripe configurado y la forma de pago PASARELA_ONLINE está activa. */
  cobroEnLinea: boolean;
  /** Tasas de la tabla impuestos, en porcentaje (13 = 13 %). */
  tasaIva: number;
  tasaIt: number;
  cotizacionesPendientes?: CotizacionResumen[];
}

export interface Dosificacion {
  id: number;
  numeroAutorizacion: string;
  leyendaSin: string;
  numeroInicial: number;
  numeroFinal: number;
  ultimoNumero: number | null;
  disponibles: number;
  fechaLimite: string;
  activo: boolean;
  estado: 'VIGENTE' | 'INACTIVA' | 'VENCIDA' | 'AGOTADA';
}

export interface DosificacionRequest {
  numeroAutorizacion: string;
  leyendaSin: string;
  numeroInicial: number;
  numeroFinal: number;
  fechaLimite: string;
}

export interface CobroStripeRequest {
  venta: VentaRequest;
  successUrl: string;
  cancelUrl: string;
}

export interface CobroStripeResponse {
  sessionId: string;
  checkoutUrl: string;
  total: number;
}

export interface ProductoVenta {
  id: number;
  codigo: string;
  nombre: string;
  precioVenta: number;
  stockDisponible: number;
}

export interface ClienteVenta {
  id: number;
  razonSocial: string;
  nitCi: string;
}
