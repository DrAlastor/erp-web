import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { PedidosService } from '../services/pedidos.service';
import { ESTADOS_PEDIDO, Pedido, PedidoResumen } from '../models/pedido.model';
import { claseEstado, mensajeError } from '../utils/pedido.utils';
import { PedidoFormComponent } from './pedido-form.component';
import { PedidoDetalleComponent } from './pedido-detalle.component';

/** CU16 — listado de pedidos de venta con filtros, paginación y acceso a alta/edición/detalle. */
@Component({
  selector: 'app-pedidos-venta',
  standalone: true,
  imports: [FormsModule, DecimalPipe, SiPermisoDirective, PedidoFormComponent, PedidoDetalleComponent],
  templateUrl: './pedidos-venta.component.html',
  styleUrl: './pedidos-venta.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidosVentaComponent {
  private readonly api = inject(PedidosService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly estados = ESTADOS_PEDIDO;
  readonly claseEstado = claseEstado;
  readonly pedidos = signal<PedidoResumen[]>([]);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);
  /** undefined = formulario cerrado, null = nuevo pedido, Pedido = edición. */
  readonly editor = signal<Pedido | null | undefined>(undefined);
  readonly detalleId = signal<number | null>(null);

  search = '';
  estado = '';
  page = 0;

  constructor() {
    this.load();
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.estado, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          if (generation !== this.generation) return;
          this.pedidos.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  openNew(): void {
    this.success.set('');
    this.editor.set(null);
  }

  edit(id: number): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.success.set('');
    this.api
      .detail(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (pedido) => {
          this.busy.set(false);
          this.detalleId.set(null);
          this.editor.set(pedido);
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  onSaved(pedido: Pedido, created: boolean): void {
    this.editor.set(undefined);
    this.success.set(`Pedido ${pedido.numeroPedido} ${created ? 'registrado' : 'actualizado'} correctamente.`);
    this.load();
  }

  onChanged(message: string): void {
    this.success.set(message);
    this.load();
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403);
    this.error.set(mensajeError(err));
  }
}
