import {
  ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal, untracked,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { PedidosService } from '../services/pedidos.service';
import { Pedido } from '../models/pedido.model';
import { claseEstado, mensajeError } from '../utils/pedido.utils';

type Accion = 'anular';

/** Detalle de un pedido con la acción Anular (motivo obligatorio). Aprobar es de la CU17. */
@Component({
  selector: 'app-pedido-detalle',
  standalone: true,
  imports: [FormsModule, DecimalPipe, SiPermisoDirective],
  templateUrl: './pedido-detalle.component.html',
  styleUrl: './pedido-detalle.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidoDetalleComponent {
  private readonly api = inject(PedidosService);
  private readonly destroy = inject(DestroyRef);

  readonly pedidoId = input.required<number>();
  readonly changed = output<string>();
  readonly editar = output<number>();
  readonly closed = output<void>();

  readonly pedido = signal<Pedido | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly accion = signal<Accion | null>(null);
  readonly claseEstado = claseEstado;
  motivo = '';

  constructor() {
    effect(() => {
      const id = this.pedidoId();
      untracked(() => this.cargar(id));
    });
  }

  cargar(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .detail(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (pedido) => {
          this.pedido.set(pedido);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }

  iniciar(accion: Accion): void {
    this.error.set('');
    this.motivo = '';
    this.accion.set(accion);
  }

  ejecutar(): void {
    const pedido = this.pedido();
    const accion = this.accion();
    if (!pedido || !accion || this.busy()) return;
    const motivo = this.motivo.trim();
    if (accion === 'anular' && !motivo) {
      this.error.set('Indica el motivo de la anulación.');
      return;
    }
    const request: Observable<Pedido> = this.api.anular(pedido.id, motivo);
    this.busy.set(true);
    this.error.set('');
    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (actualizado) => {
        this.busy.set(false);
        this.accion.set(null);
        this.pedido.set(actualizado);
        this.changed.emit(`Pedido ${actualizado.numeroPedido} anulado correctamente.`);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
