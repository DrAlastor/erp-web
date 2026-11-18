import { HttpErrorResponse } from '@angular/common/http';
import { LineaAsientoRequest } from '../models/asiento.model';

/** Redondea a centavos para comparar importes sin errores de coma flotante. */
export const centavos = (n: number | null | undefined) => Math.round((Number(n) || 0) * 100);

export interface TotalesAsiento {
  debe: number;
  haber: number;
  diferencia: number;
  cuadra: boolean;
}

/** Totales del formulario; cuadra solo si debe = haber y ambos son mayores a cero. */
export function totales(lineas: LineaAsientoRequest[]): TotalesAsiento {
  const d = lineas.reduce((s, l) => s + centavos(l.debe), 0);
  const h = lineas.reduce((s, l) => s + centavos(l.haber), 0);
  return { debe: d / 100, haber: h / 100, diferencia: Math.abs(d - h) / 100, cuadra: d === h && d > 0 };
}

/**
 * Mismas reglas que el backend (ReglasAsiento), para avisar antes de enviar: cuenta en
 * cada línea, importe en un solo lado, al menos una línea al debe y otra al haber, y cuadre.
 */
export function erroresAsiento(glosa: string, lineas: LineaAsientoRequest[]): string[] {
  const errores: string[] = [];
  if (!glosa.trim()) errores.push('La glosa es obligatoria.');
  if (lineas.length < 2) errores.push('El asiento debe tener al menos dos líneas.');
  lineas.forEach((l, i) => {
    const d = centavos(l.debe);
    const h = centavos(l.haber);
    if (!l.cuentaId) errores.push(`Línea ${i + 1}: elige una cuenta.`);
    if (d < 0 || h < 0) errores.push(`Línea ${i + 1}: los importes no pueden ser negativos.`);
    else if (d > 0 && h > 0) errores.push(`Línea ${i + 1}: el importe va en el debe o en el haber, no en ambos.`);
    else if (d === 0 && h === 0) errores.push(`Línea ${i + 1}: falta el importe.`);
  });
  const t = totales(lineas);
  if (lineas.length >= 2 && (t.debe === 0 || t.haber === 0))
    errores.push('Hace falta al menos una línea al debe y otra al haber.');
  else if (lineas.length >= 2 && !t.cuadra)
    errores.push(`El asiento no cuadra: diferencia de Bs ${t.diferencia.toFixed(2)}.`);
  return errores;
}

/** Fecha de hoy en formato ISO local (yyyy-mm-dd). */
export function hoyIso(): string {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

export function mensajeError(err: HttpErrorResponse, recurso = 'asientos contables'): string {
  if (err.status === 0) return 'No se pudo conectar con el servidor. Intenta nuevamente.';
  if (err.status === 401) return 'La sesión ha expirado. Inicia sesión nuevamente.';
  if (err.status === 403) return err.error?.message?.startsWith('Su rol')
    ? err.error.message
    : `No tienes autorización para esta operación sobre ${recurso}.`;
  if (err.status >= 500) return 'El servidor no pudo completar la operación.';
  const campos = err.error?.fieldErrors ?? err.error?.errors;
  if (Array.isArray(campos) && campos.length) return campos.map((c: { message?: string }) => c.message ?? c).join(' ');
  return err.error?.message || 'No se pudo completar la operación.';
}
