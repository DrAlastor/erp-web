import { ResultadoArqueo } from '../models/arqueo-caja.model';

const round2 = (value: number) => Math.round((Number(value) || 0) * 100) / 100;

/** Espejo del backend: diferencia = contado − esperado (positivo = sobrante, negativo = faltante). null si no hay monto. */
export function calcularDiferencia(montoContado: number | null | undefined, efectivoEsperado: number): number | null {
  if (montoContado === null || montoContado === undefined || !Number.isFinite(Number(montoContado))) return null;
  const centavos = Math.round(Number(montoContado) * 100) - Math.round(Number(efectivoEsperado) * 100);
  return centavos / 100 || 0;
}

export function resultadoArqueo(diferencia: number | null | undefined): ResultadoArqueo | null {
  if (diferencia === null || diferencia === undefined) return null;
  const d = round2(diferencia);
  return d === 0 ? 'CUADRA' : d > 0 ? 'SOBRANTE' : 'FALTANTE';
}

export function claseResultado(r: ResultadoArqueo | null): string {
  return r === 'CUADRA' ? 'confirmado' : r === 'SOBRANTE' ? 'pendiente' : r === 'FALTANTE' ? 'vencida' : 'otro';
}

/** Mismas reglas que el backend para cerrar; '' si es válido. */
export function validarCierre(montoContado: number | null, efectivoEsperado: number, observaciones: string): string {
  if (montoContado === null || !(montoContado >= 0)) return 'Ingresa el monto contado en caja (0 o mayor).';
  if (Math.abs(round2(montoContado) - montoContado) > 1e-9) return 'El monto contado admite como máximo 2 decimales.';
  const dif = calcularDiferencia(montoContado, efectivoEsperado);
  if (dif !== 0 && !observaciones.trim()) return 'Hay diferencia entre lo contado y lo esperado: explica el motivo en observaciones.';
  if (observaciones.length > 500) return 'Las observaciones admiten hasta 500 caracteres.';
  return '';
}

/** Valida el rango de fechas del filtro del historial; '' si es válido. */
export function validarRango(desde: string, hasta: string): string {
  return desde && hasta && desde > hasta ? 'La fecha "desde" no puede ser posterior a "hasta".' : '';
}
