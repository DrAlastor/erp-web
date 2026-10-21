import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DashboardEjecutivoResponse } from '../models/dashboard-ejecutivo.model';
import { claseVariacion } from '../utils/dashboard.utils';
import { KpiCardComponent } from './kpi-card.component';

/** Fila de KPIs: ventas hoy, ventas del mes (con variación), por cobrar, por pagar, stock bajo y caja hoy. */
@Component({
  selector: 'app-kpi-fila',
  standalone: true,
  imports: [DecimalPipe, KpiCardComponent],
  templateUrl: './kpi-fila.component.html',
  styleUrl: './kpi-fila.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiFilaComponent {
  readonly data = input<DashboardEjecutivoResponse | null>(null);
  readonly loading = input(false);

  readonly ventas = computed(() => this.data()?.ventas ?? null);
  readonly cobrar = computed(() => this.data()?.cobrar ?? null);
  readonly pagar = computed(() => this.data()?.pagar ?? null);
  readonly inventario = computed(() => this.data()?.inventario ?? null);
  readonly caja = computed(() => this.data()?.caja ?? null);
  readonly netoCaja = computed(() => {
    const c = this.caja();
    return c ? c.ingresosHoy - c.egresosHoy : 0;
  });

  readonly claseVariacion = claseVariacion;
}
