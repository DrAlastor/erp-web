import { HttpErrorResponse } from '@angular/common/http';
import { Metricas, Pronostico, VistaIn } from '../models/inteligencia-negocio.model';

/** Slug del sidebar → pestaña. */
export const VISTAS: { vista: VistaIn; slug: string; titulo: string; icono: string }[] = [
  { vista: 'panel', slug: 'panel-modelos-predictivos', titulo: 'Panel', icono: '◈' },
  { vista: 'pronostico', slug: 'pronostico-de-ventas', titulo: 'Pronóstico', icono: '↗' },
  { vista: 'abandono', slug: 'riesgo-de-abandono', titulo: 'Abandono', icono: '⚠' },
  { vista: 'segmentacion', slug: 'segmentacion-de-clientes', titulo: 'Segmentos', icono: '◔' },
  { vista: 'canasta', slug: 'canasta-de-mercado', titulo: 'Canasta', icono: '⛁' },
  { vista: 'anomalias', slug: 'deteccion-de-anomalias', titulo: 'Anomalías', icono: '⚡' },
  { vista: 'reposicion', slug: 'sugerencia-de-compra', titulo: 'Reposición', icono: '▤' },
];

export function vistaDesdeSlug(slug: string | null | undefined): VistaIn {
  return VISTAS.find((v) => v.slug === slug)?.vista ?? 'panel';
}

export function mensajeError(err: HttpErrorResponse): string {
  if (err.status === 0) return 'No se pudo conectar con el servidor.';
  if (err.status === 403) return 'No tienes permiso para ver Inteligencia de Negocio (REPORTES / CONSULTAR).';
  const msg = (err.error as { message?: string } | null)?.message;
  return msg || `Error ${err.status} al consultar los modelos.`;
}

export function pct(valor: number | null | undefined, decimales = 1): string {
  if (valor === null || valor === undefined || Number.isNaN(valor)) return '—';
  return `${(valor * 100).toFixed(decimales)} %`;
}

export function bs(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—';
  return 'Bs. ' + Number(valor).toLocaleString('es-BO', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

export function etiquetaPeriodo(iso: string, frecuencia: 'MENSUAL' | 'SEMANAL'): string {
  const [a, m, d] = iso.split('-').map(Number);
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  return frecuencia === 'MENSUAL' ? `${meses[m - 1]} ${String(a).slice(2)}` : `${d} ${meses[m - 1]}`;
}

export interface Indicador {
  etiqueta: string;
  valor: string;
  nota?: string;
  tono?: 'ok' | 'aviso' | '';
}

const num = (m: Metricas, k: string): number | null => {
  const v = m[k];
  return typeof v === 'number' ? v : null;
};

/** Traduce las métricas técnicas de cada modelo a indicadores legibles para negocio. */
export function indicadores(clave: string, m: Metricas | null): Indicador[] {
  if (!m) return [];
  if (m.error) return [{ etiqueta: 'Estado', valor: 'Falló', nota: String(m.error), tono: 'aviso' }];
  if (m.omitido) return [{ etiqueta: 'Estado', valor: 'Pocos datos', nota: 'Se necesita más historia', tono: 'aviso' }];
  switch (clave) {
    case 'pronostico': {
      const mape = num(m, 'mape_total_mensual');
      const ing = num(m, 'mape_ingenuo_total_mensual');
      const mejora = mape !== null && ing ? 1 - mape / ing : null;
      return [
        { etiqueta: 'Error (MAPE)', valor: pct(mape), nota: `Ingenuo: ${pct(ing)}`, tono: mejora && mejora > 0 ? 'ok' : 'aviso' },
        { etiqueta: 'Mejora vs. ingenuo', valor: pct(mejora, 0), nota: String(m['modelo_total_mensual'] ?? '') },
        { etiqueta: 'Series', valor: String(Object.values((m['modelos_elegidos'] as Record<string, number>) ?? {}).reduce((a, b) => a + b, 0)) },
      ];
    }
    case 'abandono':
      return [
        { etiqueta: 'AUC ROC', valor: (num(m, 'auc') ?? 0).toFixed(2), nota: String(m['modelo'] ?? ''), tono: (num(m, 'auc') ?? 0) > 0.7 ? 'ok' : 'aviso' },
        { etiqueta: 'Precisión', valor: pct(num(m, 'precision'), 0), nota: `Exhaustividad ${pct(num(m, 'exhaustividad'), 0)}` },
        { etiqueta: 'Clientes', valor: String(num(m, 'clientes_puntuados') ?? '—') },
      ];
    case 'segmentacion':
      return [
        { etiqueta: 'Segmentos (k)', valor: String(num(m, 'k') ?? '—') },
        { etiqueta: 'Silueta', valor: (num(m, 'silueta') ?? 0).toFixed(2), nota: '> 0,25 es una separación razonable', tono: (num(m, 'silueta') ?? 0) > 0.25 ? 'ok' : 'aviso' },
        { etiqueta: 'Clientes', valor: String(num(m, 'clientes') ?? '—') },
      ];
    case 'canasta':
      return [
        { etiqueta: 'Reglas', valor: String(num(m, 'reglas') ?? 0) },
        { etiqueta: 'Ventas analizadas', valor: (num(m, 'ventas') ?? 0).toLocaleString('es-BO') },
        { etiqueta: 'Con varios productos', valor: (num(m, 'ventas_con_varios_productos') ?? 0).toLocaleString('es-BO') },
      ];
    case 'anomalias':
      return [
        { etiqueta: 'Días atípicos', valor: String(num(m, 'anomalias') ?? 0) },
        { etiqueta: 'Sucursales', valor: String(num(m, 'sucursales') ?? 0) },
      ];
    case 'reposicion':
      return [
        { etiqueta: 'Sugerencias', valor: String(num(m, 'filas') ?? 0) },
        { etiqueta: 'Unidades', valor: (num(m, 'unidades') ?? 0).toLocaleString('es-BO') },
      ];
    default:
      return [];
  }
}

// ------------------------------------------------------------------ gráfico de pronóstico

export interface GeometriaGrafico {
  historia: string;
  pronostico: string;
  banda: string;
  maximo: number;
  ticks: { x: number; texto: string }[];
  divisor: number | null;
  puntos: { x: number; y: number; periodo: string; valor: number; tipo: 'real' | 'pronostico'; bajo?: number; alto?: number }[];
}

/** Coordenadas SVG de la historia (línea sólida), el pronóstico (punteada) y la banda del 80 %. */
export function geometria(p: Pronostico | null, ancho: number, alto: number, margen = 8): GeometriaGrafico {
  const vacio: GeometriaGrafico = { historia: '', pronostico: '', banda: '', maximo: 0, ticks: [], divisor: null, puntos: [] };
  if (!p) return vacio;
  const hist = p.historia ?? [];
  const pron = p.pronostico ?? [];
  const n = hist.length + pron.length;
  if (n === 0) return vacio;
  const maximo = Math.max(1, ...hist.map((h) => Number(h.valor)), ...pron.map((q) => Number(q.alto)));
  const x = (i: number) => (n === 1 ? ancho / 2 : margen + (i * (ancho - 2 * margen)) / (n - 1));
  const y = (v: number) => alto - margen - (Number(v) / maximo) * (alto - 2 * margen);
  const linea = (pts: [number, number][]) => pts.map(([a, b], i) => `${i ? 'L' : 'M'}${a.toFixed(1)},${b.toFixed(1)}`).join(' ');

  const puntosHist: [number, number][] = hist.map((h, i) => [x(i), y(h.valor)]);
  const inicio = hist.length;
  const puntosPron: [number, number][] = pron.map((q, i) => [x(inicio + i), y(q.valor)]);
  // El pronóstico arranca desde el último dato real para que la línea sea continua.
  const enlace = puntosHist.length ? [puntosHist[puntosHist.length - 1]] : [];
  const arriba = pron.map((q, i) => `${x(inicio + i).toFixed(1)},${y(q.alto).toFixed(1)}`);
  const abajo = pron.map((q, i) => `${x(inicio + i).toFixed(1)},${y(q.bajo).toFixed(1)}`).reverse();

  const todos = [...hist.map((h) => h.periodo), ...pron.map((q) => q.periodo)];
  const pasos = Math.max(1, Math.ceil(n / 7));
  const ticks = todos
    .map((periodo, i) => ({ i, periodo }))
    .filter(({ i }) => i % pasos === 0 || i === n - 1)
    .map(({ i, periodo }) => ({ x: (x(i) / ancho) * 100, texto: etiquetaPeriodo(periodo, p.frecuencia) }));

  return {
    historia: linea(puntosHist),
    pronostico: pron.length ? linea([...enlace, ...puntosPron]) : '',
    banda: pron.length ? arriba.concat(abajo).join(' ') : '',
    maximo,
    ticks,
    divisor: pron.length && hist.length ? x(inicio - 0.5) : null,
    puntos: [
      ...hist.map((h, i) => ({ x: x(i), y: y(h.valor), periodo: h.periodo, valor: Number(h.valor), tipo: 'real' as const })),
      ...pron.map((q, i) => ({
        x: x(inicio + i), y: y(q.valor), periodo: q.periodo, valor: Number(q.valor), tipo: 'pronostico' as const,
        bajo: Number(q.bajo), alto: Number(q.alto),
      })),
    ],
  };
}
