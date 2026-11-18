import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { CajaService } from '../services/caja.service';
import { AbrirTurnoRequest, SucursalOpcion, TurnoResponse } from '../models/caja.model';
import { aNumero, mensajeErrorCaja, round2, validarApertura } from '../utils/caja.utils';

/** Apertura de caja (POST /api/caja/turnos). Se muestra cuando el usuario no tiene turno ABIERTO (204). */
@Component({
  selector: 'app-apertura-caja-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './apertura-caja-form.component.html',
  styleUrl: './apertura-caja-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AperturaCajaFormComponent {
  private readonly api = inject(CajaService);
  private readonly destroy = inject(DestroyRef);
  private readonly permisos = inject(PermisosService);

  readonly sucursales = input.required<SucursalOpcion[]>();
  readonly abierto = output<TurnoResponse>();

  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);
  /** Igual que `*siPermiso`, pero con rama "sin permiso" visible (el backend vuelve a verificar). */
  readonly puedeAbrir = computed(() => this.permisos.permisos().has('FACTURACION_CREAR'));
  readonly permisosCargados = this.permisos.cargado;

  sucursalId: number | null = null;
  montoApertura: number | null = null;
  observaciones = '';

  onMonto(value: unknown): void {
    this.montoApertura = aNumero(value);
  }

  validacion(): string {
    return validarApertura({ sucursalId: this.sucursalId ?? undefined, montoApertura: this.montoApertura ?? undefined });
  }

  abrir(): void {
    if (this.busy()) return;
    this.intentado.set(true);
    this.error.set('');
    if (this.validacion()) return;
    const body: AbrirTurnoRequest = {
      sucursalId: this.sucursalId ?? 0,
      montoApertura: round2(this.montoApertura ?? 0),
      observaciones: this.observaciones.trim() || undefined,
    };
    this.busy.set(true);
    this.api.abrirTurno(body).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (turno) => {
        this.busy.set(false);
        this.abierto.emit(turno);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(mensajeErrorCaja(err));
      },
    });
  }
}
