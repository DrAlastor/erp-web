import { Fila, HerramientaUsada, Visual } from '../models/agente.model';

/** CU41 — geometría de los gráficos del asistente (SVG propio, sin librerías). */

export const PALETA = ['#007AFF', '#34C759', '#FF9500', '#AF52DE', '#FF3B30', '#5AC8FA', '#FFCC00', '#8E8E93'];

export const numero = (v: unknown): number => {
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : 0;
};

export const etiqueta = (columna: string): string =>
  columna.replace(/_bs$/, ' (Bs)').replace(/_pct$/, ' (%)').replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());

/**
 * Hasta 2 gráficos por respuesta: primero línea, barras y torta; las tablas solo si no hay otro.
 */
export function graficosDeRespuesta(herramientas: HerramientaUsada[]): HerramientaUsada[] {
  const con = herramientas.filter((h) => h.ok && h.grafico && h.resultado?.filas?.length);
  const visuales = con.filter((h) => h.grafico!.tipo !== 'tabla');
  return (visuales.length ? visuales : con).slice(0, 2);
}

export interface Serie {
  nombre: string;
  color: string;
  ruta: string;
  puntos: { x: number; y: number; valor: number; etiqueta: string }[];
}

export interface GeometriaLinea {
  series: Serie[];
  banda: string;
  maximo: number;
  ticks: { x: number; texto: string }[];
}

export function geometriaLinea(v: Visual, filas: Fila[], ancho: number, alto: number, m = 10): GeometriaLinea {
  const n = filas.length;
  const valores = filas.flatMap((f) => [...v.series.map((s) => numero(f[s])), ...(v.rango ? [numero(f[v.rango.alto])] : [])]);
  const maximo = Math.max(1, ...valores);
  const x = (i: number) => (n <= 1 ? ancho / 2 : m + (i * (ancho - 2 * m)) / (n - 1));
  const y = (val: number) => alto - m - (val / maximo) * (alto - 2 * m);
  const ejeX = v.x ?? '';
  const series = v.series.map((s, k) => {
    const puntos = filas.map((f, i) => ({ x: x(i), y: y(numero(f[s])), valor: numero(f[s]), etiqueta: String(f[ejeX] ?? i + 1) }));
    return {
      nombre: s, color: PALETA[k % PALETA.length], puntos,
      ruta: puntos.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' '),
    };
  });
  let banda = '';
  if (v.rango) {
    const arriba = filas.map((f, i) => `${x(i).toFixed(1)},${y(numero(f[v.rango!.alto])).toFixed(1)}`);
    const abajo = filas.map((f, i) => `${x(i).toFixed(1)},${y(numero(f[v.rango!.bajo])).toFixed(1)}`).reverse();
    banda = [...arriba, ...abajo].join(' ');
  }
  const paso = Math.max(1, Math.ceil(n / 6));
  const ticks = filas
    .map((f, i) => ({ i, texto: String(f[ejeX] ?? '') }))
    .filter(({ i }) => i % paso === 0 || i === n - 1)
    .map(({ i, texto }) => ({ x: (x(i) / ancho) * 100, texto }));
  return { series, banda, maximo, ticks };
}

export interface Barra {
  etiqueta: string;
  valor: number;
  ancho: number; // % del máximo absoluto
  negativo: boolean;
}

/** Barras horizontales (admite negativos, p. ej. descomponer_variacion). */
export function barras(v: Visual, filas: Fila[], limite = 15): Barra[] {
  const serie = v.series[0];
  const datos = filas.slice(0, limite).map((f) => ({ etiqueta: String(f[v.x ?? ''] ?? ''), valor: numero(f[serie]) }));
  const maximo = Math.max(1e-9, ...datos.map((d) => Math.abs(d.valor)));
  return datos.map((d) => ({ ...d, ancho: (Math.abs(d.valor) / maximo) * 100, negativo: d.valor < 0 }));
}

export interface Porcion {
  etiqueta: string;
  valor: number;
  porcentaje: number;
  ruta: string;
  color: string;
}

/** Porciones de una torta de radio r centrada en (r, r). Los valores negativos se ignoran. */
export function porciones(v: Visual, filas: Fila[], r = 80): Porcion[] {
  const serie = v.series[0];
  const datos = filas.map((f) => ({ etiqueta: String(f[v.x ?? ''] ?? ''), valor: Math.max(0, numero(f[serie])) }));
  const total = datos.reduce((a, d) => a + d.valor, 0);
  if (total <= 0) return [];
  let angulo = -Math.PI / 2;
  return datos.map((d, i) => {
    const fraccion = d.valor / total;
    const fin = angulo + fraccion * 2 * Math.PI;
    const p = (a: number) => `${(r + r * Math.cos(a)).toFixed(2)},${(r + r * Math.sin(a)).toFixed(2)}`;
    const ruta = fraccion >= 0.9999
      ? `M${r},0 A${r},${r} 0 1,1 ${r - 0.01},0 Z`
      : `M${r},${r} L${p(angulo)} A${r},${r} 0 ${fraccion > 0.5 ? 1 : 0},1 ${p(fin)} Z`;
    angulo = fin;
    return { etiqueta: d.etiqueta, valor: d.valor, porcentaje: fraccion * 100, ruta, color: PALETA[i % PALETA.length] };
  });
}

/** Columnas visibles de una tabla de resultado (incluye privadas: la pantalla sí puede mostrarlas). */
export function columnas(filas: Fila[]): string[] {
  return filas.length ? Object.keys(filas[0]) : [];
}

export function formato(v: unknown): string {
  if (v === null || v === undefined) return '—';
  if (typeof v === 'number') return v.toLocaleString('es-BO', { maximumFractionDigits: 2 });
  if (typeof v === 'string' && /^-?\d+(\.\d+)?(E[+-]?\d+)?$/i.test(v)) return Number(v).toLocaleString('es-BO', { maximumFractionDigits: 2 });
  return String(v);
}
