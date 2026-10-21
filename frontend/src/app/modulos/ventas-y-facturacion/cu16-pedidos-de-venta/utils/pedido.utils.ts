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
  if (!body.clienteId) return 'Debes seleccionar un cliente para el pedido.';
  if (!body.sucursalId) return 'Debes seleccionar una sucursal de despacho.';
  if (!lineas.length) return 'Debes agregar al menos un producto a la lista del pedido.';
  const ids = new Set<number>();
  for (const linea of lineas) {
    if (ids.has(linea.productoId)) return `El producto "${linea.nombre || linea.codigo}" está duplicado en la lista.`;
    ids.add(linea.productoId);
    if (!(linea.cantidad > 0)) return `La cantidad de "${linea.nombre || linea.codigo}" debe ser mayor a 0.`;
    if (linea.precioUnitario < 0) return `El precio de "${linea.nombre || linea.codigo}" no puede ser negativo.`;
    if (linea.descuento < 0) return `El descuento de "${linea.nombre || linea.codigo}" no puede ser negativo.`;
    if (linea.descuento > round2(linea.cantidad * linea.precioUnitario)) {
      return `El descuento de "${linea.nombre || linea.codigo}" supera el subtotal de la línea.`;
    }
  }
  const descuento = body.descuento ?? 0;
  if (descuento < 0) return 'El descuento global no puede ser negativo.';
  if (descuento > subtotalPedido(lineas)) return 'El descuento global no puede superar el subtotal del pedido.';
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
  if (err.status === 403) return 'No tienes los permisos requeridos para esta operación comercial.';
  if (err.status === 0) return 'No se pudo conectar con el servidor backend. Verifica tu conexión o reinicia el backend.';
  if (err.status === 401) return 'Tu sesión ha expirado o no es válida. Inicia sesión nuevamente.';
  if (err.status === 404) return 'El recurso o servicio no fue encontrado (404). Verifica que el backend esté actualizado y reiniciado.';
  if (err.error?.message) return err.error.message;
  if (err.error?.mensaje) return err.error.mensaje;
  if (err.error?.detail) return err.error.detail;
  if (typeof err.error === 'string' && err.error.trim()) return err.error;
  if (err.status === 400) return 'Los datos del pedido contienen errores o faltan campos obligatorios.';
  if (err.status >= 500) return 'Error interno del servidor (' + err.status + '). Intenta de nuevo.';
  return 'Ocurrió un error al procesar la operación.';
}
