import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { VentasBloque } from '../models/dashboard-ejecutivo.model';
import { barrasSerie, claseVariacion, fechaCorta, textoVariacion } from '../utils/dashboard.utils';
import { BloqueEstadoComponent } from './bloque-estado.component';

/** Gráfico de barras SVG (sin librerías) de las ventas diarias de los últimos 30 días. */
@Component({
  selector: 'app-ventas-chart',
  standalone: true,
  imports: [DecimalPipe, BloqueEstadoComponent],
  templateUrl: './ventas-chart.component.html',
  styleUrl: './ventas-chart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VentasChartComponent {
  readonly ventas = input<VentasBloque | null>(null);
  readonly loading = input(false);

  readonly ancho = 600;
  readonly alto = 150;

  readonly serie = computed(() => this.ventas()?.serieDiaria ?? []);
  readonly barras = computed(() => barrasSerie(this.serie(), this.ancho, this.alto));
  readonly maximo = computed(() => Math.max(0, ...this.serie().map((p) => Number(p.total) || 0)));
  readonly total30 = computed(() => this.serie().reduce((acc, p) => acc + (Number(p.total) || 0), 0));
  readonly sinVentas = computed(() => this.maximo() === 0);
  readonly etiquetasX = computed(() => {
    const s = this.serie();
    if (!s.length) return [];
    const idx = [...new Set([0, Math.floor((s.length - 1) / 2), s.length - 1])];
    return idx.map((i) => ({ x: ((i + 0.5) / s.length) * 100, texto: fechaCorta(s[i].fecha) }));
  });

  readonly fechaCorta = fechaCorta;
  readonly textoVariacion = textoVariacion;
  readonly claseVariacion = claseVariacion;
}
