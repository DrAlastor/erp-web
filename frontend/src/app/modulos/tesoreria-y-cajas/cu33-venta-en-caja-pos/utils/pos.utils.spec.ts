import { agregarProducto, calcularCambio, cambiarCantidad, redondear, totalCarrito } from './pos.utils';
import { LineaPos, ProductoPos } from '../models/venta-pos.model';

const mouse: ProductoPos = { id: 1, codigo: 'M-1', nombre: 'Mouse', precioVenta: 25, stockDisponible: 2 };
const cable: ProductoPos = { id: 2, codigo: 'C-1', nombre: 'Cable', precioVenta: 7.5, stockDisponible: 10 };
const agotado: ProductoPos = { id: 3, codigo: 'X-1', nombre: 'Teclado', precioVenta: 90, stockDisponible: 0 };

function agregar(lineas: LineaPos[], p: ProductoPos): LineaPos[] {
  const r = agregarProducto(lineas, p);
  if (!r.ok) throw new Error(r.error);
  return r.lineas;
}

describe('CU33 utilidades del POS', () => {
  it('agrega productos y suma una unidad si ya están en el ticket', () => {
    let lineas = agregar([], mouse);
    lineas = agregar(lineas, cable);
    lineas = agregar(lineas, mouse);
    expect(lineas.map((l) => [l.productoId, l.cantidad])).toEqual([[1, 2], [2, 1]]);
    expect(totalCarrito(lineas)).toBe(57.5);
  });

  it('no deja superar el stock ni agregar productos agotados', () => {
    const lineas = agregar(agregar([], mouse), mouse);
    const r = agregarProducto(lineas, mouse);
    expect(r.ok).toBe(false);
    expect(agregarProducto([], agotado)).toEqual({ ok: false, error: 'Teclado no tiene stock disponible en esta sucursal.' });
  });

  it('cambiar la cantidad la acota al stock y 0 quita la línea', () => {
    const lineas = agregar(agregar([], mouse), cable);
    expect(cambiarCantidad(lineas, 1, 99).find((l) => l.productoId === 1)?.cantidad).toBe(2);
    expect(cambiarCantidad(lineas, 2, 0).map((l) => l.productoId)).toEqual([1]);
    expect(cambiarCantidad(lineas, 2, -3).map((l) => l.productoId)).toEqual([1]);
  });

  it('calcula el cambio y marca lo que falta como negativo', () => {
    expect(calcularCambio(57.5, 100)).toBe(42.5);
    expect(calcularCambio(57.5, 50)).toBe(-7.5);
    expect(calcularCambio(10, null)).toBe(-10);
  });

  it('redondea a centavos sin errores de coma flotante', () => {
    expect(redondear(0.1 + 0.2)).toBe(0.3);
    expect(totalCarrito([{ productoId: 1, codigo: '', nombre: '', precio: 33.33, stock: 9, cantidad: 3 }])).toBe(99.99);
    expect(totalCarrito([{ productoId: 1, codigo: '', nombre: '', precio: 0.1, stock: 9, cantidad: 3 }])).toBe(0.3);
  });
});
