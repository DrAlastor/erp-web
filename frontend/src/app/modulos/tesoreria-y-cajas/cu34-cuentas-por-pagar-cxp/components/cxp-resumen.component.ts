import {
  ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, output, signal, untracked,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import { ResumenCxpResponse, TopProveedorCxp } from '../models/cuenta-por-pagar.model';
import { mensajeError, segmentosAntiguedad } from '../utils/cxp.utils';

/** Tarjetas de resumen de CxP, barra de antigüedad de la deuda vencida y top de proveedores. */
@Component({
  selector: 'app-cxp-resumen',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './cxp-resumen.component.html',
  styleUrl: './cxp-resumen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CxpResumenComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly destroy = inject(DestroyRef);

  /** Cambia cuando el padre registra un pago o genera cuotas: dispara la recarga (GET /resumen). */
  readonly refresco = input(0);
  readonly proveedorSeleccionado = output<TopProveedorCxp>();

  readonly resumen = signal<ResumenCxpResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly segmentos = computed(() => segmentosAntiguedad(this.resumen()?.antiguedad ?? []));
  readonly maxProveedor = computed(() => Math.max(1, ...(this.resumen()?.topProveedores ?? []).map((p) => p.saldo)));
  readonly placeholders = [1, 2, 3, 4];

  constructor() {
    effect(() => {
      this.refresco();
      untracked(() => this.cargar());
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.resumen().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
