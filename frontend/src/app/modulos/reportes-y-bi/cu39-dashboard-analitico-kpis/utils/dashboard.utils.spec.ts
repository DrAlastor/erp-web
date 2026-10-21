import { barrasSerie, claseVariacion, fechaCorta, ordenarAlertas, porcentaje, rutaFuncion, textoVariacion } from './dashboard.utils';
import { AlertaDashboard } from '../models/dashboard-ejecutivo.model';

describe('CU39 utilidades del dashboard ejecutivo', () => {
  it('resuelve el slug del menú para el funcionId de una alerta', () => {
    expect(rutaFuncion('gestionar-cuentas-por-pagar')).toBe('gestionar-cuentas-por-pagar');
    expect(rutaFuncion('cu12-consultar-kardex')).toBe('consultar-kardex');
    expect(rutaFuncion('no-existe')).toBeNull();
    expect(rutaFuncion(null)).toBeNull();
  });

  it('calcula barras proporcionales y marca mínima para días sin venta', () => {
    const barras = barrasSerie([{ fecha: '2026-10-01', total: 0 }, { fecha: '2026-10-02', total: 50 }, { fecha: '2026-10-03', total: 100 }], 300, 100, 0);
    expect(barras.map((b) => b.x)).toEqual([0, 100, 200]);
    expect(barras[2].alto).toBe(100);
    expect(barras[1].alto).toBe(50);
    expect(barras[0].alto).toBeGreaterThan(0);
    expect(barrasSerie([], 300, 100)).toEqual([]);
  });

  it('formatea la variación y su tono', () => {
    expect(textoVariacion(12.345)).toBe('+12.3 % vs. mismo período del mes anterior');
    expect(textoVariacion(null)).toContain('Sin comparación');
    expect(claseVariacion(-3)).toBe('negativa');
    expect(claseVariacion(null)).toBe('neutra');
  });

  it('ordena alertas por severidad y acota porcentajes', () => {
    const alerta = (severidad: AlertaDashboard['severidad']): AlertaDashboard =>
      ({ tipo: 'STOCK_BAJO', severidad, mensaje: severidad, funcionId: 'consultar-kardex' });
    expect(ordenarAlertas([alerta('INFO'), alerta('ALTA'), alerta('MEDIA')]).map((a) => a.severidad)).toEqual(['ALTA', 'MEDIA', 'INFO']);
    expect(porcentaje(150, 100)).toBe(100);
    expect(porcentaje(5, 0)).toBe(0);
    expect(fechaCorta('2026-10-21')).toBe('21/10');
  });
});
