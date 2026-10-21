import { HttpErrorResponse } from '@angular/common/http';
import {
  AntiguedadCxp,
  CompraSinCuentaResponse,
  CuentaPorPagarResumenResponse,
  CuotaPrevia,
  RangoAntiguedad,
  RegistrarPagoRequest,
} from '../models/cuenta-por-pagar.model';

export const round2 = (value: number) => Math.round((Number(value) || 0) * 100) / 100;

/** Fecha local de hoy en ISO `yyyy-MM-dd`. */
export function hoyIso(base = new Date()): string {
  return `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(base.getDate()).padStart(2, '0')}`;
}

/** Suma días a una fecha ISO sin depender de la zona horaria del navegador. */
export function sumarDias(iso: string, dias: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const fecha = new Date(Date.UTC(y, m - 1, d + dias));
  return fecha.toISOString().slice(0, 10);
}

/** Default del backend: fecha de compra + días de crédito del proveedor (o +30 si no tiene). */
export function primerVencimientoSugerido(compra: CompraSinCuentaResponse): string {
  return sumarDias(compra.fecha, compra.diasCredito ?? 30);
}

/** Espejo del reparto del backend: N cuotas iguales, la última absorbe el redondeo. */
export function previsualizarCuotas(
  total: number, numeroCuotas: number, primerVencimiento: string, diasEntreCuotas: number,
): CuotaPrevia[] {
  const n = Math.trunc(numeroCuotas);
  if (!(n >= 1) || !primerVencimiento) return [];
  const centavos = Math.round(total * 100);
  const base = Math.floor(centavos / n);
  return Array.from({ length: n }, (_, i) => ({
    numero: i + 1,
    fechaVencimiento: sumarDias(primerVencimiento, i * diasEntreCuotas),
    monto: (i === n - 1 ? centavos - base * (n - 1) : base) / 100,
  }));
}

/** Mismas reglas que valida el backend para registrar un pago; '' si es válido. */
export function validarPago(body: Partial<RegistrarPagoRequest>, saldo: number, hoy = hoyIso()): string {
  if (!body.formaPagoId) return 'Selecciona la forma de pago.';
  const monto = Number(body.monto);
  if (!(monto > 0)) return 'El monto del pago debe ser mayor a 0.';
  if (round2(monto) > round2(saldo)) return `El monto no puede superar el saldo pendiente (Bs. ${round2(saldo).toFixed(2)}).`;
  if (body.fechaPago && body.fechaPago > hoy) return 'La fecha de pago no puede ser futura.';
  if ((body.numeroRecibo?.length ?? 0) > 50) return 'El número de recibo admite hasta 50 caracteres.';
  if ((body.transaccionExterna?.length ?? 0) > 100) return 'La transacción externa admite hasta 100 caracteres.';
  if ((body.observaciones?.length ?? 0) > 500) return 'Las observaciones admiten hasta 500 caracteres.';
  return '';
}

/** Validación del formulario "Generar cuotas"; '' si es válido. */
export function validarGeneracion(compraId: number | null, numeroCuotas: number, diasEntreCuotas: number): string {
  if (!compraId) return 'Selecciona la compra a crédito que originará las cuotas.';
  if (!Number.isInteger(numeroCuotas) || numeroCuotas < 1 || numeroCuotas > 24) return 'El número de cuotas debe estar entre 1 y 24.';
  if (!Number.isInteger(diasEntreCuotas) || diasEntreCuotas < 1 || diasEntreCuotas > 365) {
    return 'Los días entre cuotas deben estar entre 1 y 365.';
  }
  return '';
}

/** Estado visible: VENCIDA (derivada) tiene prioridad sobre el estado almacenado. */
export function estadoVisible(cuenta: Pick<CuentaPorPagarResumenResponse, 'estado' | 'vencida'>): string {
  return cuenta.vencida && cuenta.estado !== 'PAGADA' ? 'VENCIDA' : cuenta.estado;
}

export function claseEstadoCxp(estado: string): string {
  const clases: Record<string, string> = { PENDIENTE: 'pendiente', PARCIAL: 'parcial', PAGADA: 'confirmado', VENCIDA: 'vencida' };
  return clases[estado] ?? 'otro';
}

const RANGOS: readonly RangoAntiguedad[] = ['0-30', '31-60', '61-90', '90+'];

/** Antigüedad completa (los 4 rangos en orden) con el porcentaje de cada uno sobre el total vencido. */
export function segmentosAntiguedad(antiguedad: readonly AntiguedadCxp[]): (AntiguedadCxp & { pct: number })[] {
  const total = antiguedad.reduce((acc, a) => acc + (Number(a.monto) || 0), 0);
  return RANGOS.map((rango) => {
    const item = antiguedad.find((a) => a.rango === rango) ?? { rango, monto: 0, cantidad: 0 };
    return { ...item, pct: total > 0 ? round2((item.monto / total) * 100) : 0 };
  });
}

export function mensajeError(err: HttpErrorResponse): string {
  if (err.status === 403) return 'No tienes los permisos de Contabilidad requeridos para esta operación.';
  if (err.status === 0) return 'No se pudo conectar con el servidor backend. Verifica tu conexión o reinicia el backend.';
  if (err.status === 401) return 'Tu sesión ha expirado o no es válida. Inicia sesión nuevamente.';
  if (err.error?.message) return err.error.message;
  if (err.error?.mensaje) return err.error.mensaje;
  if (err.error?.detail) return err.error.detail;
  if (typeof err.error === 'string' && err.error.trim()) return err.error;
  if (err.status === 404) return 'El recurso no fue encontrado (404). Verifica que el backend esté actualizado.';
  if (err.status === 409) return 'La operación entra en conflicto con el estado actual de la cuenta.';
  if (err.status === 400) return 'Los datos enviados contienen errores o faltan campos obligatorios.';
  if (err.status >= 500) return `Error interno del servidor (${err.status}). Intenta de nuevo.`;
  return 'Ocurrió un error al procesar la operación.';
}
