/**
 * HU-24 / CU34 — contrato REST `/api/cuentas-por-pagar` (copiado literalmente de la spec).
 * Montos con escala 2 y fechas ISO `yyyy-MM-dd`.
 */

/** Estados almacenados. "Vencida" es derivada (saldo > 0 y fechaVencimiento < hoy). */
export type EstadoCuentaPorPagar = 'PENDIENTE' | 'PARCIAL' | 'PAGADA';

/** Valores aceptados por el filtro `estado` del listado (VENCIDA aplica la condición derivada). */
export type FiltroEstadoCxp = '' | EstadoCuentaPorPagar | 'VENCIDA';

export interface CuentaPorPagarResumenResponse {
  id: number;
  compraId: number;
  numeroFactura: string;
  proveedorId: number;
  proveedorRazonSocial: string;
  proveedorNitCi: string;
  numeroCuota: number;
  montoTotal: number;
  montoPagado: number;
  saldo: number;
  fechaVencimiento: string;
  estado: EstadoCuentaPorPagar;
  vencida: boolean;
  diasVencido: number;
}

export interface PagoProveedorResponse {
  id: number;
  formaPagoId: number;
  formaPagoNombre: string;
  monto: number;
  fechaPago: string;
  numeroRecibo: string | null;
  transaccionExterna: string | null;
  observaciones: string | null;
  usuarioNombre: string | null;
  fechaCreacion: string | null;
}

export interface CuentaPorPagarResponse extends CuentaPorPagarResumenResponse {
  fechaCompra: string;
  totalCompra: number;
  sucursalNombre: string;
  pagos: PagoProveedorResponse[];
}

export type RangoAntiguedad = '0-30' | '31-60' | '61-90' | '90+';

export interface AntiguedadCxp {
  rango: RangoAntiguedad;
  monto: number;
  cantidad: number;
}

export interface TopProveedorCxp {
  proveedorId: number;
  razonSocial: string;
  saldo: number;
}

export interface ResumenCxpResponse {
  saldoTotal: number;
  vencido: number;
  porVencer7Dias: number;
  pagadoMes: number;
  cantidadPendientes: number;
  cantidadVencidas: number;
  antiguedad: AntiguedadCxp[];
  topProveedores: TopProveedorCxp[];
}

export interface RegistrarPagoRequest {
  formaPagoId: number;
  monto: number;
  fechaPago?: string;
  numeroRecibo?: string;
  transaccionExterna?: string;
  observaciones?: string;
}

export interface GenerarCuotasRequest {
  compraId: number;
  numeroCuotas: number;
  primerVencimiento?: string;
  diasEntreCuotas?: number;
}

export interface CompraSinCuentaResponse {
  compraId: number;
  numeroFactura: string;
  proveedorRazonSocial: string;
  fecha: string;
  total: number;
  diasCredito: number | null;
}

export interface ProveedorOpcionResponse {
  id: number;
  razonSocial: string;
  nitCi: string;
}

export interface FormaPagoOpcionResponse {
  id: number;
  nombre: string;
}

/** Página de Spring Data (`Page<CuentaPorPagarResumenResponse>`), mismos campos que usa CU16. */
export interface CuentaPorPagarPage {
  content: CuentaPorPagarResumenResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Cuota calculada en el cliente solo como vista previa antes de confirmar la generación. */
export interface CuotaPrevia {
  numero: number;
  fechaVencimiento: string;
  monto: number;
}

export const FILTROS_ESTADO_CXP: readonly { value: FiltroEstadoCxp; label: string }[] = [
  { value: '', label: 'Todos los estados' },
  { value: 'PENDIENTE', label: 'Pendiente' },
  { value: 'PARCIAL', label: 'Pago parcial' },
  { value: 'VENCIDA', label: 'Vencida' },
  { value: 'PAGADA', label: 'Pagada' },
];
