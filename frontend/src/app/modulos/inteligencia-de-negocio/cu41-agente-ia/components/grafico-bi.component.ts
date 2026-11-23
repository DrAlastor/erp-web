import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Fila, Visual } from '../models/agente.model';
import { PALETA, barras, columnas, etiqueta, formato, geometriaLinea, porciones } from '../utils/graficos';

/**
 * Gráfico de un resultado según el `Visual` que decide el backend (línea, barras, torta o tabla).
 * El frontend no elige qué graficar.
 */
@Component({
  selector: 'app-grafico-bi',
  standalone: true,
  template: `
    <figure class="grafico">
      <figcaption>
        <strong>{{ visual().titulo }}</strong>
        @if (guardable()) {
          <button type="button" class="guardar" (click)="guardar.emit()" [disabled]="guardado()">
            {{ guardado() ? '✓ Guardado' : '☆ Guardar' }}
          </button>
        }
      </figcaption>

      @switch (visual().tipo) {
        @case ('linea') {
          <div class="lienzo">
            <span class="max">{{ fmt(linea().maximo) }}</span>
            <svg [attr.viewBox]="'0 0 ' + ancho + ' ' + alto" preserveAspectRatio="none" role="img" [attr.aria-label]="visual().titulo">
              <line x1="0" [attr.y1]="alto - 10" [attr.x2]="ancho" [attr.y2]="alto - 10" class="base" />
              @if (linea().banda) { <polygon [attr.points]="linea().banda" class="banda" /> }
              @for (s of linea().series; track s.nombre) {
                <path [attr.d]="s.ruta" [attr.stroke]="s.color" class="trazo" />
                @for (p of s.puntos; track $index) {
                  <circle [attr.cx]="p.x" [attr.cy]="p.y" r="2.6" [attr.fill]="s.color"><title>{{ p.etiqueta }}: {{ fmt(p.valor) }}</title></circle>
                }
              }
            </svg>
            <div class="eje">@for (t of linea().ticks; track t.x) { <span [style.left.%]="t.x">{{ t.texto }}</span> }</div>
          </div>
          @if (visual().series.length > 1 || visual().rango) {
            <div class="leyenda">
              @for (s of linea().series; track s.nombre) { <span><i [style.background]="s.color"></i>{{ etiqueta(s.nombre) }}</span> }
              @if (visual().rango) { <span><i class="banda-i"></i>Rango del 80 %</span> }
            </div>
          }
        }
        @case ('barras') {
          <ul class="barras">
            @for (b of barrasG(); track $index) {
              <li [title]="b.etiqueta + ': ' + fmt(b.valor)">
                <span class="et">{{ b.etiqueta }}</span>
                <span class="pista"><i [style.width.%]="b.ancho" [class.neg]="b.negativo"></i></span>
                <span class="val">{{ fmt(b.valor) }}</span>
              </li>
            }
          </ul>
        }
        @case ('torta') {
          <div class="torta">
            <svg viewBox="0 0 160 160" role="img" [attr.aria-label]="visual().titulo">
              @for (p of tortaG(); track $index) { <path [attr.d]="p.ruta" [attr.fill]="p.color"><title>{{ p.etiqueta }}: {{ fmt(p.valor) }}</title></path> }
            </svg>
            <ul>
              @for (p of tortaG(); track $index) {
                <li><i [style.background]="p.color"></i>{{ p.etiqueta }} <b>{{ p.porcentaje.toFixed(1) }} %</b></li>
              }
            </ul>
          </div>
        }
        @default {
          <div class="tabla">
            <table>
              <thead><tr>@for (c of cols(); track c) { <th>{{ etiqueta(c) }}</th> }</tr></thead>
              <tbody>
                @for (f of filas().slice(0, 50); track $index) {
                  <tr>@for (c of cols(); track c) { <td>{{ fmt(f[c]) }}</td> }</tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </figure>
  `,
  styles: [`
    :host { display: block; min-width: 0; }
    .grafico { margin: 0; padding: 12px 14px; border-radius: 12px; border: 1px solid var(--bg-card-border); background: var(--bg-surface); }
    figcaption { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 13px; }
    .guardar { padding: 4px 10px; border-radius: 8px; border: 1px solid var(--bg-card-border); background: transparent; font-size: 12px; cursor: pointer; color: var(--primary); }
    .guardar:disabled { color: var(--text-muted); cursor: default; }
    .lienzo { position: relative; padding-top: 14px; }
    .max { position: absolute; top: 0; left: 0; font-size: 10px; color: var(--text-muted); font-family: var(--font-mono); }
    svg { display: block; width: 100%; height: 180px; overflow: visible; }
    .base { stroke: var(--bg-card-border); }
    .trazo { fill: none; stroke-width: 2.2; vector-effect: non-scaling-stroke; }
    .banda { fill: var(--primary-light); }
    .eje { position: relative; height: 16px; margin-top: 4px; font-size: 10px; color: var(--text-muted); }
    .eje span { position: absolute; transform: translateX(-50%); white-space: nowrap; }
    .leyenda { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 6px; font-size: 11px; color: var(--text-muted); }
    .leyenda i { display: inline-block; width: 12px; height: 4px; margin-right: 5px; vertical-align: middle; border-radius: 2px; }
    .banda-i { height: 9px !important; background: var(--primary-light); }
    .barras { display: flex; flex-direction: column; gap: 5px; margin: 0; padding: 0; list-style: none; font-size: 12px; }
    .barras li { display: grid; grid-template-columns: minmax(80px, 34%) 1fr auto; align-items: center; gap: 8px; }
    .et { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-secondary); }
    .pista { height: 10px; border-radius: 5px; background: var(--bg-subtle); overflow: hidden; }
    .pista i { display: block; height: 100%; border-radius: 5px; background: var(--primary); }
    .pista i.neg { background: var(--destructive); }
    .val { font-family: var(--font-mono); font-size: 11px; }
    .torta { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
    .torta svg { width: 140px; height: 140px; }
    .torta ul { margin: 0; padding: 0; list-style: none; font-size: 12px; }
    .torta li { margin: 3px 0; }
    .torta li i { display: inline-block; width: 10px; height: 10px; margin-right: 6px; border-radius: 3px; vertical-align: middle; }
    .tabla { max-height: 280px; overflow: auto; -webkit-overflow-scrolling: touch; border-radius: 8px; border: 1px solid var(--bg-card-border); }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th, td { padding: 6px 10px; border-bottom: 1px solid var(--bg-card-border); text-align: left; white-space: nowrap; }
    th { position: sticky; top: 0; background: var(--bg-subtle); font-weight: 600; z-index: 1; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraficoBiComponent {
  readonly visual = input.required<Visual>();
  readonly filas = input<Fila[]>([]);
  readonly guardable = input(false);
  readonly guardado = input(false);
  readonly guardar = output<void>();

  readonly ancho = 600;
  readonly alto = 180;
  readonly paleta = PALETA;
  readonly etiqueta = etiqueta;
  readonly fmt = formato;

  readonly linea = computed(() => geometriaLinea(this.visual(), this.filas(), this.ancho, this.alto));
  readonly barrasG = computed(() => barras(this.visual(), this.filas()));
  readonly tortaG = computed(() => porciones(this.visual(), this.filas()));
  readonly cols = computed(() => columnas(this.filas()));
}
