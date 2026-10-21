import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { CuentaPorPagarResumenResponse } from '../models/cuenta-por-pagar.model';
import { claseEstadoCxp, estadoVisible } from '../utils/cxp.utils';

/** Tabla paginada de cuotas por pagar; resalta las vencidas con sus días de atraso. */
@Component({
  selector: 'app-cxp-tabla',
  standalone: true,
  imports: [DecimalPipe, SiPermisoDirective],
  templateUrl: './cxp-tabla.component.html',
  styleUrl: './cxp-tabla.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CxpTablaComponent {
  readonly cuentas = input.required<CuentaPorPagarResumenResponse[]>();
  readonly loading = input(false);
  readonly filtrado = input(false);
  readonly page = input(0);
  readonly pages = input(0);
  readonly total = input(0);

  readonly ver = output<number>();
  readonly pagar = output<number>();
  readonly navegar = output<number>();

  readonly estadoVisible = estadoVisible;
  readonly claseEstado = claseEstadoCxp;
  readonly filasSkeleton = [1, 2, 3, 4, 5];
}
