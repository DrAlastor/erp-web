import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CajaService } from '../services/caja.service';
import { CajaTurno, CierreCaja, EstadoCaja } from '../models/caja.model';

@Component({
  selector: 'app-apertura-caja',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './apertura-caja.component.html',
  styleUrl: './apertura-caja.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AperturaCajaComponent {
  private readonly api = inject(CajaService);
  private readonly destroy = inject(DestroyRef);

  readonly estado = signal<EstadoCaja | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly cierre = signal<CierreCaja | null>(null);

  sucursalId: number | null = null;
  montoApertura: number | null = null;
  observacionesApertura = '';
  montoCierre: number | null = null;
  observacionesCierre = '';

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.estado(this.sucursalId ?? undefined)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (estado) => {
          this.estado.set(estado);
          if (!this.sucursalId && estado.sucursales.length) {
            this.sucursalId = estado.turno?.sucursalId ?? estado.sucursales[0].id;
          }
          this.loading.set(false);
        },
        error: (err) => this.fail(err),
      });
  }

  abrir(): void {
    if (!this.sucursalId || this.montoApertura === null || this.montoApertura < 0) {
      this.error.set('Selecciona la sucursal e indica un monto de apertura válido.');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.api.abrir(this.sucursalId, this.montoApertura, this.observacionesApertura)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (turno: CajaTurno) => {
          this.busy.set(false);
          this.success.set(`Turno #${turno.id} abierto correctamente.`);
          this.montoApertura = null;
          this.observacionesApertura = '';
          this.cargar();
        },
        error: (err) => this.fail(err),
      });
  }

  cerrar(): void {
    const turno = this.estado()?.turno;
    if (!turno) {
      this.error.set('No existe un turno abierto para cerrar.');
      return;
    }
    if (this.montoCierre === null || this.montoCierre < 0) {
      this.error.set('Indica el monto físico contado al cerrar caja.');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.api.cerrar(turno.id, this.montoCierre, this.observacionesCierre)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (resultado) => {
          this.busy.set(false);
          this.cierre.set(resultado);
          this.success.set(
            resultado.diferencia === 0
              ? 'Turno cerrado sin diferencias.'
              : `Turno cerrado con diferencia de Bs ${resultado.diferencia.toFixed(2)}.`
          );
          this.montoCierre = null;
          this.observacionesCierre = '';
          this.cargar();
        },
        error: (err) => this.fail(err),
      });
  }

  private fail(err: HttpErrorResponse): void {
    this.loading.set(false);
    this.busy.set(false);
    this.error.set(err.error?.message || err.error?.error || 'No se pudo procesar la operación de caja.');
  }
}
