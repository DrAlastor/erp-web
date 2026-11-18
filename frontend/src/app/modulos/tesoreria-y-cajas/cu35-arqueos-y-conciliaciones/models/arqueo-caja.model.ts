/**
 * HU-35 / CU35 — Cierre, Arqueo y Consulta de Caja. Tipos propios del cierre y del historial;
 * `TurnoResponse`, `ArqueoResponse` y `MovimientoResponse` son los mismos del contrato `/api/caja` (cu32).
 */
import { ArqueoResponse, EstadoTurno, MovimientoResponse, TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';

export interface CerrarTurnoRequest {
  montoContado: number;
  /** Obligatorias si la diferencia (contado − esperado) es distinta de 0. */
  observaciones?: string;
}

export interface TurnoDetalleResponse {
  turno: TurnoResponse;
  arqueo: ArqueoResponse;
  movimientos: MovimientoResponse[];
}

/** Página de Spring Data (`Page<TurnoResponse>`), mismos campos que usa CxP. */
export interface TurnoPage {
  content: TurnoResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltrosHistorialCaja {
  sucursalId: number | null;
  estado: '' | EstadoTurno;
  /** yyyy-MM-dd (fecha de apertura) */
  desde: string;
  hasta: string;
}

export const FILTROS_HISTORIAL_VACIOS: FiltrosHistorialCaja = { sucursalId: null, estado: '', desde: '', hasta: '' };

export type ResultadoArqueo = 'CUADRA' | 'SOBRANTE' | 'FALTANTE';
