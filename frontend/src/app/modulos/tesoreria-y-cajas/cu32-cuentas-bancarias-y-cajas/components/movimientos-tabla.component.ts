import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MovimientoResponse } from '../models/caja.model';
import { claseTipoMovimiento, fechaHoraCorta } from '../utils/caja.utils';

/** Tabla de movimientos de un turno (más reciente primero). La reutiliza el detalle de turno de CU35. */
@Component({
  selector: 'app-movimientos-tabla',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './movimientos-tabla.component.html',
  styleUrl: './movimientos-tabla.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientosTablaComponent {
  readonly movimientos = input.required<MovimientoResponse[]>();
  readonly loading = input(false);
  /** Texto del estado vacío (el turno abierto invita a registrar; el historial solo informa). */
  readonly vacio = input('Aún no hay movimientos en este turno. Registra el primero con el formulario de arriba.');

  readonly fecha = fechaHoraCorta;
  readonly claseTipo = claseTipoMovimiento;
  readonly filasSkeleton = [1, 2, 3];
}
