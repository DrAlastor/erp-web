import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { mensajeError, puedeContabilidad } from '../../contabilidad-permisos';
import { PeriodosContablesService } from '../services/periodos-contables.service';
import { AccionPeriodo, Gestion, PeriodoContable } from '../models/periodo-contable.model';
import { accionDisponible, fechaCorta, gestionesDisponibles, mesesFaltantes } from '../utils/periodos.utils';

interface Confirmacion {
  periodo: PeriodoContable;
  accion: Exclude<AccionPeriodo, null>;
}

/**
 * CU26 - Gestionar Periodos Contables.
 *
 * Genera los 12 periodos de una gestión y los cierra o reabre en orden. Un periodo cerrado no
 * admite asientos (las ventas de esas fechas se rechazan).
 */
@Component({
  selector: 'app-periodos-contables',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './periodos-contables.component.html',
  styleUrl: './periodos-contables.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PeriodosContablesComponent {
  private readonly api = inject(PeriodosContablesService);
  private readonly permisos = inject(PermisosService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly fecha = fechaCorta;
  private readonly anioActual = new Date().getFullYear();

  readonly existentes = signal<number[]>([]);
  readonly opciones = computed(() => gestionesDisponibles(this.existentes(), this.anioActual));
  readonly seleccionada = signal(this.anioActual);
  readonly datos = signal<Gestion | null>(null);
  readonly faltantes = computed(() => mesesFaltantes(this.datos()?.periodos ?? []));

  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly confirmando = signal<Confirmacion | null>(null);

  readonly puedeCrear = computed(() => this.permisos.cargado() && puedeContabilidad(this.permisos, 'CREAR'));
  readonly puedeModificar = computed(
    () => this.permisos.cargado() && puedeContabilidad(this.permisos, 'MODIFICAR'),
  );

  constructor() {
    this.permisos
      .asegurarCargados()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ error: () => undefined });
    this.cargarGestiones();
    this.load();
  }

  load(): void {
    const generation = ++this.generation;
    this.loading.set(true);
    this.api
      .gestion(this.seleccionada())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (g) => {
          if (generation !== this.generation) return;
          this.datos.set(g);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  seleccionar(gestion: number): void {
    this.limpiarMensajes();
    this.seleccionada.set(Number(gestion));
    this.load();
  }

  accion(periodo: PeriodoContable): AccionPeriodo {
    return accionDisponible(periodo, this.datos()?.periodos ?? []);
  }

  generar(): void {
    const gestion = this.seleccionada();
    this.ejecutar(this.api.generar(gestion), (g) => {
      this.datos.set(g);
      this.cargarGestiones();
      this.success.set(
        g.creados
          ? `Se generaron ${g.creados} periodos de la gestión ${gestion}.`
          : `La gestión ${gestion} ya tenía sus 12 periodos.`,
      );
    });
  }

  pedir(periodo: PeriodoContable, accion: Exclude<AccionPeriodo, null>): void {
    this.limpiarMensajes();
    this.confirmando.set({ periodo, accion });
  }

  confirmar(): void {
    const c = this.confirmando();
    if (!c) return;
    const peticion = c.accion === 'cerrar' ? this.api.cerrar(c.periodo.id) : this.api.reabrir(c.periodo.id);
    this.ejecutar(peticion, (p) => {
      this.confirmando.set(null);
      this.load();
      this.success.set(`Periodo ${p.nombre} ${c.accion === 'cerrar' ? 'cerrado' : 'reabierto'}.`);
    });
  }

  private cargarGestiones(): void {
    this.api
      .gestiones()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (g) => this.existentes.set(g), error: () => undefined });
  }

  private ejecutar<T>(peticion: Observable<T>, alTerminar: (r: T) => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.limpiarMensajes();
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.busy.set(false);
        alTerminar(r);
      },
      error: (err) => {
        this.busy.set(false);
        this.confirmando.set(null);
        this.fail(err);
      },
    });
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.success.set('');
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && !this.datos());
    this.error.set(mensajeError(err.status, err.error?.message, 'los periodos contables'));
  }
}
