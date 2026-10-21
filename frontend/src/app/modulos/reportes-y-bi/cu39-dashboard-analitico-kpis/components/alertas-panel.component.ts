import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlertaDashboard } from '../models/dashboard-ejecutivo.model';
import { etiquetaAlerta, ordenarAlertas, rutaFuncion } from '../utils/dashboard.utils';
import { BloqueEstadoComponent } from './bloque-estado.component';

/**
 * Alertas accionables ordenadas por severidad. Cada una navega a `/app/:funcion` con el mismo
 * slug que usa el menú lateral (`erp-navigation.ts`); si la función no existe no es clicable.
 */
@Component({
  selector: 'app-alertas-panel',
  standalone: true,
  imports: [RouterLink, BloqueEstadoComponent],
  templateUrl: './alertas-panel.component.html',
  styleUrl: './alertas-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertasPanelComponent {
  readonly alertas = input<AlertaDashboard[] | null>(null);
  readonly loading = input(false);

  readonly items = computed(() =>
    ordenarAlertas(this.alertas() ?? []).map((a) => ({ ...a, ruta: rutaFuncion(a.funcionId), etiqueta: etiquetaAlerta(a.tipo) })),
  );
}
