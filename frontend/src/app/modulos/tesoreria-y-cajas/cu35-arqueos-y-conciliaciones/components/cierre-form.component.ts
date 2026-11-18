import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { aNumero, mensajeErrorCaja, round2 } from '../../cu32-cuentas-bancarias-y-cajas/utils/caja.utils';
import { ArqueoCajaService } from '../services/arqueo-caja.service';
import { calcularDiferencia, claseResultado, resultadoArqueo, validarCierre } from '../utils/arqueo.utils';

/** Monto contado + diferencia en vivo + observaciones (obligatorias si hay diferencia) y confirmación del cierre. */
@Component({
  selector: 'app-cierre-form',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './cierre-form.component.html',
  styleUrl: './cierre-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CierreFormComponent {
  private readonly api = inject(ArqueoCajaService);
  private readonly destroy = inject(DestroyRef);

  readonly turnoId = input.required<number>();
  readonly efectivoEsperado = input.required<number>();
  readonly cerrado = output<TurnoResponse>();
  /** El backend rechazó el cierre (400/409): el arqueo pudo cambiar y conviene recargarlo. */
  readonly fallo = output<void>();

  readonly montoContado = signal<number | null>(null);
  readonly observaciones = signal('');
  readonly confirmando = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);

  readonly diferencia = computed(() => calcularDiferencia(this.montoContado(), this.efectivoEsperado()));
  readonly resultado = computed(() => resultadoArqueo(this.diferencia()));
  readonly validacion = computed(() => validarCierre(this.montoContado(), this.efectivoEsperado(), this.observaciones()));
  readonly obsRequeridas = computed(() => (this.diferencia() ?? 0) !== 0);
  readonly claseResultado = claseResultado;

  onMonto(value: unknown): void {
    this.montoContado.set(aNumero(value));
    this.confirmando.set(false);
  }

  solicitarCierre(): void {
    this.intentado.set(true);
    this.error.set('');
    if (!this.validacion()) this.confirmando.set(true);
  }

  confirmar(): void {
    if (this.busy() || this.validacion()) return;
    this.busy.set(true);
    const body = { montoContado: round2(this.montoContado() ?? 0), observaciones: this.observaciones().trim() || undefined };
    this.api.cerrar(this.turnoId(), body).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (turno) => {
        this.busy.set(false);
        this.cerrado.emit(turno);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.confirmando.set(false);
        this.error.set(mensajeErrorCaja(err));
        if (err.status === 400 || err.status === 409) this.fallo.emit();
      },
    });
  }
}
