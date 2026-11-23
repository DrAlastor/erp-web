import { HerramientaUsada, Visual } from '../models/agente.model';
import { barras, formato, geometriaLinea, graficosDeRespuesta, porciones } from './graficos';
import { bloques, fragmentos } from './markdown';

const visual = (tipo: Visual['tipo'], extra: Partial<Visual> = {}): Visual =>
  ({ titulo: 't', tipo, x: 'grupo', series: ['ventas_bs'], rango: null, ...extra });

const usada = (nombre: string, tipo: Visual['tipo'] | null, filas = 3, ok = true): HerramientaUsada => ({
  nombre, parametros: {}, ok,
  grafico: tipo ? visual(tipo) : null,
  resultado: { filas: Array.from({ length: filas }, (_, i) => ({ grupo: `g${i}`, ventas_bs: i + 1 })), resumen: null, nota: null, disponible: null, columnasPrivadas: [] },
});

describe('CU41 Markdown mínimo y seguro', () => {
  it('interpreta negrita, cursiva y código sin generar HTML', () => {
    expect(fragmentos('Vendiste **Bs 100** en *octubre* con `ventas_por_periodo`.')).toEqual([
      { tipo: 'texto', texto: 'Vendiste ' }, { tipo: 'negrita', texto: 'Bs 100' }, { tipo: 'texto', texto: ' en ' },
      { tipo: 'cursiva', texto: 'octubre' }, { tipo: 'texto', texto: ' con ' }, { tipo: 'codigo', texto: 'ventas_por_periodo' },
      { tipo: 'texto', texto: '.' },
    ]);
  });

  it('un intento de HTML queda como texto literal', () => {
    const b = bloques('<img src=x onerror="alert(1)"> hola');
    expect(b).toEqual([{ tipo: 'parrafo', partes: [{ tipo: 'texto', texto: '<img src=x onerror="alert(1)"> hola' }] }]);
  });

  it('arma párrafos, títulos, listas y tablas', () => {
    const b = bloques('## Resumen\nVentas de octubre.\nSiguen subiendo.\n\n- uno\n- **dos**\n\n1. a\n2. b\n\n| Mes | Bs |\n|---|---:|\n| oct | 10 |\n| nov | 20 |');
    expect(b.map((x) => x.tipo)).toEqual(['titulo', 'parrafo', 'lista', 'lista', 'tabla']);
    expect((b[1] as { partes: { texto: string }[] }).partes[0].texto).toBe('Ventas de octubre. Siguen subiendo.');
    expect((b[2] as { ordenada: boolean; items: unknown[] }).ordenada).toBe(false);
    expect((b[3] as { ordenada: boolean }).ordenada).toBe(true);
    const tabla = b[4] as { encabezado: { texto: string }[][]; filas: unknown[][] };
    expect(tabla.encabezado.map((c) => c[0].texto)).toEqual(['Mes', 'Bs']);
    expect(tabla.filas.length).toBe(2);
  });
});

describe('CU41 gráficos de la respuesta', () => {
  it('máximo 2 por respuesta y las tablas solo si no hay otros', () => {
    const r = graficosDeRespuesta([usada('a', 'tabla'), usada('b', 'linea'), usada('c', 'barras'), usada('d', 'torta')]);
    expect(r.map((h) => h.nombre)).toEqual(['b', 'c']);
    expect(graficosDeRespuesta([usada('a', 'tabla'), usada('x', null)]).map((h) => h.nombre)).toEqual(['a']);
    expect(graficosDeRespuesta([usada('a', 'barras', 0), usada('b', 'linea', 3, false)])).toEqual([]);
  });

  it('línea con banda de pronóstico usa el alto como máximo', () => {
    const v = visual('linea', { x: 'periodo', series: ['valor'], rango: { bajo: 'bajo', alto: 'alto' } });
    const g = geometriaLinea(v, [{ periodo: 'a', valor: 10, bajo: 5, alto: 20 }, { periodo: 'b', valor: 15, bajo: 8, alto: 40 }], 100, 100, 0);
    expect(g.maximo).toBe(40);
    expect(g.series[0].ruta).toBe('M0.0,75.0 L100.0,62.5');
    expect(g.banda.split(' ').length).toBe(4);
  });

  it('barras admiten valores negativos (descomposición de la variación)', () => {
    const b = barras(visual('barras', { series: ['diferencia_bs'] }), [{ grupo: 'Laptops', diferencia_bs: -100 }, { grupo: 'Redes', diferencia_bs: 50 }]);
    expect(b[0]).toEqual({ etiqueta: 'Laptops', valor: -100, ancho: 100, negativo: true });
    expect(b[1].ancho).toBe(50);
  });

  it('la torta suma 100 % e ignora negativos', () => {
    const p = porciones(visual('torta', { x: 'segmento', series: ['clientes'] }), [{ segmento: 'A', clientes: 3 }, { segmento: 'B', clientes: 1 }, { segmento: 'C', clientes: -5 }]);
    expect(p.map((x) => Math.round(x.porcentaje))).toEqual([75, 25, 0]);
    expect(p[0].ruta.startsWith('M80,80')).toBe(true);
    expect(porciones(visual('torta'), [{ grupo: 'x', ventas_bs: 0 }])).toEqual([]);
  });

  it('formatea números aunque lleguen en notación científica', () => {
    expect(formato(null)).toBe('—');
    expect(formato('texto')).toBe('texto');
    expect(formato('5.1E+2')).toBe(Number(510).toLocaleString('es-BO'));
  });
});
