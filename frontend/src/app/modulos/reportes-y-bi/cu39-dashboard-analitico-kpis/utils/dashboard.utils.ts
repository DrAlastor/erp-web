import { HttpErrorResponse } from '@angular/common/http';
import { ERP_MODULES } from '../../../../core/models/erp-navigation';
import { AlertaDashboard, PuntoSerieDiaria, SeveridadAlerta, TipoAlertaDashboard } from '../models/dashboard-ejecutivo.model';

export interface BarraSerie {
  x: number;
  y: number;
  ancho: number;
  alto: number;
  fecha: string;
  total: number;
}

/** Slug de la ruta `/app/:funcion` para un `funcionId` del menú lateral (acepta prefijo `cuNN-`); null si no existe. */
export function rutaFuncion(funcionId: string | null | undefined, modulos = ERP_MODULES): string | null {
  if (!funcionId) return null;
  const id = funcionId.toLowerCase();
  const limpio = id.replace(/^cu\d+-/, '');
  for (const modulo of modulos) {
    const fn = modulo.functions.find((f) => [f.id, f.slug].some((v) => v === id || v === limpio));
    if (fn) return fn.slug || fn.id;
  }
  return null;
}

/** Geometría de barras para el gráfico SVG de ventas diarias (viewBox ancho × alto, sin librerías). */
export function barrasSerie(serie: readonly PuntoSerieDiaria[], ancho: number, alto: number, hueco = 2): BarraSerie[] {
  if (!serie.length) return [];
  const max = Math.max(...serie.map((p) => Number(p.total) || 0));
  const paso = ancho / serie.length;
  return serie.map((p, i) => {
    const valor = Math.max(0, Number(p.total) || 0);
    const h = max > 0 ? (valor / max) * alto : 0;
    // Los días sin venta muestran una marca mínima para que el eje no quede vacío.
    const visible = valor > 0 ? Math.max(h, 1.5) : 0.75;
    return { x: i * paso + hueco / 2, y: alto - visible, ancho: Math.max(paso - hueco, 1), alto: visible, fecha: p.fecha, total: valor };
  });
}

export function porcentaje(valor: number, max: number): number {
  return max > 0 ? Math.min(100, Math.max(0, (valor / max) * 100)) : 0;
}

/** Texto de la variación vs. el mismo tramo de días del mes anterior; null cuando el backend no pudo calcularla. */
export function textoVariacion(pct: number | null | undefined): string {
  if (pct === null || pct === undefined || !Number.isFinite(pct)) return 'Sin comparación con el mes anterior';
  const signo = pct > 0 ? '+' : '';
  return `${signo}${pct.toFixed(1)} % vs. mismo período del mes anterior`;
}

export function claseVariacion(pct: number | null | undefined): 'positiva' | 'negativa' | 'neutra' {
  if (pct === null || pct === undefined || !Number.isFinite(pct) || pct === 0) return 'neutra';
  return pct > 0 ? 'positiva' : 'negativa';
}

const PESO: Record<SeveridadAlerta, number> = { ALTA: 0, MEDIA: 1, INFO: 2 };

export function ordenarAlertas(alertas: readonly AlertaDashboard[]): AlertaDashboard[] {
  return [...alertas].sort((a, b) => (PESO[a.severidad] ?? 3) - (PESO[b.severidad] ?? 3));
}

const ETIQUETAS: Record<TipoAlertaDashboard, string> = {
  CXP_VENCIDA: 'Cuentas por pagar', CXC_VENCIDA: 'Cuentas por cobrar', STOCK_BAJO: 'Stock bajo',
  SIN_STOCK: 'Sin stock', PEDIDOS_PENDIENTES: 'Pedidos', VENTAS_A_LA_BAJA: 'Ventas',
};

export function etiquetaAlerta(tipo: string): string {
  return ETIQUETAS[tipo as TipoAlertaDashboard] ?? tipo;
}

/** Fecha ISO `yyyy-MM-dd` → `dd/MM` para ejes y tooltips. */
export function fechaCorta(iso: string): string {
  const [, m, d] = iso.slice(0, 10).split('-');
  return d && m ? `${d}/${m}` : iso;
}

export function mensajeError(err: HttpErrorResponse): string {
  if (err.status === 403) return 'No tienes el permiso REPORTES_CONSULTAR para ver el dashboard ejecutivo.';
  if (err.status === 0) return 'No se pudo conectar con el servidor backend. Verifica tu conexión o reinicia el backend.';
  if (err.status === 401) return 'Tu sesión ha expirado o no es válida. Inicia sesión nuevamente.';
  if (err.error?.message) return err.error.message;
  if (err.error?.mensaje) return err.error.mensaje;
  if (err.status === 404) return 'El servicio del dashboard no fue encontrado (404). Verifica que el backend esté actualizado.';
  if (err.status >= 500) return `Error interno del servidor (${err.status}). Intenta de nuevo.`;
  return 'Ocurrió un error al cargar el dashboard.';
}
