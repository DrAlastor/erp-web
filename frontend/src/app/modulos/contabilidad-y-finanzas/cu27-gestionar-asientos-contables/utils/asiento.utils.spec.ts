import { erroresAsiento, totales } from './asiento.utils';

const linea = (cuentaId: number | null, debe: number | null, haber: number | null) => ({ cuentaId, glosa: null, debe, haber });

describe('CU27 asiento.utils', () => {
  it('suma en centavos y detecta el cuadre', () => {
    const t = totales([linea(1, 0.1, null), linea(1, 0.2, null), linea(2, null, 0.3)]);
    expect(t.debe).toBe(0.3);
    expect(t.cuadra).toBe(true);
  });

  it('informa la diferencia de un asiento descuadrado', () => {
    const errores = erroresAsiento('Glosa', [linea(1, 100, null), linea(2, null, 90)]);
    expect(errores).toContain('El asiento no cuadra: diferencia de Bs 10.00.');
  });

  it('rechaza líneas con importe en ambos lados, sin importe o sin cuenta', () => {
    const errores = erroresAsiento('Glosa', [linea(1, 5, 5), linea(null, null, null)]);
    expect(errores).toContain('Línea 1: el importe va en el debe o en el haber, no en ambos.');
    expect(errores).toContain('Línea 2: elige una cuenta.');
    expect(errores).toContain('Línea 2: falta el importe.');
  });

  it('un asiento correcto no tiene errores', () => {
    expect(erroresAsiento('Depósito', [linea(1, 500, null), linea(2, null, 500)])).toEqual([]);
  });

  it('exige glosa', () => {
    expect(erroresAsiento('  ', [linea(1, 5, null), linea(2, null, 5)])).toContain('La glosa es obligatoria.');
  });
});
