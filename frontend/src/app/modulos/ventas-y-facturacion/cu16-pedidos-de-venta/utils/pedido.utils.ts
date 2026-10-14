import { HttpErrorResponse } from '@angular/common/http';
import { LineaPedido, PedidoRequest } from '../models/pedido.model';

const round2 = (value: number) => Math.round((Number(value) || 0) * 100) / 100;

export function subtotalLinea(linea: LineaPedido): number {
  return round2(linea.cantidad * linea.precioUnitario - (linea.descuento || 0));
}

export function subtotalPedido(lineas: readonly LineaPedido[]): number {
  return round2(lineas.reduce((acc, linea) => acc + subtotalLinea(linea), 0));
}

export function sinStockSuficiente(linea: LineaPedido): boolean {
  return linea.stock !== null && linea.cantidad > linea.stock;
}

/** Mismas reglas que valida el backend; devuelve el primer problema encontrado o ''. */
export function validarPedido(body: Partial<PedidoRequest>, lineas: readonly LineaPedido[]): string {
  if (!body.clienteId) return 'Selecciona un cliente.';
  if (!body.sucursalId) return 'Selecciona una sucursal.';
  if (!lineas.length) return 'Agrega al menos un producto al pedido.';
  const ids = new Set<number>();
  for (const linea of lineas) {
    if (ids.has(linea.productoId)) return `El producto ${linea.codigo} está repetido.`;
    ids.add(linea.productoId);
    if (!(linea.cantidad > 0)) return `La cantidad de ${linea.codigo} debe ser mayor a 0.`;
    if (linea.precioUnitario < 0) return `El precio de ${linea.codigo} no puede ser negativo.`;
    if (linea.descuento < 0) return `El descuento de ${linea.codigo} no puede ser negativo.`;
    if (linea.descuento > round2(linea.cantidad * linea.precioUnitario)) {
      return `El descuento de ${linea.codigo} supera el subtotal de la línea.`;
    }
  }
  const descuento = body.descuento ?? 0;
  if (descuento < 0) return 'El descuento global no puede ser negativo.';
  if (descuento > subtotalPedido(lineas)) return 'El descuento global supera el subtotal del pedido.';
  return '';
}

/** Clase del badge; estados desconocidos se muestran neutros. */
export function claseEstado(estado: string): string {
  const clases: Record<string, string> = {
    PENDIENTE: 'pendiente', DESPACHO_PARCIAL: 'pendiente',
    APROBADO: 'confirmado', DESPACHADO: 'confirmado', COMPLETADO: 'confirmado',
    RECHAZADO: 'anulado', ANULADO: 'anulado',
  };
  return clases[estado] ?? 'otro';
}

export function mensajeError(err: HttpErrorResponse): string {
  if (err.status === 403) return 'No tienes autorización para gestionar pedidos de venta.';
  if (err.status === 0) return 'No se pudo conectar con el servidor. Intenta nuevamente.';
  if (err.status === 401) return 'La sesión ha expirado. Inicia sesión nuevamente.';
  if (err.status >= 500) return 'El servidor no pudo completar la operación.';
  return err.error?.message || err.error?.mensaje || 'No se pudo completar la operación.';
}
