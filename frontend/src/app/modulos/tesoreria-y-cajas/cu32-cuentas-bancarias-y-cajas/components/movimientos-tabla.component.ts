import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MovimientoResponse } from '../models/caja.model';
import { claseTipoMovimiento, fechaHoraCorta } from '../utils/caja.utils';

/**
 * Tabla de un turno (más reciente primero): ingresos/egresos manuales y cobros de ventas (tipo VENTA).
 * La reutiliza el detalle de turno de CU35.
 */
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
  readonly vacio = input('Aún no hay ventas ni movimientos en este turno. Las ventas aparecen aquí solas; los ingresos y egresos regístralos con el formulario de arriba.');

  readonly fecha = fechaHoraCorta;
  readonly claseTipo = claseTipoMovimiento;
  readonly filasSkeleton = [1, 2, 3];
}
