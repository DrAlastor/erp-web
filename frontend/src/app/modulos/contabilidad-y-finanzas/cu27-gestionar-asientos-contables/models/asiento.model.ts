/** HU-27 / CU27 — modelos de `/api/asientos-contables`. */

export type EstadoAsiento = 'BORRADOR' | 'MAYORIZADO' | 'ANULADO';
export type OrigenAsiento = '' | 'MANUAL' | 'AUTOMATICO';

export interface AsientoResumen {
  id: number;
  numero: string;
  fecha: string;
  tipo: string;
  glosa: string;
  totalDebe: number;
  totalHaber: number;
  estado: EstadoAsiento;
  manual: boolean;
  periodo: string | null;
  creadoPor: string | null;
}

export interface LineaAsiento {
  id: number;
  cuentaId: number;
  cuentaCodigo: string;
  cuentaNombre: string;
  glosa: string | null;
  debe: number;
  haber: number;
}

export interface Asiento extends AsientoResumen {
  editable: boolean;
  anulable: boolean;
  estadoPeriodo: string | null;
  fechaCreacion: string | null;
  lineas: LineaAsiento[];
}

export interface AsientoPage {
  content: AsientoResumen[];
  totalElements: number;
  totalPages: number;
}

export interface LineaAsientoRequest {
  cuentaId: number | null;
  glosa: string | null;
  debe: number | null;
  haber: number | null;
}

export interface AsientoRequest {
  fecha: string;
  tipo: string;
  glosa: string;
  lineas: LineaAsientoRequest[];
}

export interface CuentaOpcion {
  id: number;
  codigo: string;
  nombre: string;
  tipo: string;
  naturaleza: string;
}

export interface FiltroAsientos {
  search: string;
  tipo: string;
  estado: string;
  origen: OrigenAsiento;
  desde: string;
  hasta: string;
}

export interface PermisosAsientos {
  editar: boolean;
  anular: boolean;
}

/** Tipos que el sistema genera solo; se muestran en el filtro del Libro Diario. */
export const TIPOS_AUTOMATICOS = ['VENTAS', 'ANULACION', 'DEVOLUCION', 'COBRO', 'REVERSION', 'APERTURA'];
