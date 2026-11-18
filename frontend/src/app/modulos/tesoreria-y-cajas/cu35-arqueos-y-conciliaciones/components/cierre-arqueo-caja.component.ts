import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { CierreTurnoComponent } from './cierre-turno.component';
import { HistorialTurnosComponent } from './historial-turnos.component';

type TabCaja = 'cierre' | 'historial';

/**
 * HU-35 / CU35 Cierre, Arqueo y Consulta de Caja. Pestañas "Cerrar mi turno" e "Historial".
 * Permisos: FACTURACION_CONSULTAR (lectura) y FACTURACION_CREAR (cerrar turno).
 */
@Component({
  selector: 'app-cierre-arqueo-caja',
  standalone: true,
  imports: [DecimalPipe, CierreTurnoComponent, HistorialTurnosComponent],
  templateUrl: './cierre-arqueo-caja.component.html',
  styleUrl: './cierre-arqueo-caja.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CierreArqueoCajaComponent {
  readonly tab = signal<TabCaja>('cierre');
  readonly ultimoCierre = signal<TurnoResponse | null>(null);
  /** Turno cuyo detalle se abre al entrar al historial desde "Ver en historial". */
  readonly detalleAbrir = signal<number | null>(null);

  cambiarTab(tab: TabCaja): void {
    this.detalleAbrir.set(null);
    this.tab.set(tab);
  }

  onCerrado(turno: TurnoResponse): void {
    this.ultimoCierre.set(turno);
  }

  verCierreEnHistorial(): void {
    this.detalleAbrir.set(this.ultimoCierre()?.id ?? null);
    this.tab.set('historial');
  }
}
