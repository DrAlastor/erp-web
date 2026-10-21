import { aclarar, esHexValido, hexARgb, luminancia, oscurecer, rgba, textoSobre, tonosDe } from './colores';

describe('CU06 utilidades de color', () => {
  it('valida el formato #RRGGBB', () => {
    expect(esHexValido('#007AFF')).toBe(true);
    expect(esHexValido('#007aff')).toBe(true);
    expect(esHexValido('007AFF')).toBe(false);
    expect(esHexValido('#07F')).toBe(false);
    expect(esHexValido(null)).toBe(false);
  });

  it('convierte hex a rgb y arma rgba', () => {
    expect(hexARgb('#007AFF')).toEqual({ r: 0, g: 122, b: 255 });
    expect(rgba('#007AFF', 0.1)).toBe('rgba(0, 122, 255, 0.1)');
  });

  it('aclara hacia blanco y oscurece hacia negro', () => {
    expect(aclarar('#000000', 1)).toBe('#FFFFFF');
    expect(oscurecer('#FFFFFF', 1)).toBe('#000000');
    expect(aclarar('#808080', 0)).toBe('#808080');
  });

  it('elige texto legible según el fondo', () => {
    expect(luminancia('#FFFFFF')).toBeCloseTo(1);
    expect(textoSobre('#FFFFFF')).toBe('#1D1D1F');
    expect(textoSobre('#1D1D1F')).toBe('#FFFFFF');
  });

  it('deriva los tonos de la marca y aclara la base en modo oscuro', () => {
    const claro = tonosDe('#E11D48', false);
    expect(claro.base).toBe('#E11D48');
    expect(luminancia(claro.hover)).toBeLessThan(luminancia(claro.base));
    expect(claro.light).toBe('rgba(225, 29, 72, 0.1)');

    const oscuro = tonosDe('#E11D48', true);
    expect(luminancia(oscuro.base)).toBeGreaterThan(luminancia('#E11D48'));
  });
});
