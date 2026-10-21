import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ComercialBloque } from '../models/dashboard-ejecutivo.model';
import { claseEstado } from '../../../ventas-y-facturacion/cu16-pedidos-de-venta/utils/pedido.utils';
import { porcentaje } from '../utils/dashboard.utils';
import { BloqueEstadoComponent } from './bloque-estado.component';

/** Pedidos por estado (badges de CU16) más cotizaciones vigentes y clientes activos. */
@Component({
  selector: 'app-pedidos-estado',
  standalone: true,
  imports: [DecimalPipe, BloqueEstadoComponent],
  template: `
    <section class="panel" aria-labelledby="dash-pedidos-title">
      <div class="panel-head"><div><p class="eyebrow">Comercial</p><h3 id="dash-pedidos-title">Pedidos por estado</h3></div></div>
      <app-bloque-estado nombre="Pedidos por estado" [loading]="loading()" [disponible]="!!comercial()" [filas]="4"
        [vacio]="!filas().length" vacioTexto="No hay pedidos registrados.">
        <ul class="estados">
          @for (e of filas(); track e.estado) {
            <li>
              <span class="badge" [class]="claseEstado(e.estado)">{{ e.estado }}</span>
              <span class="barra" aria-hidden="true"><span [style.width.%]="e.pct"></span></span>
              <strong class="mono-val">{{ e.cantidad | number }}</strong>
            </li>
          }
        </ul>
      </app-bloque-estado>
      @if (!loading() && comercial(); as c) {
        <div class="extra">
          <div><span>Cotizaciones vigentes</span><strong class="mono-val">{{ c.cotizacionesVigentes | number }}</strong></div>
          <div><span>Clientes activos</span><strong class="mono-val">{{ c.clientesActivos | number }}</strong></div>
        </div>
      }
    </section>
  `,
  styleUrl: './pedidos-estado.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidosEstadoComponent {
  readonly comercial = input<ComercialBloque | null>(null);
  readonly loading = input(false);
  readonly claseEstado = claseEstado;

  readonly filas = computed(() => {
    const lista = (this.comercial()?.pedidosPorEstado ?? []).filter((e) => e.cantidad > 0);
    const max = Math.max(0, ...lista.map((e) => e.cantidad));
    return lista.map((e) => ({ ...e, pct: porcentaje(e.cantidad, max) }));
  });
}
