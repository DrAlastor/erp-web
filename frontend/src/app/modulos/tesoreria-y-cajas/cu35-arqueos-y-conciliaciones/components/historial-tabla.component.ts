import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RUTA_APERTURA_CAJA, TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { claseEstadoTurno, fechaHoraCorta } from '../../cu32-cuentas-bancarias-y-cajas/utils/caja.utils';
import { claseResultado, resultadoArqueo } from '../utils/arqueo.utils';

/** Tabla paginada del historial de turnos de caja con acceso al detalle. */
@Component({
  selector: 'app-historial-tabla',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './historial-tabla.component.html',
  styleUrl: './historial-tabla.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialTablaComponent {
  readonly turnos = input.required<TurnoResponse[]>();
  readonly loading = input(false);
  readonly filtrado = input(false);
  readonly page = input(0);
  readonly pages = input(0);
  readonly total = input(0);

  readonly ver = output<number>();
  readonly navegar = output<number>();
  readonly limpiar = output<void>();

  readonly rutaApertura = RUTA_APERTURA_CAJA;
  readonly fecha = fechaHoraCorta;
  readonly claseEstado = claseEstadoTurno;
  readonly resultado = resultadoArqueo;
  readonly claseResultado = claseResultado;
  readonly filasSkeleton = [1, 2, 3, 4, 5];
}
