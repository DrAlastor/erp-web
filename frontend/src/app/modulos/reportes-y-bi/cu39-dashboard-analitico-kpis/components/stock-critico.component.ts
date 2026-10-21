import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { InventarioBloque } from '../models/dashboard-ejecutivo.model';
import { BloqueEstadoComponent } from './bloque-estado.component';

/** Mini tabla con los 5 productos de stock más crítico y el valor del inventario. */
@Component({
  selector: 'app-stock-critico',
  standalone: true,
  imports: [DecimalPipe, RouterLink, BloqueEstadoComponent],
  template: `
    <section class="panel" aria-labelledby="dash-stock-title">
      <div class="panel-head">
        <div><p class="eyebrow">Inventario</p><h3 id="dash-stock-title">Stock crítico</h3></div>
        @if (!loading() && inventario(); as i) {
          <small>Valor del inventario <strong class="mono-val">Bs. {{ i.valorInventario | number: '1.2-2' }}</strong></small>
        }
      </div>
      <app-bloque-estado nombre="Stock crítico" [loading]="loading()" [disponible]="!!inventario()" [filas]="4"
        [vacio]="!inventario()?.alertas?.length" vacioTexto="✓ Ningún producto está por debajo de su stock mínimo.">
        <div class="table-scroll">
          <table>
            <caption class="sr-only">Productos con stock bajo o agotado</caption>
            <thead><tr><th>Producto</th><th class="num">Stock</th><th class="num">Mínimo</th></tr></thead>
            <tbody>
              @for (a of inventario()?.alertas ?? []; track a.productoId) {
                <tr [class.agotado]="a.stock <= 0">
                  <td><strong>{{ a.nombre }}</strong><span class="cell-sub">{{ a.codigo }}</span></td>
                  <td class="num">{{ a.stock | number: '1.0-2' }}</td>
                  <td class="num">{{ a.stockMinimo | number: '1.0-2' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <a class="ver-mas" [routerLink]="['/app', 'consultar-kardex']">Ver kardex de inventario →</a>
      </app-bloque-estado>
    </section>
  `,
  styleUrl: './stock-critico.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockCriticoComponent {
  readonly inventario = input<InventarioBloque | null>(null);
  readonly loading = input(false);
}
