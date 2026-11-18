import { AccionPeriodo, PeriodoContable } from '../models/periodo-contable.model';

/**
 * Acción disponible para un periodo, con las mismas reglas que el backend:
 * se cierra el primer mes abierto si todos los anteriores están cerrados, y solo se reabre
 * el último mes cerrado.
 */
export function accionDisponible(periodo: PeriodoContable, periodos: readonly PeriodoContable[]): AccionPeriodo {
  const porMes = new Map(periodos.map((p) => [p.mes, p]));
  if (periodo.estado === 'ABIERTO') {
    for (let mes = 1; mes < periodo.mes; mes++) {
      if (porMes.get(mes)?.estado !== 'CERRADO') return null;
    }
    return 'cerrar';
  }
  return porMes.get(periodo.mes + 1)?.estado === 'CERRADO' ? null : 'reabrir';
}

/** Gestiones que se pueden generar: 5 años atrás hasta el año siguiente, más las ya existentes. */
export function gestionesDisponibles(existentes: readonly number[], anioActual: number): number[] {
  const todas = new Set(existentes);
  for (let a = anioActual - 5; a <= anioActual + 1; a++) todas.add(a);
  return [...todas].sort((a, b) => b - a);
}

/** "2026-11-01" → "01/11/2026" sin pasar por Date (evita corrimientos de zona horaria). */
export function fechaCorta(iso: string | null): string {
  if (!iso) return '—';
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

/** Cantidad de meses que le faltan a la gestión para tener los 12. */
export function mesesFaltantes(periodos: readonly PeriodoContable[]): number {
  return 12 - new Set(periodos.map((p) => p.mes)).size;
}
