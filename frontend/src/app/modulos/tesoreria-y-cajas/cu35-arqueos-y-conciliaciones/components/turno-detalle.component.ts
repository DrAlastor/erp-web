import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalFocoDirective } from '../../cu34-cuentas-por-pagar-cxp/components/modal-foco.directive';
import { MovimientosTablaComponent } from '../../cu32-cuentas-bancarias-y-cajas/components/movimientos-tabla.component';
import { claseEstadoTurno, fechaHoraCorta, mensajeErrorCaja } from '../../cu32-cuentas-bancarias-y-cajas/utils/caja.utils';
import { ArqueoCajaService } from '../services/arqueo-caja.service';
import { TurnoDetalleResponse } from '../models/arqueo-caja.model';
import { ArqueoDesgloseComponent } from './arqueo-desglose.component';

/** Modal de detalle de un turno (GET /api/caja/turnos/{id}): datos, arqueo y movimientos. Solo lectura. */
@Component({
  selector: 'app-turno-detalle',
  standalone: true,
  imports: [DecimalPipe, ModalFocoDirective, MovimientosTablaComponent, ArqueoDesgloseComponent],
  templateUrl: './turno-detalle.component.html',
  styleUrl: './turno-detalle.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TurnoDetalleComponent {
  private readonly api = inject(ArqueoCajaService);
  private readonly destroy = inject(DestroyRef);

  readonly turnoId = input.required<number>();
  readonly closed = output<void>();

  readonly detalle = signal<TurnoDetalleResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly fecha = fechaHoraCorta;
  readonly claseEstado = claseEstadoTurno;

  constructor() {
    effect(() => {
      const id = this.turnoId();
      untracked(() => this.cargar(id));
    });
  }

  cargar(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.api.detalle(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(mensajeErrorCaja(err));
      },
    });
  }
}
