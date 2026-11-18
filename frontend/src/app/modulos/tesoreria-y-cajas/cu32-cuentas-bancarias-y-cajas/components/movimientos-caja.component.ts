import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { CajaService } from '../services/caja.service';
import {
  ArqueoResponse, CajaOpcionesResponse, MovimientoResponse, RUTA_CIERRE_CAJA, TurnoResponse,
} from '../models/caja.model';
import { mensajeErrorCaja } from '../utils/caja.utils';
import { AperturaCajaFormComponent } from './apertura-caja-form.component';
import { CajaResumenComponent } from './caja-resumen.component';
import { MovimientoFormComponent } from './movimiento-form.component';
import { MovimientosTablaComponent } from './movimientos-tabla.component';

/**
 * HU-34 / CU34 Registrar Movimiento de Caja (+ CU32 apertura mínima). Sin turno ABIERTO muestra la apertura;
 * con turno: tarjetas, formulario de movimiento y tabla. Permisos: FACTURACION_CONSULTAR / FACTURACION_CREAR.
 */
@Component({
  selector: 'app-movimientos-caja',
  standalone: true,
  imports: [RouterLink, SiPermisoDirective, AperturaCajaFormComponent, CajaResumenComponent, MovimientoFormComponent, MovimientosTablaComponent],
  templateUrl: './movimientos-caja.component.html',
  styleUrl: './movimientos-caja.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientosCajaComponent {
  private readonly api = inject(CajaService);
  private readonly destroy = inject(DestroyRef);

  /** Función del menú desde la que se abrió: solo cambia los textos de cabecera. */
  readonly vista = input<'apertura' | 'movimientos'>('movimientos');

  readonly rutaCierre = RUTA_CIERRE_CAJA;
  readonly opciones = signal<CajaOpcionesResponse>({ sucursales: [], formasPago: [] });
  readonly turno = signal<TurnoResponse | null>(null);
  readonly arqueo = signal<ArqueoResponse | null>(null);
  readonly movimientos = signal<MovimientoResponse[]>([]);
  readonly loading = signal(true);
  readonly movLoading = signal(false);
  readonly arqueoFallo = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({ opciones: this.api.opciones(), turno: this.api.turnoActual() })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: ({ opciones, turno }) => {
          this.opciones.set(opciones);
          this.loading.set(false);
          this.usarTurno(turno);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(mensajeErrorCaja(err));
        },
      });
  }

  onAbierto(turno: TurnoResponse): void {
    this.success.set(`Caja abierta en ${turno.sucursalNombre} con un fondo de Bs ${turno.montoApertura.toFixed(2)}.`);
    this.usarTurno(turno);
  }

  onRegistrado(mov: MovimientoResponse): void {
    this.movimientos.update((l) => [mov, ...l]);
    this.success.set(`${mov.tipo === 'INGRESO' ? 'Ingreso' : 'Egreso'} de Bs ${mov.monto.toFixed(2)} registrado (${mov.numeroComprobante}).`);
    this.cargarArqueo(mov.turnoCajaId);
  }

  private usarTurno(turno: TurnoResponse | null): void {
    this.turno.set(turno);
    this.arqueo.set(null);
    this.movimientos.set([]);
    if (!turno) return;
    this.movLoading.set(true);
    this.api.movimientos(turno.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (lista) => {
        this.movimientos.set(lista);
        this.movLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.movLoading.set(false);
        this.error.set(mensajeErrorCaja(err));
      },
    });
    this.cargarArqueo(turno.id);
  }

  private cargarArqueo(turnoId: number): void {
    this.arqueoFallo.set(false);
    this.api.arqueo(turnoId).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (a) => this.arqueo.set(a),
      error: (err: HttpErrorResponse) => {
        this.arqueoFallo.set(true);
        this.error.set(mensajeErrorCaja(err));
      },
    });
  }
}
