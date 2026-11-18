/**
 * HU-34 / CU34 (+ apertura mínima) — contrato REST `/api/caja` copiado de la spec.
 * Montos como número JSON (BigDecimal, escala 2) y fechas ISO `yyyy-MM-ddTHH:mm:ss`.
 * CU35 (cierre, arqueo y consulta) reutiliza estos mismos tipos.
 */

export type EstadoTurno = 'ABIERTO' | 'CERRADO';
export type TipoMovimiento = 'INGRESO' | 'EGRESO';

export interface SucursalOpcion {
  id: number;
  nombre: string;
}

export interface FormaPagoCajaOpcion {
  id: number;
  codigo: string;
  nombre: string;
}

export interface CajaOpcionesResponse {
  sucursales: SucursalOpcion[];
  formasPago: FormaPagoCajaOpcion[];
}

export interface TurnoResponse {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  cajeroId: number;
  cajeroNombre: string;
  montoApertura: number;
  montoCierre: number | null;
  diferencia: number | null;
  fechaApertura: string;
  fechaCierre: string | null;
  estado: EstadoTurno;
  observaciones: string | null;
  efectivoEsperado: number;
}

export interface MovimientoResponse {
  id: number;
  turnoCajaId: number;
  tipo: TipoMovimiento;
  formaPagoId: number;
  formaPagoNombre: string;
  concepto: string;
  monto: number;
  numeroComprobante: string;
  fechaHora: string;
}

export interface ArqueoFormaPago {
  formaPagoId: number;
  nombre: string;
  ingresos: number;
  egresos: number;
  ventas: number;
}

export interface ArqueoResponse {
  turnoId: number;
  montoApertura: number;
  ventasEfectivo: number;
  ingresosEfectivo: number;
  egresosEfectivo: number;
  efectivoEsperado: number;
  totalIngresos: number;
  totalEgresos: number;
  cantidadMovimientos: number;
  porFormaPago: ArqueoFormaPago[];
  /** null mientras el turno está ABIERTO. */
  montoContado: number | null;
  /** null mientras el turno está ABIERTO. */
  diferencia: number | null;
}

export interface AbrirTurnoRequest {
  sucursalId: number;
  montoApertura: number;
  observaciones?: string;
}

export interface RegistrarMovimientoRequest {
  tipo: TipoMovimiento;
  formaPagoId: number;
  concepto: string;
  monto: number;
  numeroComprobante?: string;
}

/** Ids de navegación (`erp-navigation.ts`) usados para los enlaces entre apertura/movimientos y cierre. */
export const RUTA_APERTURA_CAJA: string[] = ['/app', 'apertura-de-caja'];
export const RUTA_CIERRE_CAJA: string[] = ['/app', 'cierre-arqueo-consulta-caja'];
