import {
  ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal, untracked,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import { CuentaPorPagarResponse } from '../models/cuenta-por-pagar.model';
import { claseEstadoCxp, estadoVisible, mensajeError } from '../utils/cxp.utils';
import { CxpPagoFormComponent } from './cxp-pago-form.component';
import { ModalFocoDirective } from './modal-foco.directive';

/** Detalle de una cuota por pagar con su historial de pagos y acceso a "Registrar pago". */
@Component({
  selector: 'app-cxp-detalle',
  standalone: true,
  imports: [DecimalPipe, SiPermisoDirective, CxpPagoFormComponent, ModalFocoDirective],
  templateUrl: './cxp-detalle.component.html',
  styleUrl: './cxp-detalle.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CxpDetalleComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly destroy = inject(DestroyRef);

  readonly cuentaId = input.required<number>();
  /** Abre directamente el modal de pago al terminar de cargar (botón "Pagar" de la tabla). */
  readonly iniciarPago = input(false);
  readonly changed = output<string>();
  readonly closed = output<void>();

  readonly cuenta = signal<CuentaPorPagarResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly pagando = signal(false);
  readonly estadoVisible = estadoVisible;
  readonly claseEstado = claseEstadoCxp;

  constructor() {
    effect(() => {
      const id = this.cuentaId();
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
        next: (cuenta) => {
          this.cuenta.set(cuenta);
          this.loading.set(false);
          if (this.iniciarPago() && cuenta.saldo > 0) this.pagando.set(true);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }

  onPagado(actualizada: CuentaPorPagarResponse): void {
    this.pagando.set(false);
    this.cuenta.set(actualizada);
    const sufijo = actualizada.estado === 'PAGADA' ? 'La cuota quedó PAGADA.' : `Saldo restante Bs. ${actualizada.saldo.toFixed(2)}.`;
    this.changed.emit(`Pago registrado para ${actualizada.proveedorRazonSocial} (factura ${actualizada.numeroFactura}). ${sufijo}`);
  }

  cerrar(): void {
    if (!this.pagando()) this.closed.emit();
  }
}
