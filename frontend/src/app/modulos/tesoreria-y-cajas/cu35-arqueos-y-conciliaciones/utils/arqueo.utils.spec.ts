import { calcularDiferencia, claseResultado, resultadoArqueo, validarCierre, validarRango } from './arqueo.utils';

describe('CU35 utilidades de arqueo de caja', () => {
  it('calcula la diferencia contado − esperado sin errores de coma flotante', () => {
    expect(calcularDiferencia(100.3, 100.1)).toBe(0.2);
    expect(calcularDiferencia(99.9, 100)).toBe(-0.1);
    expect(calcularDiferencia(250, 250)).toBe(0);
    expect(Object.is(calcularDiferencia(0, 0), -0)).toBe(false);
    expect(calcularDiferencia(null, 100)).toBeNull();
  });

  it('clasifica el resultado del arqueo', () => {
    expect(resultadoArqueo(0)).toBe('CUADRA');
    expect(resultadoArqueo(5.5)).toBe('SOBRANTE');
    expect(resultadoArqueo(-0.01)).toBe('FALTANTE');
    expect(resultadoArqueo(null)).toBeNull();
    expect(claseResultado('FALTANTE')).toBe('vencida');
  });

  it('exige observaciones solo si hay diferencia', () => {
    expect(validarCierre(100, 100, '')).toBe('');
    expect(validarCierre(90, 100, '')).toContain('observaciones');
    expect(validarCierre(90, 100, 'Faltante por cambio mal dado')).toBe('');
    expect(validarCierre(null, 100, '')).toContain('monto contado');
    expect(validarCierre(-1, 100, 'x')).toContain('0 o mayor');
    expect(validarCierre(10.123, 10.123, '')).toContain('2 decimales');
  });

  it('valida el rango de fechas del historial', () => {
    expect(validarRango('2026-11-01', '2026-11-17')).toBe('');
    expect(validarRango('2026-11-18', '2026-11-17')).toContain('posterior');
    expect(validarRango('', '2026-11-17')).toBe('');
  });
});
