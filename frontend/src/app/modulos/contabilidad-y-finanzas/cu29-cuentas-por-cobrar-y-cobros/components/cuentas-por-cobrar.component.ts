import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CuentasPorCobrarService } from '../services/cuentas-por-cobrar.service';
import {
  Cobro,
  CuentaPorCobrar,
  CuotaCxc,
  EstadoCuentaCliente,
  FILTROS_ESTADO_CXC,
  FiltroEstadoCxc,
  Opcion,
  ResumenCxc,
  TRAMOS_ANTIGUEDAD,
} from '../models/cuenta-por-cobrar.model';
import { errorMonto, imprimirRecibo, textoVencimiento } from '../utils/cxc.utils';
import { hoyIso, mensajeError } from '../../cu27-gestionar-asientos-contables/utils/asiento.utils';

type Vista = 'cartera' | 'detalle' | 'estado-cuenta';

/**
 * HU-29 / CU29 — Gestionar Cuentas por Cobrar y Cobros.
 *
 * <p>Cartera con su tablero (saldo, vencido, antigüedad y clientes con más deuda), detalle
 * de cada cuota con su historial de cobros, registro de cobros parciales o totales con
 * recibo imprimible y estado de cuenta por cliente. Las cuotas las crea la venta a crédito
 * (CU19); el asiento contable y el ingreso de caja de cada cobro los registra el backend.
 */
@Component({
  selector: 'app-cuentas-por-cobrar',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './cuentas-por-cobrar.component.html',
  styleUrl: './cuentas-por-cobrar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CuentasPorCobrarComponent {
  private readonly api = inject(CuentasPorCobrarService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly filtros = FILTROS_ESTADO_CXC;
  readonly tramos = TRAMOS_ANTIGUEDAD;
  readonly vista = signal<Vista>('cartera');
  readonly cuotas = signal<CuotaCxc[]>([]);
  readonly resumen = signal<ResumenCxc | null>(null);
  readonly clientes = signal<Opcion[]>([]);
  readonly formasPago = signal<Opcion[]>([]);
  readonly detalle = signal<CuentaPorCobrar | null>(null);
  readonly estadoCuenta = signal<EstadoCuentaCliente | null>(null);
  readonly puedeCobrar = signal(false);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);

  search = '';
  estado: FiltroEstadoCxc = '';
  clienteId: number | null = null;
  page = 0;

  // formulario de cobro
  readonly cobrando = signal(false);
  readonly errorCobro = signal('');
  readonly ultimoCobro = signal<{ cobro: Cobro; asiento: string } | null>(null);
  cobro = this.cobroVacio();

  constructor() {
    this.load();
    this.cargarResumen();
    this.api.clientes('').pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (c) => this.clientes.set(c), error: () => undefined });
    this.api.formasPago().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (f) => this.formasPago.set(f), error: () => undefined });
    this.api.permisos().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (p) => this.puedeCobrar.set(p.cobrar), error: () => undefined });
  }

  // ------------------------------------------------------------- cartera --

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.api
      .list(this.search.trim(), this.estado, this.clienteId, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          if (generation !== this.generation) return;
          this.cuotas.set(r.content);
          this.total.set(r.totalElements);
          this.pages.set(r.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  filtrarVencidas(): void {
    this.estado = 'VENCIDA';
    this.load(true);
  }

  limpiarFiltros(): void {
    this.search = '';
    this.estado = '';
    this.clienteId = null;
    this.load(true);
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  volver(): void {
    this.vista.set('cartera');
    this.detalle.set(null);
    this.estadoCuenta.set(null);
    this.cobrando.set(false);
    this.ultimoCobro.set(null);
    this.load();
    this.cargarResumen();
  }

  vencimiento(c: CuotaCxc): string {
    return textoVencimiento(c.fechaVencimiento, c.vencida, c.diasVencida);
  }

  // ------------------------------------------------------------- detalle --

  abrir(id: number): void {
    this.limpiarMensajes();
    this.ultimoCobro.set(null);
    this.busy.set(true);
    this.api
      .detail(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (d) => {
          this.busy.set(false);
          this.detalle.set(d);
          this.cobrando.set(false);
          this.vista.set('detalle');
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  nuevoCobro(d: CuentaPorCobrar): void {
    this.cobro = { ...this.cobroVacio(), monto: d.cuota.saldo };
    this.errorCobro.set('');
    this.cobrando.set(true);
  }

  registrarCobro(d: CuentaPorCobrar): void {
    if (this.busy()) return;
    const problema = !this.cobro.formaPagoId ? 'Elige la forma de pago.' : errorMonto(this.cobro.monto, d.cuota.saldo);
    this.errorCobro.set(problema);
    if (problema) return;
    this.busy.set(true);
    this.limpiarMensajes();
    this.api
      .registrarCobro(d.cuota.id, {
        formaPagoId: Number(this.cobro.formaPagoId),
        monto: Number(this.cobro.monto),
        fechaCobro: this.cobro.fechaCobro || null,
        transaccionExterna: this.cobro.transaccionExterna.trim() || null,
        observaciones: this.cobro.observaciones.trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          this.busy.set(false);
          this.cobrando.set(false);
          this.detalle.set(r.cuenta);
          this.ultimoCobro.set({ cobro: r.cobro, asiento: r.numeroAsiento });
          this.success.set(
            r.cuenta.cuota.estado === 'PAGADO'
              ? `Cobro ${r.cobro.numeroRecibo} registrado. La cuota quedó pagada.`
              : `Cobro ${r.cobro.numeroRecibo} registrado. Saldo pendiente: Bs ${r.cuenta.cuota.saldo.toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}.`,
          );
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.errorCobro.set(mensajeError(err, 'cuentas por cobrar'));
        },
      });
  }

  imprimir(d: CuentaPorCobrar, c: Cobro): void {
    imprimirRecibo(d, c);
  }

  // ------------------------------------------------------ estado de cuenta --

  verEstadoCuenta(clienteId: number): void {
    this.limpiarMensajes();
    this.busy.set(true);
    this.api
      .estadoCuenta(clienteId)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (e) => {
          this.busy.set(false);
          this.estadoCuenta.set(e);
          this.vista.set('estado-cuenta');
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  // --------------------------------------------------------------- apoyo --

  private cargarResumen(): void {
    this.api.resumen().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (r) => this.resumen.set(r), error: () => undefined });
  }

  private cobroVacio() {
    return { formaPagoId: null as number | null, monto: null as number | null, fechaCobro: hoyIso(), transaccionExterna: '', observaciones: '' };
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.success.set('');
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && this.vista() === 'cartera');
    this.error.set(mensajeError(err, 'cuentas por cobrar'));
  }
}
