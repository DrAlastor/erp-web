import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ArqueoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { claseResultado, resultadoArqueo } from '../utils/arqueo.utils';

/** Desglose del arqueo: cálculo del efectivo esperado y totales por forma de pago (vista previa o turno cerrado). */
@Component({
  selector: 'app-arqueo-desglose',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './arqueo-desglose.component.html',
  styleUrl: './arqueo-desglose.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArqueoDesgloseComponent {
  readonly arqueo = input.required<ArqueoResponse>();

  readonly resultado = computed(() => resultadoArqueo(this.arqueo().diferencia));
  readonly claseResultado = claseResultado;
}
