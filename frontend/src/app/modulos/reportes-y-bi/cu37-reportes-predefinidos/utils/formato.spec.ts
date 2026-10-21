import { documentoImprimible, fechaIso, formatearFecha, formatearValor } from './formato';
import { ColumnaReporte, ResultadoReporte } from '../models/reportes.model';

const moneda: ColumnaReporte = { clave: 'total', etiqueta: 'Total', tipo: 'MONEDA', totalizar: true };
const entero: ColumnaReporte = { clave: 'ventas', etiqueta: 'Ventas', tipo: 'ENTERO', totalizar: true };
const fecha: ColumnaReporte = { clave: 'dia', etiqueta: 'Día', tipo: 'FECHA', totalizar: false };
const texto: ColumnaReporte = { clave: 'producto', etiqueta: 'Producto', tipo: 'TEXTO', totalizar: false };

describe('CU37 formato de reportes', () => {
  it('formatea montos con dos decimales y enteros sin decimales', () => {
    expect(formatearValor(33850, moneda)).toMatch(/^33[.\s ]?850,00$/);
    expect(formatearValor(2, entero)).toBe('2');
    expect(formatearValor(null, moneda)).toBe('');
  });

  it('muestra las fechas en formato dd/mm/aaaa', () => {
    expect(formatearValor('2026-01-17', fecha)).toBe('17/01/2026');
    expect(formatearFecha('2026-10-21T09:51:58.47')).toBe('21/10/2026 09:51');
    expect(fechaIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('el documento imprimible escapa el HTML de los datos y trae los totales', () => {
    const r: ResultadoReporte = {
      codigo: 'X', titulo: 'Ventas <b>', categoria: 'Ventas', generadoEn: '2026-01-17T10:30:00', generadoPor: 'admin',
      filtros: { Sucursal: 'Central' },
      columnas: [texto, moneda],
      filas: [{ producto: '<script>alert(1)</script>', total: 10 }],
      totales: { total: 10 },
    };

    const html = documentoImprimible(r, { logo: null, colorPrimario: '#E11D48', empresa: 'Mi Empresa' });

    expect(html).not.toContain('<script>alert(1)</script>');
    expect(html).toContain('&lt;script&gt;');
    expect(html).toContain('Ventas &lt;b&gt;');
    expect(html).toContain('Totales');
    expect(html).toContain('#E11D48');
    expect(html).toContain('Mi Empresa');
  });

  it('un color de marca inválido no se inyecta en el CSS de impresión', () => {
    const r: ResultadoReporte = {
      codigo: 'X', titulo: 'T', categoria: 'C', generadoEn: '2026-01-17', generadoPor: null,
      filtros: {}, columnas: [texto], filas: [], totales: {},
    };

    const html = documentoImprimible(r, { logo: null, colorPrimario: 'red;}</style><script>', empresa: null });

    expect(html).toContain('#007AFF');
    expect(html).not.toContain('</style><script>');
    expect(html).toContain('No hay datos');
  });
});
