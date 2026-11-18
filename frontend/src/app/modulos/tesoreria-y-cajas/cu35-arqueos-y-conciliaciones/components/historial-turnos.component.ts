import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CajaService } from '../../cu32-cuentas-bancarias-y-cajas/services/caja.service';
import { SucursalOpcion, TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { mensajeErrorCaja } from '../../cu32-cuentas-bancarias-y-cajas/utils/caja.utils';
import { ArqueoCajaService } from '../services/arqueo-caja.service';
import { FILTROS_HISTORIAL_VACIOS, FiltrosHistorialCaja } from '../models/arqueo-caja.model';
import { validarRango } from '../utils/arqueo.utils';
import { HistorialTablaComponent } from './historial-tabla.component';
import { TurnoDetalleComponent } from './turno-detalle.component';

/** Pestaña "Historial": turnos filtrables por sucursal, estado y fechas de apertura (admin ve todos; el resto, los suyos). */
@Component({
  selector: 'app-historial-turnos',
  standalone: true,
  imports: [FormsModule, HistorialTablaComponent, TurnoDetalleComponent],
  templateUrl: './historial-turnos.component.html',
  styleUrl: './historial-turnos.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialTurnosComponent implements OnInit {
  private readonly api = inject(ArqueoCajaService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  /** Turno a abrir en detalle al montar (p. ej. el recién cerrado). */
  readonly detalleInicial = input<number | null>(null);

  readonly sucursales = signal<SucursalOpcion[]>([]);
  readonly turnos = signal<TurnoResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly detalle = signal<number | null>(null);

  filtros: FiltrosHistorialCaja = { ...FILTROS_HISTORIAL_VACIOS };
  page = 0;

  constructor() {
    inject(CajaService).opciones().pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (o) => this.sucursales.set(o.sucursales), error: () => {} });
    this.load();
  }

  ngOnInit(): void {
    this.detalle.set(this.detalleInicial());
  }

  filtrado(): boolean {
    const f = this.filtros;
    return !!(f.sucursalId || f.estado || f.desde || f.hasta);
  }

  load(reset = false): void {
    const rango = validarRango(this.filtros.desde, this.filtros.hasta);
    if (rango) {
      this.error.set(rango);
      return;
    }
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api.historial(this.filtros, this.page).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        if (generation !== this.generation) return;
        this.turnos.set(r.content);
        this.total.set(r.totalElements);
        this.pages.set(r.totalPages);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;
        this.loading.set(false);
        this.error.set(mensajeErrorCaja(err));
      },
    });
  }

  limpiarFiltros(): void {
    this.filtros = { ...FILTROS_HISTORIAL_VACIOS };
    this.load(true);
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }
}
