import { HttpErrorResponse } from '@angular/common/http';
import { ERP_MODULES } from '../../../../core/models/erp-navigation';
import { Pronostico } from '../models/inteligencia-negocio.model';
import { params } from '../services/inteligencia-negocio.service';
import { VISTAS, bs, etiquetaPeriodo, geometria, indicadores, mensajeError, pct, vistaDesdeSlug } from './ml.utils';

const pronostico = (over: Partial<Pronostico> = {}): Pronostico => ({
  entrenado: true, disponible: true, aviso: null, frecuencia: 'MENSUAL', ambito: 'TOTAL', clave: 'TOTAL',
  modelo: 'HOLT', origen: 'PRECALCULADO', mape: 0.05, mapeIngenuo: 0.12,
  historia: [{ periodo: '2026-09-01', valor: 100 }, { periodo: '2026-10-01', valor: 200 }],
  pronostico: [{ periodo: '2026-11-01', valor: 150, bajo: 100, alto: 400 }],
  ...over,
});

describe('CU40 utilidades de Inteligencia de Negocio', () => {
  it('cada entrada del sidebar abre una pestaña y todas existen en la navegación', () => {
    const modulo = ERP_MODULES.find((m) => m.id === 'inteligencia-de-negocio');
    expect(modulo?.name).toBe('Inteligencia de Negocio');
    expect(modulo?.functions.map((f) => f.slug)).toEqual(VISTAS.map((v) => v.slug));
    expect(vistaDesdeSlug('riesgo-de-abandono')).toBe('abandono');
    expect(vistaDesdeSlug('sugerencia-de-compra')).toBe('reposicion');
    expect(vistaDesdeSlug('otra-cosa')).toBe('panel');
    expect(vistaDesdeSlug(undefined)).toBe('panel');
  });

  it('formatea porcentajes, montos y periodos', () => {
    expect(pct(0.0578)).toBe('5.8 %');
    expect(pct(null)).toBe('—');
    expect(bs(null)).toBe('—');
    expect(etiquetaPeriodo('2026-12-01', 'MENSUAL')).toBe('dic 26');
    expect(etiquetaPeriodo('2026-11-16', 'SEMANAL')).toBe('16 nov');
  });

  it('traduce métricas a indicadores y marca modelos sin datos o con error', () => {
    const pron = indicadores('pronostico', { mape_total_mensual: 0.05, mape_ingenuo_total_mensual: 0.1, modelos_elegidos: { HOLT: 3, INGENUO: 2 } });
    expect(pron[0].valor).toBe('5.0 %');
    expect(pron[0].tono).toBe('ok');
    expect(pron[1].valor).toBe('50 %');
    expect(pron[2].valor).toBe('5');
    expect(indicadores('abandono', { auc: 0.88, modelo: 'RANDOM_FOREST' })[0].valor).toBe('0.88');
    expect(indicadores('canasta', { omitido: 'pocos datos' })[0].valor).toBe('Pocos datos');
    expect(indicadores('anomalias', { error: 'ValueError' })[0].valor).toBe('Falló');
    expect(indicadores('segmentacion', null)).toEqual([]);
  });

  it('calcula la geometría del gráfico con banda y divisor entre real y pronóstico', () => {
    const g = geometria(pronostico(), 300, 100, 0);
    expect(g.maximo).toBe(400);
    expect(g.historia.startsWith('M0.0,75.0')).toBe(true);
    expect(g.pronostico.startsWith('M150.0,50.0')).toBe(true); // arranca en el último dato real
    expect(g.banda.split(' ').length).toBe(2);
    expect(g.divisor).toBe(225);
    expect(g.puntos.length).toBe(3);
    expect(g.puntos[2].tipo).toBe('pronostico');
  });

  it('el gráfico sin datos queda vacío', () => {
    expect(geometria(null, 300, 100).historia).toBe('');
    const g = geometria(pronostico({ pronostico: [] }), 300, 100);
    expect(g.pronostico).toBe('');
    expect(g.divisor).toBeNull();
  });

  it('los filtros vacíos no se envían', () => {
    const p = params({ frecuencia: 'MENSUAL', sucursalId: null, categoria: '', horizonte: 18 });
    expect(p.keys()).toEqual(['frecuencia', 'horizonte']);
  });

  it('muestra mensajes de error claros', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 0 }))).toContain('conectar');
    expect(mensajeError(new HttpErrorResponse({ status: 403 }))).toContain('permiso');
    expect(mensajeError(new HttpErrorResponse({ status: 503, error: { message: 'ML caído' } }))).toBe('ML caído');
  });
});
