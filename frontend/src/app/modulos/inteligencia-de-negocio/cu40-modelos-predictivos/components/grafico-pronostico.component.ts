import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Pronostico } from '../models/inteligencia-negocio.model';
import { etiquetaPeriodo, geometria } from '../utils/ml.utils';

/** Línea de ventas reales + pronóstico punteado con banda del 80 % (SVG, sin librerías). */
@Component({
  selector: 'app-grafico-pronostico',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <div class="grafico">
      <span class="max mono" aria-hidden="true">Bs. {{ g().maximo | number: '1.0-0' }}</span>
      <svg [attr.viewBox]="'0 0 ' + ancho + ' ' + alto" preserveAspectRatio="none" role="img"
        [attr.aria-label]="'Ventas reales y pronóstico; ' + (datos()?.pronostico?.length ?? 0) + ' periodos pronosticados'"
        (mouseleave)="activo.set(null)">
        <line x1="0" [attr.y1]="alto / 2" [attr.x2]="ancho" [attr.y2]="alto / 2" class="guia" />
        <line x1="0" [attr.y1]="alto - 8" [attr.x2]="ancho" [attr.y2]="alto - 8" class="base" />
        @if (g().divisor !== null) {
          <line [attr.x1]="g().divisor" y1="0" [attr.x2]="g().divisor" [attr.y2]="alto" class="divisor" />
        }
        @if (g().banda) { <polygon [attr.points]="g().banda" class="banda" /> }
        <path [attr.d]="g().historia" class="real" />
        @if (g().pronostico) { <path [attr.d]="g().pronostico" class="pron" /> }
        @for (p of g().puntos; track p.periodo) {
          <circle [attr.cx]="p.x" [attr.cy]="p.y" [attr.r]="activo() === p.periodo ? 5 : 2.6"
            [class]="p.tipo" (mouseenter)="activo.set(p.periodo)">
            <title>{{ etiqueta(p.periodo) }}: Bs. {{ p.valor | number: '1.0-0' }}{{ p.tipo === 'pronostico' ? ' (pronóstico)' : '' }}</title>
          </circle>
        }
      </svg>
      <div class="eje-x" aria-hidden="true">
        @for (t of g().ticks; track t.x) { <span [style.left.%]="t.x">{{ t.texto }}</span> }
      </div>
      @if (detalle(); as d) {
        <p class="detalle" role="status">
          <strong>{{ etiqueta(d.periodo) }}</strong> ·
          @if (d.tipo === 'pronostico') {
            pronóstico Bs. {{ d.valor | number: '1.0-0' }} (entre {{ d.bajo | number: '1.0-0' }} y {{ d.alto | number: '1.0-0' }})
          } @else {
            real Bs. {{ d.valor | number: '1.0-0' }}
          }
        </p>
      }
      <div class="leyenda">
        <span><i class="l-real"></i>Ventas reales</span>
        <span><i class="l-pron"></i>Pronóstico</span>
        <span><i class="l-banda"></i>Intervalo del 80 %</span>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .grafico { position: relative; padding-top: 18px; }
    svg { width: 100%; height: 260px; display: block; overflow: visible; }
    .max { position: absolute; top: 0; left: 0; font-size: 11px; color: var(--text-muted); }
    .mono { font-family: var(--font-mono); }
    .guia { stroke: var(--bg-card-border); stroke-dasharray: 4 4; }
    .base { stroke: var(--bg-card-border); }
    .divisor { stroke: var(--text-dim); stroke-dasharray: 2 4; }
    .banda { fill: var(--primary-light); stroke: none; }
    .real { fill: none; stroke: var(--text-main); stroke-width: 2; vector-effect: non-scaling-stroke; }
    .pron { fill: none; stroke: var(--primary); stroke-width: 2.4; stroke-dasharray: 6 5; vector-effect: non-scaling-stroke; }
    circle { cursor: pointer; transition: r 120ms ease; }
    circle.real { fill: var(--text-main); stroke: none; }
    circle.pronostico { fill: var(--primary); }
    .eje-x { position: relative; height: 18px; margin-top: 6px; font-size: 10px; color: var(--text-muted); }
    .eje-x span { position: absolute; transform: translateX(-50%); white-space: nowrap; }
    .detalle { margin: 6px 0 0; font-size: 12px; color: var(--text-secondary); }
    .leyenda { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 8px; font-size: 11px; color: var(--text-muted); }
    .leyenda i { display: inline-block; width: 18px; height: 3px; margin-right: 6px; vertical-align: middle; border-radius: 2px; }
    .l-real { background: var(--text-main); }
    .l-pron { background: repeating-linear-gradient(90deg, var(--primary) 0 6px, transparent 6px 10px); }
    .l-banda { height: 10px !important; background: var(--primary-light); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraficoPronosticoComponent {
  readonly datos = input<Pronostico | null>(null);
  readonly ancho = 720;
  readonly alto = 260;
  readonly activo = signal<string | null>(null);
  readonly g = computed(() => geometria(this.datos(), this.ancho, this.alto));
  readonly detalle = computed(() => {
    const a = this.activo();
    return a ? this.g().puntos.find((p) => p.periodo === a) ?? null : null;
  });

  etiqueta(periodo: string): string {
    return etiquetaPeriodo(periodo, this.datos()?.frecuencia ?? 'MENSUAL');
  }
}
