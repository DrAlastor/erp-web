import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import { CompraSinCuentaResponse, CuentaPorPagarResumenResponse } from '../models/cuenta-por-pagar.model';
import { mensajeError, previsualizarCuotas, primerVencimientoSugerido, validarGeneracion } from '../utils/cxp.utils';
import { CompraSelectorComponent } from './compra-selector.component';
import { ModalFocoDirective } from './modal-foco.directive';

/** Modal "Generar cuotas desde compra": elige la compra a crédito, define el plan y muestra la vista previa. */
@Component({
  selector: 'app-cxp-generar-cuotas',
  standalone: true,
  imports: [FormsModule, DecimalPipe, CompraSelectorComponent, ModalFocoDirective],
  templateUrl: './cxp-generar-cuotas.component.html',
  styleUrl: './cxp-generar-cuotas.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CxpGenerarCuotasComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly destroy = inject(DestroyRef);

  readonly generado = output<CuentaPorPagarResumenResponse[]>();
  readonly closed = output<void>();

  readonly compra = signal<CompraSinCuentaResponse | null>(null);
  readonly numeroCuotas = signal(3);
  readonly primerVencimiento = signal('');
  readonly diasEntreCuotas = signal(30);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);

  readonly validacion = computed(() =>
    validarGeneracion(this.compra()?.compraId ?? null, this.numeroCuotas(), this.diasEntreCuotas()),
  );
  readonly cuotas = computed(() => {
    const compra = this.compra();
    if (!compra || this.validacion()) return [];
    return previsualizarCuotas(compra.total, this.numeroCuotas(), this.primerVencimiento(), this.diasEntreCuotas());
  });

  onCompra(compra: CompraSinCuentaResponse | null): void {
    this.compra.set(compra);
    this.primerVencimiento.set(compra ? primerVencimientoSugerido(compra) : '');
  }

  entero(value: unknown): number {
    const n = Number(value);
    return Number.isFinite(n) ? Math.trunc(n) : 0;
  }

  cerrar(): void {
    if (!this.busy()) this.closed.emit();
  }

  confirmar(): void {
    if (this.busy()) return;
    this.intentado.set(true);
    const compra = this.compra();
    if (this.validacion() || !compra) return;
    this.busy.set(true);
    this.error.set('');
    this.api
      .generar({
        compraId: compra.compraId,
        numeroCuotas: this.numeroCuotas(),
        primerVencimiento: this.primerVencimiento() || undefined,
        diasEntreCuotas: this.diasEntreCuotas(),
      })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (cuotas) => {
          this.busy.set(false);
          this.generado.emit(cuotas);
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }
}
