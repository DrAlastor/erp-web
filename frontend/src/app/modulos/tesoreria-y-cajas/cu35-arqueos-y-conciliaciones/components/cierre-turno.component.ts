import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { CajaService } from '../../cu32-cuentas-bancarias-y-cajas/services/caja.service';
import { ArqueoResponse, RUTA_APERTURA_CAJA, TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { fechaHoraCorta, mensajeErrorCaja } from '../../cu32-cuentas-bancarias-y-cajas/utils/caja.utils';
import { ArqueoDesgloseComponent } from './arqueo-desglose.component';
import { CierreFormComponent } from './cierre-form.component';

/** Pestaña "Cerrar mi turno": vista previa del arqueo del turno ABIERTO del usuario y formulario de cierre. */
@Component({
  selector: 'app-cierre-turno',
  standalone: true,
  imports: [RouterLink, SiPermisoDirective, ArqueoDesgloseComponent, CierreFormComponent],
  templateUrl: './cierre-turno.component.html',
  styleUrl: './cierre-turno.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CierreTurnoComponent {
  private readonly caja = inject(CajaService);
  private readonly destroy = inject(DestroyRef);
  private readonly permisos = inject(PermisosService);

  readonly cerrado = output<TurnoResponse>();

  readonly rutaApertura = RUTA_APERTURA_CAJA;
  readonly turno = signal<TurnoResponse | null>(null);
  readonly arqueo = signal<ArqueoResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly fecha = fechaHoraCorta;
  readonly puedeCerrar = computed(() => this.permisos.permisos().has('FACTURACION_CREAR'));

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.caja
      .turnoActual()
      .pipe(
        switchMap((turno) => {
          this.turno.set(turno);
          return turno ? this.caja.arqueo(turno.id) : of(null);
        }),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe({
        next: (arqueo) => {
          this.arqueo.set(arqueo);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(mensajeErrorCaja(err));
        },
      });
  }

  /** Recarga solo el arqueo (sin desmontar el formulario) para que el esperado y la diferencia queden al día. */
  recargarArqueo(turnoId: number): void {
    this.caja.arqueo(turnoId).pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (a) => this.arqueo.set(a) });
  }

  onCerrado(turno: TurnoResponse): void {
    this.turno.set(null);
    this.arqueo.set(null);
    this.cerrado.emit(turno);
  }
}
