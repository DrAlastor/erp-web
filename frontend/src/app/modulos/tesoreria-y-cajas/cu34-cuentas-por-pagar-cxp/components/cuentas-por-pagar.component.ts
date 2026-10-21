import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SiPermisoDirective } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/directives/si-permiso.directive';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import {
  CuentaPorPagarResumenResponse, FILTROS_ESTADO_CXP, FiltroEstadoCxp, ProveedorOpcionResponse, TopProveedorCxp,
} from '../models/cuenta-por-pagar.model';
import { mensajeError } from '../utils/cxp.utils';
import { CxpResumenComponent } from './cxp-resumen.component';
import { CxpTablaComponent } from './cxp-tabla.component';
import { CxpDetalleComponent } from './cxp-detalle.component';
import { CxpGenerarCuotasComponent } from './cxp-generar-cuotas.component';

/**
 * HU-24 / CU34 — Gestionar Cuentas por Pagar. Resumen, listado filtrable y orquestación de modales.
 * Permisos: CONTABILIDAD_CONSULTAR (lectura), CONTABILIDAD_CREAR (registrar pago y generar cuotas).
 */
@Component({
  selector: 'app-cuentas-por-pagar',
  standalone: true,
  imports: [FormsModule, SiPermisoDirective, CxpResumenComponent, CxpTablaComponent, CxpDetalleComponent, CxpGenerarCuotasComponent],
  templateUrl: './cuentas-por-pagar.component.html',
  styleUrl: './cuentas-por-pagar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CuentasPorPagarComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly filtros = FILTROS_ESTADO_CXP;
  readonly cuentas = signal<CuentaPorPagarResumenResponse[]>([]);
  readonly proveedores = signal<ProveedorOpcionResponse[]>([]);
  readonly loading = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  /** Se incrementa tras cada pago o generación para que el resumen se recargue. */
  readonly refresco = signal(0);
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly detalle = signal<{ id: number; pagar: boolean } | null>(null);
  readonly generando = signal(false);

  search = '';
  estado: FiltroEstadoCxp = '';
  proveedorId: number | null = null;
  page = 0;

  constructor() {
    this.load();
    this.api.proveedores('').pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (l) => this.proveedores.set(l), error: () => {} });
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.estado, this.proveedorId, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          if (generation !== this.generation) return;
          this.cuentas.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.forbidden.set(err.status === 403);
          this.error.set(mensajeError(err));
        },
      });
  }

  filtrarProveedor(p: TopProveedorCxp): void {
    if (!this.proveedores().some((o) => o.id === p.proveedorId)) {
      this.proveedores.update((l) => [...l, { id: p.proveedorId, razonSocial: p.razonSocial, nitCi: '' }]);
    }
    this.proveedorId = p.proveedorId;
    this.load(true);
  }

  limpiarFiltros(): void {
    this.search = '';
    this.estado = '';
    this.proveedorId = null;
    this.load(true);
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  onChanged(message: string): void {
    this.success.set(message);
    this.load();
    this.refresco.update((v) => v + 1);
  }

  onGenerado(cuotas: CuentaPorPagarResumenResponse[]): void {
    this.generando.set(false);
    this.onChanged(`Se generaron ${cuotas.length} cuotas para la factura ${cuotas[0]?.numeroFactura ?? ''}.`);
  }
}
