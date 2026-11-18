import { LineaPos, ProductoPos } from '../models/venta-pos.model';

export function redondear(v: number): number {
  return Math.round((v + Number.EPSILON) * 100) / 100;
}

export function totalCarrito(lineas: LineaPos[]): number {
  // Igual que el backend (CalculadoraCobroPos): se redondea el precio y luego el importe de cada línea.
  return redondear(lineas.reduce((acc, l) => acc + redondear(l.cantidad * redondear(l.precio)), 0));
}

/** Cambio a entregar; negativo significa que falta dinero. */
export function calcularCambio(total: number, recibido: number | null): number {
  return redondear((recibido ?? 0) - total);
}

export type ResultadoAgregar = { ok: true; lineas: LineaPos[] } | { ok: false; error: string };

/** Agrega una unidad del producto sin superar el stock disponible. */
export function agregarProducto(lineas: LineaPos[], p: ProductoPos): ResultadoAgregar {
  if (p.stockDisponible <= 0) return { ok: false, error: `${p.nombre} no tiene stock disponible en esta sucursal.` };
  const existente = lineas.find((l) => l.productoId === p.id);
  if (existente) {
    if (existente.cantidad + 1 > existente.stock) return { ok: false, error: `Solo hay ${existente.stock} unidades de ${p.nombre}.` };
    return { ok: true, lineas: lineas.map((l) => (l === existente ? { ...l, cantidad: l.cantidad + 1 } : l)) };
  }
  return {
    ok: true,
    lineas: [...lineas, { productoId: p.id, codigo: p.codigo, nombre: p.nombre, precio: p.precioVenta, stock: p.stockDisponible, cantidad: 1 }],
  };
}

/** Cambia la cantidad acotándola a [0, stock]; 0 quita la línea. */
export function cambiarCantidad(lineas: LineaPos[], productoId: number, cantidad: number): LineaPos[] {
  return lineas.flatMap((l) => {
    if (l.productoId !== productoId) return [l];
    const valor = Math.max(0, Math.min(Number(cantidad) || 0, l.stock));
    return valor === 0 ? [] : [{ ...l, cantidad: valor }];
  });
}
