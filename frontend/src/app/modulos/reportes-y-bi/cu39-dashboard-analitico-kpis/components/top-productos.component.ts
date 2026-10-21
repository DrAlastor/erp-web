import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TopProductoVenta } from '../models/dashboard-ejecutivo.model';
import { porcentaje } from '../utils/dashboard.utils';
import { BloqueEstadoComponent } from './bloque-estado.component';

/** Top 5 de productos vendidos en el mes actual, con barras horizontales en CSS. */
@Component({
  selector: 'app-top-productos',
  standalone: true,
  imports: [DecimalPipe, BloqueEstadoComponent],
  template: `
    <section class="panel" aria-labelledby="dash-top-title">
      <div class="panel-head">
        <div><p class="eyebrow">Mes actual</p><h3 id="dash-top-title">Productos más vendidos</h3></div>
      </div>
      <app-bloque-estado nombre="Productos más vendidos" [loading]="loading()" [disponible]="productos() !== null"
        [vacio]="!filas().length" vacioTexto="Aún no hay ventas de productos este mes.">
        <ol class="ranking">
          @for (p of filas(); track p.productoId; let i = $index) {
            <li>
              <span class="pos" aria-hidden="true">{{ i + 1 }}</span>
              <div class="info">
                <div class="linea"><span class="nombre">{{ p.nombre }}</span>
                  <strong class="mono-val">Bs. {{ p.total | number: '1.2-2' }}</strong></div>
                <div class="barra" aria-hidden="true"><span [style.width.%]="p.pct"></span></div>
                <small>{{ p.cantidad | number: '1.0-2' }} unidades</small>
              </div>
            </li>
          }
        </ol>
      </app-bloque-estado>
    </section>
  `,
  styleUrl: './top-productos.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopProductosComponent {
  readonly productos = input<TopProductoVenta[] | null>(null);
  readonly loading = input(false);

  readonly filas = computed(() => {
    const lista = (this.productos() ?? []).slice(0, 5);
    const max = Math.max(0, ...lista.map((p) => p.total));
    return lista.map((p) => ({ ...p, pct: porcentaje(p.total, max) }));
  });
}
