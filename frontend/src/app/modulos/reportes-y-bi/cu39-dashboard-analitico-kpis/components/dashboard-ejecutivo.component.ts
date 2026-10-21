import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DashboardEjecutivoService } from '../services/dashboard-ejecutivo.service';
import { DashboardEjecutivoResponse, SucursalOpcion } from '../models/dashboard-ejecutivo.model';
import { mensajeError } from '../utils/dashboard.utils';
import { KpiFilaComponent } from './kpi-fila.component';
import { AlertasPanelComponent } from './alertas-panel.component';
import { VentasChartComponent } from './ventas-chart.component';
import { TopProductosComponent } from './top-productos.component';
import { PedidosEstadoComponent } from './pedidos-estado.component';
import { StockCriticoComponent } from './stock-critico.component';

/**
 * HU-36 / CU39 — Dashboard Ejecutivo: chequeo rápido de la empresa en una pantalla (solo lectura).
 * Permiso: REPORTES_CONSULTAR. Cada bloque maneja su propio estado (carga / no disponible / vacío).
 */
@Component({
  selector: 'app-dashboard-ejecutivo',
  standalone: true,
  imports: [
    FormsModule, KpiFilaComponent, AlertasPanelComponent, VentasChartComponent,
    TopProductosComponent, PedidosEstadoComponent, StockCriticoComponent,
  ],
  templateUrl: './dashboard-ejecutivo.component.html',
  styleUrl: './dashboard-ejecutivo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEjecutivoComponent {
  private readonly api = inject(DashboardEjecutivoService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly data = signal<DashboardEjecutivoResponse | null>(null);
  readonly sucursales = signal<SucursalOpcion[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly forbidden = signal(false);
  readonly fechaHoy = new Date().toLocaleDateString('es-BO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  readonly actualizado = computed(() => {
    const iso = this.data()?.generadoEn;
    return iso ? new Date(iso).toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' }) : '';
  });
  /** Skeleton solo en la primera carga; al actualizar se mantienen los datos anteriores. */
  readonly cargandoBloques = computed(() => this.loading() && !this.data());

  sucursalId: number | null = null;

  constructor() {
    this.cargar();
    this.api
      .sucursales()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (lista) => this.sucursales.set(lista), error: () => this.sucursales.set([]) });
  }

  cargar(): void {
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .obtener(this.sucursalId)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (data) => {
          if (generation !== this.generation) return;
          this.data.set(data);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.forbidden.set(err.status === 403);
          this.error.set(mensajeError(err));
        },
      });
  }

  cambiarSucursal(id: number | null): void {
    this.sucursalId = id;
    this.data.set(null);
    this.cargar();
  }
}
