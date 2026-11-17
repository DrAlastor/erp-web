import { errorMonto, saldoTrasCobro, textoVencimiento } from './cxc.utils';
import { CuentaPorCobrar } from '../models/cuenta-por-cobrar.model';

describe('CU29 cxc.utils', () => {
  it('valida el monto contra el saldo', () => {
    expect(errorMonto(0, 100)).toContain('mayor a cero');
    expect(errorMonto(100.01, 100)).toContain('supera el saldo');
    expect(errorMonto(10.555, 100)).toContain('2 decimales');
    expect(errorMonto(100, 100)).toBe('');
  });

  it('describe el vencimiento', () => {
    const hoy = new Date(2026, 10, 17);
    expect(textoVencimiento('2026-11-17', false, 0, hoy)).toBe('vence hoy');
    expect(textoVencimiento('2026-11-20', false, 0, hoy)).toBe('vence en 3 días');
    expect(textoVencimiento('2026-10-01', true, 47, hoy)).toBe('vencida hace 47 días');
  });

  it('calcula el saldo que dejó cada recibo', () => {
    const cuenta = {
      cuota: { montoTotal: 1000 },
      cobros: [
        { id: 7, monto: 300 },
        { id: 4, monto: 400 },
      ],
    } as unknown as CuentaPorCobrar;
    expect(saldoTrasCobro(cuenta, cuenta.cobros[1])).toBe(600);
    expect(saldoTrasCobro(cuenta, cuenta.cobros[0])).toBe(300);
  });
});
