import { aNumero, fechaHoraCorta, validarApertura, validarMovimiento } from './caja.utils';

describe('CU34 utilidades de caja', () => {
  it('valida la apertura (sucursal y monto >= 0 con 2 decimales)', () => {
    expect(validarApertura({ sucursalId: 1, montoApertura: 0 })).toBe('');
    expect(validarApertura({ montoApertura: 100 })).toContain('sucursal');
    expect(validarApertura({ sucursalId: 1, montoApertura: -1 })).toContain('0 o mayor');
    expect(validarApertura({ sucursalId: 1, montoApertura: 1.005 })).toContain('2 decimales');
  });

  it('valida el movimiento igual que el backend', () => {
    const ok = { tipo: 'EGRESO' as const, formaPagoId: 1, concepto: 'Movilidad', monto: 15.5 };
    expect(validarMovimiento(ok)).toBe('');
    expect(validarMovimiento({ ...ok, concepto: 'ab' })).toContain('entre 3 y 255');
    expect(validarMovimiento({ ...ok, monto: 0 })).toContain('mayor a 0');
    expect(validarMovimiento({ ...ok, formaPagoId: 0 })).toContain('forma de pago');
  });

  it('formatea fechas ISO y convierte inputs numéricos', () => {
    expect(fechaHoraCorta('2026-11-17T08:30:15')).toBe('17/11/2026 08:30');
    expect(fechaHoraCorta(null)).toBe('—');
    expect(aNumero('')).toBeNull();
    expect(aNumero('12.5')).toBe(12.5);
  });
});
