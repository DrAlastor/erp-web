import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import { CuentaPorPagarResponse, FormaPagoOpcionResponse, RegistrarPagoRequest } from '../models/cuenta-por-pagar.model';
import { hoyIso, mensajeError, round2, validarPago } from '../utils/cxp.utils';
import { ModalFocoDirective } from './modal-foco.directive';

/** Modal "Registrar pago" de una cuota (CONTABILIDAD_CREAR). Valida monto ≤ saldo antes de enviar. */
@Component({
  selector: 'app-cxp-pago-form',
  standalone: true,
  imports: [FormsModule, DecimalPipe, ModalFocoDirective],
  templateUrl: './cxp-pago-form.component.html',
  styleUrl: './cxp-pago-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CxpPagoFormComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly destroy = inject(DestroyRef);

  readonly cuenta = input.required<CuentaPorPagarResponse>();
  readonly pagado = output<CuentaPorPagarResponse>();
  readonly closed = output<void>();

  readonly formas = signal<FormaPagoOpcionResponse[]>([]);
  readonly formasCargando = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);
  readonly hoy = hoyIso();

  readonly formaPagoId = signal<number | null>(null);
  readonly monto = signal<number | null>(null);
  fechaPago = this.hoy;
  numeroRecibo = '';
  transaccionExterna = '';
  observaciones = '';

  readonly validacion = computed(() =>
    validarPago({ formaPagoId: this.formaPagoId() ?? undefined, monto: this.monto() ?? 0 }, this.cuenta().saldo, this.hoy),
  );
  readonly saldoRestante = computed(() => round2(this.cuenta().saldo - (this.monto() ?? 0)));

  constructor() {
    this.api
      .formasPago()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          this.formas.set(lista);
          this.formasCargando.set(false);
          if (lista.length === 1) this.formaPagoId.set(lista[0].id);
        },
        error: (err: HttpErrorResponse) => {
          this.formasCargando.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }

  pagarSaldo(): void {
    this.monto.set(this.cuenta().saldo);
  }

  onMonto(value: unknown): void {
    const parsed = Number(value);
    this.monto.set(value === '' || value === null || !Number.isFinite(parsed) ? null : parsed);
  }

  cerrar(): void {
    if (!this.busy()) this.closed.emit();
  }

  guardar(): void {
    if (this.busy()) return;
    this.intentado.set(true);
    const body: RegistrarPagoRequest = {
      formaPagoId: this.formaPagoId() ?? 0,
      monto: round2(this.monto() ?? 0),
      fechaPago: this.fechaPago || undefined,
      numeroRecibo: this.numeroRecibo.trim() || undefined,
      transaccionExterna: this.transaccionExterna.trim() || undefined,
      observaciones: this.observaciones.trim() || undefined,
    };
    const problema = validarPago(body, this.cuenta().saldo, this.hoy);
    // Los errores de monto/forma de pago ya se muestran reactivos (validacion); aquí solo los demás.
    this.error.set(problema === this.validacion() ? '' : problema);
    if (problema) return;
    this.busy.set(true);
    this.api
      .registrarPago(this.cuenta().id, body)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (actualizada) => {
          this.busy.set(false);
          this.pagado.emit(actualizada);
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }
}
