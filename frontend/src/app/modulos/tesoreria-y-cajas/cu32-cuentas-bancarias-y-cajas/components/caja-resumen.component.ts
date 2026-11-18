import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ArqueoResponse, TurnoResponse } from '../models/caja.model';
import { fechaHoraCorta } from '../utils/caja.utils';

/** Tarjetas del turno abierto: fondo, ingresos, egresos y efectivo esperado (datos de GET /arqueo). */
@Component({
  selector: 'app-caja-resumen',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './caja-resumen.component.html',
  styleUrl: './caja-resumen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CajaResumenComponent {
  readonly turno = input.required<TurnoResponse>();
  readonly arqueo = input<ArqueoResponse | null>(null);
  readonly loading = input(false);

  readonly fecha = fechaHoraCorta;
  readonly placeholders = [1, 2, 3, 4];
}
