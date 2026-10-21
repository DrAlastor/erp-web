import {
  estadoVisible, previsualizarCuotas, primerVencimientoSugerido, segmentosAntiguedad, sumarDias, validarGeneracion, validarPago,
} from './cxp.utils';

describe('CU34 utilidades de cuentas por pagar', () => {
  it('reparte el total en cuotas y la última absorbe el redondeo', () => {
    const cuotas = previsualizarCuotas(100, 3, '2026-11-01', 30);
    expect(cuotas.map((c) => c.monto)).toEqual([33.33, 33.33, 33.34]);
    expect(cuotas.map((c) => c.fechaVencimiento)).toEqual(['2026-11-01', '2026-12-01', '2026-12-31']);
    expect(previsualizarCuotas(100, 0, '2026-11-01', 30)).toEqual([]);
  });

  it('sugiere el primer vencimiento con los días de crédito o +30', () => {
    const compra = { compraId: 1, numeroFactura: 'F-1', proveedorRazonSocial: 'P', fecha: '2026-01-31', total: 10, diasCredito: 15 };
    expect(primerVencimientoSugerido(compra)).toBe('2026-02-15');
    expect(primerVencimientoSugerido({ ...compra, diasCredito: null })).toBe('2026-03-02');
    expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('valida el pago contra el saldo y la fecha', () => {
    expect(validarPago({ formaPagoId: 1, monto: 50 }, 50, '2026-10-21')).toBe('');
    expect(validarPago({ formaPagoId: 1, monto: 50.01 }, 50, '2026-10-21')).toContain('saldo');
    expect(validarPago({ formaPagoId: 1, monto: 0 }, 50, '2026-10-21')).toContain('mayor a 0');
    expect(validarPago({ monto: 10 }, 50, '2026-10-21')).toContain('forma de pago');
    expect(validarPago({ formaPagoId: 1, monto: 10, fechaPago: '2026-10-22' }, 50, '2026-10-21')).toContain('futura');
  });

  it('valida los límites de la generación de cuotas', () => {
    expect(validarGeneracion(1, 24, 365)).toBe('');
    expect(validarGeneracion(null, 3, 30)).toContain('compra');
    expect(validarGeneracion(1, 25, 30)).toContain('1 y 24');
    expect(validarGeneracion(1, 3, 0)).toContain('1 y 365');
  });

  it('deriva VENCIDA y completa los rangos de antigüedad', () => {
    expect(estadoVisible({ estado: 'PARCIAL', vencida: true })).toBe('VENCIDA');
    expect(estadoVisible({ estado: 'PAGADA', vencida: true })).toBe('PAGADA');
    const seg = segmentosAntiguedad([{ rango: '90+', monto: 75, cantidad: 1 }, { rango: '0-30', monto: 25, cantidad: 2 }]);
    expect(seg.map((s) => s.rango)).toEqual(['0-30', '31-60', '61-90', '90+']);
    expect(seg.map((s) => s.pct)).toEqual([25, 0, 0, 75]);
  });
});
