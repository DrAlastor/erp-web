/** Utilidades para derivar los tonos de la paleta a partir de un color #RRGGBB. */

export interface Rgb {
  r: number;
  g: number;
  b: number;
}

const HEX = /^#[0-9a-f]{6}$/i;

export function esHexValido(valor: string | null | undefined): boolean {
  return !!valor && HEX.test(valor);
}

export function hexARgb(hex: string): Rgb {
  const limpio = hex.replace('#', '');
  return {
    r: parseInt(limpio.slice(0, 2), 16),
    g: parseInt(limpio.slice(2, 4), 16),
    b: parseInt(limpio.slice(4, 6), 16),
  };
}

export function rgbAHex({ r, g, b }: Rgb): string {
  const canal = (v: number) => Math.round(Math.min(255, Math.max(0, v))).toString(16).padStart(2, '0');
  return `#${canal(r)}${canal(g)}${canal(b)}`.toUpperCase();
}

/** Mezcla el color con blanco (`cantidad` > 0) o con negro (`cantidad` < 0), entre -1 y 1. */
export function mezclar(hex: string, cantidad: number): string {
  const { r, g, b } = hexARgb(hex);
  const destino = cantidad >= 0 ? 255 : 0;
  const t = Math.min(1, Math.abs(cantidad));
  return rgbAHex({ r: r + (destino - r) * t, g: g + (destino - g) * t, b: b + (destino - b) * t });
}

export const aclarar = (hex: string, cantidad: number) => mezclar(hex, Math.abs(cantidad));
export const oscurecer = (hex: string, cantidad: number) => mezclar(hex, -Math.abs(cantidad));

export function rgba(hex: string, alfa: number): string {
  const { r, g, b } = hexARgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${alfa})`;
}

/** Luminancia relativa (WCAG), de 0 (negro) a 1 (blanco). */
export function luminancia(hex: string): number {
  const lineal = (c: number) => {
    const v = c / 255;
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  };
  const { r, g, b } = hexARgb(hex);
  return 0.2126 * lineal(r) + 0.7152 * lineal(g) + 0.0722 * lineal(b);
}

/** Texto legible (blanco o casi negro) sobre un fondo del color dado. */
export function textoSobre(hex: string): string {
  return luminancia(hex) > 0.45 ? '#1D1D1F' : '#FFFFFF';
}

export interface TonosColor {
  base: string;
  hover: string;
  active: string;
  light: string;
  glow: string;
}

/**
 * Tonos de un color de marca para las variables `--primary-*`.
 *
 * En modo oscuro el color se aclara un poco, como hace la paleta original (#007AFF → #0A84FF),
 * para que conserve el contraste sobre fondos negros.
 */
export function tonosDe(hex: string, oscuro: boolean): TonosColor {
  const base = oscuro ? aclarar(hex, 0.08) : hex.toUpperCase();
  return {
    base,
    hover: oscuro ? aclarar(base, 0.18) : oscurecer(base, 0.14),
    active: oscurecer(base, oscuro ? 0.12 : 0.28),
    light: rgba(base, oscuro ? 0.16 : 0.1),
    glow: `0 8px 24px ${rgba(base, oscuro ? 0.3 : 0.22)}`,
  };
}
