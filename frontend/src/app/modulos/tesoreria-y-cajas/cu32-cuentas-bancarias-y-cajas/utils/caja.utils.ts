import type { HttpErrorResponse } from '@angular/common/http';
import type { AbrirTurnoRequest, RegistrarMovimientoRequest } from '../models/caja.model';

export const round2 = (value: number) => Math.round((Number(value) || 0) * 100) / 100;

/** Convierte el valor de un `<input type="number">` a número o null si está vacío / no es finito. */
export function aNumero(value: unknown): number | null {
  if (value === '' || value === null || value === undefined) return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

const DOS_DECIMALES = (n: number) => Math.abs(round2(n) - n) < 1e-9;

/** Mismas reglas que valida el backend para abrir turno; '' si es válido. */
export function validarApertura(body: Partial<AbrirTurnoRequest>): string {
  if (!body.sucursalId) return 'Selecciona la sucursal donde abrirás la caja.';
  const monto = body.montoApertura;
  if (monto === null || monto === undefined || !(monto >= 0)) return 'El monto de apertura debe ser 0 o mayor.';
  if (!DOS_DECIMALES(monto)) return 'El monto de apertura admite como máximo 2 decimales.';
  return '';
}

/** Mismas reglas que valida el backend para registrar un movimiento; '' si es válido. */
export function validarMovimiento(body: Partial<RegistrarMovimientoRequest>): string {
  if (body.tipo !== 'INGRESO' && body.tipo !== 'EGRESO') return 'Selecciona si es un ingreso o un egreso.';
  if (!body.formaPagoId) return 'Selecciona la forma de pago.';
  const concepto = (body.concepto ?? '').trim();
  if (concepto.length < 3 || concepto.length > 255) return 'El concepto debe tener entre 3 y 255 caracteres.';
  const monto = Number(body.monto);
  if (!(monto > 0)) return 'El monto debe ser mayor a 0.';
  if (!DOS_DECIMALES(monto)) return 'El monto admite como máximo 2 decimales.';
  if ((body.numeroComprobante?.length ?? 0) > 50) return 'El número de comprobante admite hasta 50 caracteres.';
  return '';
}

/** `2026-11-17T08:30:00` → `17/11/2026 08:30` (sin depender de la zona horaria del navegador). */
export function fechaHoraCorta(iso: string | null | undefined): string {
  if (!iso) return '—';
  const [fecha, hora = ''] = iso.split('T');
  const [y, m, d] = fecha.split('-');
  return `${d}/${m}/${y}${hora ? ' ' + hora.slice(0, 5) : ''}`;
}

export function claseEstadoTurno(estado: string): string {
  return estado === 'ABIERTO' ? 'parcial' : estado === 'CERRADO' ? 'confirmado' : 'otro';
}

export function claseTipoMovimiento(tipo: string): string {
  return tipo === 'INGRESO' ? 'confirmado' : 'vencida';
}

/** Mensaje legible para el usuario; prioriza el mensaje en español que envía el backend. */
export function mensajeErrorCaja(err: HttpErrorResponse): string {
  if (err.status === 0) return 'No se pudo conectar con el servidor backend. Verifica tu conexión o reinicia el backend.';
  if (err.status === 401) return 'Tu sesión ha expirado o no es válida. Inicia sesión nuevamente.';
  if (err.error?.message) return err.error.message;
  if (err.error?.mensaje) return err.error.mensaje;
  if (err.error?.detail) return err.error.detail;
  if (typeof err.error === 'string' && err.error.trim()) return err.error;
  if (err.status === 403) return 'No tienes permiso para operar esta caja (se requiere Facturación o ser el cajero del turno).';
  if (err.status === 404) return 'El turno de caja no existe o fue eliminado.';
  if (err.status === 409) return 'La operación entra en conflicto con el estado actual del turno de caja.';
  if (err.status === 422) return 'La forma de pago seleccionada no existe o está inactiva.';
  if (err.status === 400) return 'Los datos enviados contienen errores o faltan campos obligatorios.';
  if (err.status >= 500) return `Error interno del servidor (${err.status}). Intenta de nuevo.`;
  return 'Ocurrió un error al procesar la operación de caja.';
}
