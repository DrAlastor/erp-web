import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { DespachosService } from '../services/despachos.service';
import {
  Despacho,
  DespachoResumen,
  ESTADOS_DESPACHO,
  EstadoDespacho,
  FaltanteStock,
  PedidoPendienteAprobacion,
  PedidoPorDespachar,
  Sucursal,
} from '../models/despacho.model';

type Vista = 'pendientes' | 'notas' | 'preparar' | 'detalle';

/** Cantidad a despachar de un producto mientras se prepara la nota. */
interface RenglonPreparacion {
  productoId: number;
  productoCodigo: string;
  productoNombre: string;
  pendiente: number;
  stockDisponible: number;
  cantidad: number;
}

/** CU18 - Gestionar Despacho de Venta. */
@Component({
  selector: 'app-despachos',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './despachos.component.html',
  styleUrl: './despachos.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DespachosComponent {
  private readonly api = inject(DespachosService);
  private readonly destroy = inject(DestroyRef);

  readonly estados = ESTADOS_DESPACHO;
  readonly vista = signal<Vista>('pendientes');
  readonly pendientes = signal<PedidoPorDespachar[]>([]);
  readonly porAprobar = signal<PedidoPendienteAprobacion[]>([]);
  readonly notas = signal<DespachoResumen[]>([]);
  readonly sucursales = signal<Sucursal[]>([]);
  readonly seleccionado = signal<Despacho | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly faltantes = signal<FaltanteStock[]>([]);
  readonly total = signal(0);
  readonly pages = signal(0);

  // filtros
  sucursalFiltro: number | null = null;
  search = '';
  estado = '';
  page = 0;

  // preparación de la nota
  readonly pedido = signal<PedidoPorDespachar | null>(null);
  readonly renglones = signal<RenglonPreparacion[]>([]);
  sucursalId: number | null = null;
  conductor = '';
  placa = '';
  observaciones = '';

  // anulación
  readonly anulando = signal(false);
  motivo = '';

  constructor() {
    this.cargarPendientes();
    this.api
      .branches()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (s) => this.sucursales.set(s), error: () => this.sucursales.set([]) });
  }

  // -------------------------------------------------------------- pestañas --

  irA(vista: 'pendientes' | 'notas'): void {
    this.limpiarMensajes();
    this.vista.set(vista);
    if (vista === 'pendientes') this.cargarPendientes();
    else this.cargarNotas(true);
  }

  cargarPendientes(): void {
    this.loading.set(true);
    this.api
      .pending(this.sucursalFiltro)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          this.pendientes.set(r);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err, true);
        },
      });
    // Provisional hasta la CU17: si el usuario no puede verlos, simplemente no se muestran.
    this.api
      .awaitingApproval()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (r) => this.porAprobar.set(r), error: () => this.porAprobar.set([]) });
  }

  cargarNotas(reset = false): void {
    if (reset) this.page = 0;
    this.loading.set(true);
    this.api
      .list(this.search.trim(), this.estado, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          this.notas.set(r.content);
          this.total.set(r.totalElements);
          this.pages.set(r.totalPages);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err, true);
        },
      });
  }

  navigate(delta: number): void {
    this.page += delta;
    this.cargarNotas();
  }

  aprobar(p: PedidoPendienteAprobacion): void {
    this.ejecutar(this.api.approve(p.id), () => {
      this.success.set(`Pedido ${p.numeroPedido} aprobado: ya se puede despachar.`);
      this.cargarPendientes();
    });
  }

  // ------------------------------------------------------------- preparar --

  preparar(p: PedidoPorDespachar): void {
    this.limpiarMensajes();
    this.pedido.set(p);
    this.sucursalId = p.sucursalId;
    this.conductor = '';
    this.placa = '';
    this.observaciones = '';
    this.renglones.set(
      p.items
        .filter((i) => i.pendiente > 0)
        .map((i) => ({
          productoId: i.productoId,
          productoCodigo: i.productoCodigo,
          productoNombre: i.productoNombre,
          pendiente: Number(i.pendiente),
          stockDisponible: Number(i.stockDisponible),
          cantidad: Math.max(0, Math.min(Number(i.pendiente), Number(i.stockDisponible))),
        })),
    );
    this.vista.set('preparar');
  }

  cambiarCantidad(i: number, valor: number): void {
    this.renglones.update((r) => r.map((x, k) => (k === i ? { ...x, cantidad: valor } : x)));
  }

  registrar(): void {
    const p = this.pedido();
    if (!p || !this.sucursalId) return;
    const items = this.renglones()
      .filter((r) => Number(r.cantidad) > 0)
      .map((r) => ({ productoId: r.productoId, cantidad: Number(r.cantidad) }));
    if (items.length === 0) {
      this.error.set('Indica al menos una cantidad mayor a cero.');
      return;
    }
    this.ejecutar(
      this.api.create({
        pedidoId: p.pedidoId,
        sucursalId: Number(this.sucursalId),
        conductor: this.conductor.trim() || null,
        placa: this.placa.trim() || null,
        observaciones: this.observaciones.trim() || null,
        items,
      }),
      (d) => this.mostrar(d, `Nota ${d.numeroDespacho} registrada. Confírmala cuando la mercadería salga.`),
    );
  }

  // -------------------------------------------------------------- detalle --

  abrir(id: number): void {
    this.ejecutar(this.api.detail(id), (d) => this.mostrar(d));
  }

  confirmar(d: Despacho): void {
    this.ejecutar(this.api.confirm(d.id), (r) =>
      this.mostrar(r, `Despacho confirmado: se descontó el stock y se registró la salida en el Kardex.`),
    );
  }

  entregar(d: Despacho): void {
    this.ejecutar(this.api.deliver(d.id), (r) => this.mostrar(r, 'Despacho marcado como entregado.'));
  }

  anular(): void {
    const d = this.seleccionado();
    if (!d || !this.motivo.trim()) return;
    const salio = d.estado === 'DESPACHADO';
    this.ejecutar(this.api.cancel(d.id, this.motivo.trim()), (r) => {
      this.anulando.set(false);
      this.mostrar(r, salio ? 'Despacho anulado: el stock volvió al almacén.' : 'Despacho anulado.');
    });
  }

  etiqueta(estado: EstadoDespacho | string): string {
    return this.estados.find((e) => e.valor === estado)?.etiqueta ?? estado.replace('_', ' ').toLowerCase();
  }

  // ---------------------------------------------------------------- apoyo --

  private mostrar(d: Despacho, mensaje = ''): void {
    this.seleccionado.set(d);
    this.vista.set('detalle');
    if (mensaje) this.success.set(mensaje);
  }

  private ejecutar<T>(peticion: Observable<T>, alTerminar: (r: T) => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.limpiarMensajes();
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.busy.set(false);
        alTerminar(r);
      },
      error: (err) => {
        this.busy.set(false);
        this.fail(err);
      },
    });
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.success.set('');
    this.faltantes.set([]);
  }

  private fail(err: HttpErrorResponse, alCargar = false): void {
    this.forbidden.set(alCargar && err.status === 403);
    this.faltantes.set(err.status === 409 && Array.isArray(err.error?.faltantes) ? err.error.faltantes : []);
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para esta operación de despacho.'
        : err.status === 0
          ? 'No se pudo conectar con el servidor. Intenta nuevamente.'
          : err.status === 401
            ? 'La sesión ha expirado. Inicia sesión nuevamente.'
            : err.status >= 500
              ? 'El servidor no pudo completar la operación.'
              : err.error?.message || 'No se pudo completar la operación.',
    );
  }
}
