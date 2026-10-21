import { ColumnaReporte, ResultadoReporte, ValorCelda } from '../models/reportes.model';

const DECIMAL = new Intl.NumberFormat('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const ENTERO = new Intl.NumberFormat('es-BO', { maximumFractionDigits: 0 });

/** Formatea el valor de una celda según el tipo de su columna. */
export function formatearValor(valor: ValorCelda | undefined, columna: ColumnaReporte): string {
  if (valor === null || valor === undefined || valor === '') return '';
  switch (columna.tipo) {
    case 'MONEDA':
    case 'DECIMAL':
      return DECIMAL.format(Number(valor));
    case 'ENTERO':
      return ENTERO.format(Number(valor));
    case 'FECHA':
      return formatearFecha(String(valor));
    default:
      return String(valor);
  }
}

/** `2026-01-17` → `17/01/2026`; `2026-01-17T10:30:00` → `17/01/2026 10:30`. */
export function formatearFecha(iso: string): string {
  const [fecha, hora] = iso.split('T');
  const [a, m, d] = fecha.split('-');
  if (!a || !m || !d) return iso;
  return hora ? `${d}/${m}/${a} ${hora.slice(0, 5)}` : `${d}/${m}/${a}`;
}

export function esNumerica(columna: ColumnaReporte): boolean {
  return columna.tipo === 'MONEDA' || columna.tipo === 'DECIMAL' || columna.tipo === 'ENTERO';
}

/** Fecha local en formato `YYYY-MM-DD`, para los `<input type="date">`. */
export function fechaIso(fecha: Date): string {
  const m = String(fecha.getMonth() + 1).padStart(2, '0');
  const d = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${m}-${d}`;
}

function escapar(texto: string): string {
  return texto
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Documento HTML del reporte, listo para imprimir o guardar como PDF desde el navegador.
 * Es independiente del panel: no arrastra el menú ni los estilos de la aplicación.
 */
export function documentoImprimible(
  r: ResultadoReporte,
  marca: { logo: string | null; colorPrimario: string; empresa: string | null },
): string {
  const numerica = (c: ColumnaReporte) => (esNumerica(c) ? ' class="num"' : '');
  const encabezados = r.columnas.map((c) => `<th${numerica(c)}>${escapar(c.etiqueta)}</th>`).join('');
  const filas = r.filas.length
    ? r.filas
        .map((f) => `<tr>${r.columnas.map((c) => `<td${numerica(c)}>${escapar(formatearValor(f[c.clave], c))}</td>`).join('')}</tr>`)
        .join('')
    : `<tr><td colspan="${r.columnas.length}" class="vacio">No hay datos para los filtros elegidos.</td></tr>`;
  const hayTotales = Object.keys(r.totales).length > 0;
  const totales = hayTotales
    ? `<tfoot><tr>${r.columnas
        .map((c, i) => {
          const valor = r.totales[c.clave];
          const texto = valor !== undefined ? formatearValor(valor, c) : i === 0 ? 'Totales' : '';
          return `<td${numerica(c)}>${escapar(texto)}</td>`;
        })
        .join('')}</tr></tfoot>`
    : '';
  const filtros = Object.entries(r.filtros)
    .map(([k, v]) => `<span><b>${escapar(k)}:</b> ${escapar(v)}</span>`)
    .join('');
  const color = /^#[0-9a-f]{6}$/i.test(marca.colorPrimario) ? marca.colorPrimario : '#007AFF';
  const logo = marca.logo ? `<img src="${escapar(marca.logo)}" alt="">` : '';

  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>${escapar(r.titulo)}</title>
<style>
  @page { size: A4; margin: 14mm; }
  * { box-sizing: border-box; }
  body { font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; color: #1d1d1f; font-size: 11px; margin: 0; }
  header { display: flex; align-items: center; gap: 12px; border-bottom: 2px solid ${color}; padding-bottom: 8px; margin-bottom: 10px; }
  header img { width: 42px; height: 42px; object-fit: contain; }
  h1 { font-size: 17px; margin: 0; }
  .empresa { color: #515154; font-size: 11px; margin: 2px 0 0; }
  .meta { display: flex; flex-wrap: wrap; gap: 4px 16px; color: #515154; margin-bottom: 10px; }
  table { width: 100%; border-collapse: collapse; }
  th { text-align: left; background: #f5f5f7; border-bottom: 1px solid #d1d5db; padding: 5px 6px; }
  td { border-bottom: 1px solid #e8e8ed; padding: 4px 6px; }
  .num { text-align: right; font-variant-numeric: tabular-nums; }
  tfoot td { font-weight: 700; border-top: 2px solid #1d1d1f; border-bottom: 0; }
  .vacio { text-align: center; color: #86868b; padding: 18px; }
  tr { break-inside: avoid; }
</style></head>
<body>
  <header>${logo}<div><h1>${escapar(r.titulo)}</h1>${marca.empresa ? `<p class="empresa">${escapar(marca.empresa)}</p>` : ''}</div></header>
  <div class="meta">${filtros}<span><b>Generado:</b> ${escapar(formatearFecha(r.generadoEn))}${r.generadoPor ? ' · ' + escapar(r.generadoPor) : ''}</span></div>
  <table><thead><tr>${encabezados}</tr></thead><tbody>${filas}</tbody>${totales}</table>
</body></html>`;
}
