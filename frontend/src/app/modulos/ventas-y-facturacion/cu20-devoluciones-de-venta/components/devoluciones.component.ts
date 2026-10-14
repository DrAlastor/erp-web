import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NotaCreditoService } from '../services/nota-credito.service';
import {
  MOTIVOS_NOTA_CREDITO,
  MotivoNotaCredito,
  NotaCredito,
  NotaCreditoResumen,
  VentaDevolucion,
} from '../models/nota-credito.model';

@Component({
  selector: 'app-devoluciones',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './devoluciones.component.html',
  styleUrl: './devoluciones.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DevolucionesComponent {
  private readonly api = inject(NotaCreditoService);
  private readonly destroy = inject(DestroyRef);

  readonly motivos = MOTIVOS_NOTA_CREDITO;
  readonly tab = signal<'nueva' | 'historial'>('nueva');
  readonly ventas = signal<VentaDevolucion[] | null>(null);
  readonly venta = signal<VentaDevolucion | null>(null);
  readonly notas = signal<NotaCreditoResumen[]>([]);
  readonly nota = signal<NotaCredito | null>(null);
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  // Búsqueda de la factura
  numeroFactura: number | null = null;
  nitCliente = '';

  // Devolución: cantidad a devolver por línea de la factura
  cantidades: Record<number, number> = {};
  motivo: MotivoNotaCredito = 'DEFECTUOSO';
  observaciones = '';

  // Historial
  search = '';
  page = 0;

  cambiarTab(tab: 'nueva' | 'historial'): void {
    this.tab.set(tab);
    this.error.set('');
    this.success.set('');
    if (tab === 'historial') this.cargarNotas(true);
  }

  buscarVentas(): void {
    if (!this.numeroFactura && !this.nitCliente.trim()) {
      this.error.set('Indica el número del comprobante o el NIT/CI del cliente.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.venta.set(null);
    this.api
      .buscarVentas(this.numeroFactura, this.nitCliente.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (ventas) => {
          this.loading.set(false);
          this.forbidden.set(false);
          this.ventas.set(ventas);
          if (ventas.length === 1) this.elegir(ventas[0]);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  elegir(venta: VentaDevolucion): void {
    this.venta.set(venta);
    this.cantidades = {};
    for (const linea of venta.lineas) this.cantidades[linea.ventaDetalleId] = 0;
    this.motivo = 'DEFECTUOSO';
    this.observaciones = '';
  }

  devolverTodo(): void {
    const venta = this.venta();
    if (!venta) return;
    for (const linea of venta.lineas) this.cantidades[linea.ventaDetalleId] = linea.cantidadDisponible;
  }

  sinDisponible(venta: VentaDevolucion): boolean {
    return venta.lineas.every((l) => l.cantidadDisponible <= 0);
  }

  importeLinea(ventaDetalleId: number, precio: number): number {
    return this.redondear((Number(this.cantidades[ventaDetalleId]) || 0) * precio);
  }

  totalDevolucion(): number {
    const venta = this.venta();
    if (!venta) return 0;
    return this.redondear(
      venta.lineas.reduce((acc, l) => acc + this.importeLinea(l.ventaDetalleId, l.precioUnitarioNeto), 0),
    );
  }

  puedeEmitir(): boolean {
    const venta = this.venta();
    if (!venta || this.busy()) return false;
    let alguna = false;
    for (const linea of venta.lineas) {
      const cantidad = Number(this.cantidades[linea.ventaDetalleId]) || 0;
      if (cantidad < 0 || cantidad > linea.cantidadDisponible) return false;
      if (cantidad > 0) alguna = true;
    }
    return alguna;
  }

  emitir(): void {
    const venta = this.venta();
    if (!venta || !this.puedeEmitir()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .emitir({
        ventaId: venta.ventaId,
        motivo: this.motivo,
        observaciones: this.observaciones.trim() || null,
        items: venta.lineas
          .filter((l) => Number(this.cantidades[l.ventaDetalleId]) > 0)
          .map((l) => ({ ventaDetalleId: l.ventaDetalleId, cantidad: Number(this.cantidades[l.ventaDetalleId]) })),
      })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (nota) => {
          this.busy.set(false);
          this.nota.set(nota);
          this.success.set(`Nota de crédito ${nota.numeroNota} emitida por Bs ${nota.total.toFixed(2)}.`);
          this.venta.set(null);
          this.ventas.set(null);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  cargarNotas(reset = false): void {
    if (reset) this.page = 0;
    this.loading.set(true);
    this.error.set('');
    this.api
      .listar(this.search.trim(), this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          this.loading.set(false);
          this.forbidden.set(false);
          this.notas.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  navegar(delta: number): void {
    this.page += delta;
    this.cargarNotas();
  }

  ver(nota: NotaCreditoResumen): void {
    this.busy.set(true);
    this.api
      .detalle(nota.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (detalle) => {
          this.busy.set(false);
          this.nota.set(detalle);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  /** "Factura N° 1003" o "Nota de venta N° 12". */
  nombreComprobante(v: { tipoComprobante: string; numeroFactura: number }): string {
    return `${v.tipoComprobante === 'NOTA_VENTA' ? 'Nota de venta' : 'Factura'} N° ${v.numeroFactura}`;
  }

  etiquetaMotivo(motivo: string): string {
    return this.motivos.find((m) => m.valor === motivo)?.etiqueta ?? motivo;
  }

  private redondear(valor: number): number {
    return Math.round((valor + Number.EPSILON) * 100) / 100;
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403);
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para gestionar devoluciones.'
        : err.status === 0
          ? 'No se pudo conectar con el servidor. Intenta nuevamente.'
          : err.status === 401
            ? 'La sesión ha expirado. Inicia sesión nuevamente.'
            : err.status >= 500
              ? 'El servidor no pudo completar la operación.'
              : err.error?.message || 'No se pudo completar la operación.',
    );
  }
}
