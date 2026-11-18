import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CajaService } from '../services/caja.service';
import { FormaPagoCajaOpcion, MovimientoResponse, RegistrarMovimientoRequest, TipoMovimiento } from '../models/caja.model';
import { aNumero, mensajeErrorCaja, round2, validarMovimiento } from '../utils/caja.utils';

/**
 * HU-34 — formulario "Registrar movimiento" (POST /turnos/{id}/movimientos). Los movimientos no se editan:
 * una corrección se registra como movimiento contrario. El tope de efectivo lo valida el backend (409).
 */
@Component({
  selector: 'app-movimiento-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './movimiento-form.component.html',
  styleUrl: './movimiento-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientoFormComponent {
  private readonly api = inject(CajaService);
  private readonly destroy = inject(DestroyRef);

  readonly turnoId = input.required<number>();
  readonly formasPago = input.required<FormaPagoCajaOpcion[]>();
  readonly registrado = output<MovimientoResponse>();

  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);

  tipo: TipoMovimiento = 'INGRESO';
  formaPagoId: number | null = null;
  concepto = '';
  monto: number | null = null;
  numeroComprobante = '';

  onMonto(value: unknown): void {
    this.monto = aNumero(value);
  }

  body(): RegistrarMovimientoRequest {
    return {
      tipo: this.tipo,
      formaPagoId: this.formaPagoId ?? 0,
      concepto: this.concepto.trim(),
      monto: this.monto ?? 0,
      numeroComprobante: this.numeroComprobante.trim() || undefined,
    };
  }

  validacion(): string {
    return validarMovimiento(this.body());
  }

  registrar(): void {
    if (this.busy()) return;
    this.intentado.set(true);
    this.error.set('');
    if (this.validacion()) return;
    const body = { ...this.body(), monto: round2(this.monto ?? 0) };
    this.busy.set(true);
    this.api.registrarMovimiento(this.turnoId(), body).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (mov) => {
        this.busy.set(false);
        this.intentado.set(false);
        this.concepto = '';
        this.monto = null;
        this.numeroComprobante = '';
        this.registrado.emit(mov);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(mensajeErrorCaja(err));
      },
    });
  }
}
