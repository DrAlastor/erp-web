import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Envoltorio de estado de un bloque del dashboard: skeleton mientras carga, aviso si el backend
 * no pudo calcular el bloque (llega null), mensaje de vacío o el contenido proyectado.
 */
@Component({
  selector: 'app-bloque-estado',
  standalone: true,
  template: `
    @if (loading()) {
      <div class="skeletons" aria-hidden="true">
        @for (l of lineas(); track l) { <span class="skeleton" [style.width.%]="l"></span> }
      </div>
      <span class="sr-only" role="status">Cargando {{ nombre() }}...</span>
    } @else if (!disponible()) {
      <p class="estado no-disponible" role="note">⚠ {{ nombre() }} no disponible en este momento.</p>
    } @else if (vacio()) {
      <p class="estado">{{ vacioTexto() }}</p>
    } @else {
      <ng-content />
    }
  `,
  styleUrl: './bloque-estado.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BloqueEstadoComponent {
  readonly nombre = input.required<string>();
  readonly loading = input(false);
  readonly disponible = input(true);
  readonly vacio = input(false);
  readonly vacioTexto = input('Sin datos para mostrar.');
  readonly filas = input(3);

  readonly lineas = computed(() => Array.from({ length: this.filas() }, (_, i) => 92 - ((i * 17) % 40)));
}
