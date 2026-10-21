import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TonoKpi = '' | 'alerta' | 'aviso' | 'ok';

/** Tarjeta KPI del dashboard. `valor` null = el backend no pudo calcular ese bloque. */
@Component({
  selector: 'app-kpi-card',
  standalone: true,
  template: `
    <article class="kpi-card" [class]="tono()" [attr.aria-busy]="loading()">
      <span class="kpi-icon" aria-hidden="true">{{ icono() }}</span>
      <div class="kpi-body">
        <span class="kpi-label">{{ etiqueta() }}</span>
        @if (loading()) {
          <span class="skeleton val" aria-hidden="true"></span>
          <span class="skeleton nota" aria-hidden="true"></span>
        } @else if (valor() === null) {
          <strong class="kpi-val muted">—</strong>
          <small class="kpi-note">No disponible</small>
        } @else {
          <div class="kpi-val-row">
            <strong class="kpi-val">{{ valor() }}</strong>
            @if (chip()) { <span class="kpi-chip" [class]="chipClase()">{{ chip() }}</span> }
          </div>
          <small class="kpi-note">{{ nota() }}</small>
        }
      </div>
    </article>
  `,
  styleUrl: './kpi-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiCardComponent {
  readonly etiqueta = input.required<string>();
  readonly icono = input('');
  readonly valor = input<string | null>(null);
  readonly nota = input('');
  readonly chip = input('');
  readonly chipClase = input('');
  readonly tono = input<TonoKpi>('');
  readonly loading = input(false);
}
