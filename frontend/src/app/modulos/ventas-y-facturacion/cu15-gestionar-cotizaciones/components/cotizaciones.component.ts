import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { Cliente, ClientesService } from '../../cu13-gestionar-clientes/services/clientes.service';
import { CotizacionesService } from '../services/cotizaciones.service';
import { imprimirProforma } from './proforma';
import {
  Cotizacion,
  CotizacionRequest,
  CotizacionResumen,
  ESTADOS_COTIZACION,
  EstadoCotizacion,
  OpcionesCotizacion,
  PedidoGenerado,
  RenglonFormulario,
} from '../models/cotizacion.model';

type Vista = 'lista' | 'editor' | 'detalle';

/**
 * CU15 - Gestionar Cotizaciones.
 *
 * <p>El precio que se ve mientras se arma la cotización es sólo una vista previa: el
 * backend lo vuelve a tomar del catálogo al guardar.
 */
@Component({
  selector: 'app-cotizaciones',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './cotizaciones.component.html',
  styleUrl: './cotizaciones.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CotizacionesComponent {
  private readonly api = inject(CotizacionesService);
  private readonly clientesApi = inject(ClientesService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly estados = ESTADOS_COTIZACION;
  readonly vista = signal<Vista>('lista');
  readonly cotizaciones = signal<CotizacionResumen[]>([]);
  readonly opciones = signal<OpcionesCotizacion | null>(null);
  readonly seleccionada = signal<Cotizacion | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);

  // filtros
  search = '';
  estado = '';
  page = 0;

  // formulario
  readonly editandoId = signal<number | null>(null);
  readonly clientes = signal<Cliente[]>([]);
  readonly renglones = signal<RenglonFormulario[]>([]);
  readonly descuentoGlobal = signal(0);
  clienteBusqueda = '';
  clienteId: number | null = null;
  clienteNombre = '';
  validezDias = 15;
  observaciones = '';

  // diálogos
  readonly anulando = signal(false);
  readonly convirtiendo = signal(false);
  readonly pedidoGenerado = signal<PedidoGenerado | null>(null);
  motivo = '';
  sucursalId: number | null = null;

  /** Vista previa de los importes con los precios de lista. */
  readonly vistaPrevia = computed(() => {
    const productos = this.opciones()?.productos ?? [];
    let bruto = 0;
    let descuentos = this.descuentoGlobal() || 0;
    for (const r of this.renglones()) {
      const p = productos.find((x) => x.id === Number(r.productoId));
      if (!p) continue;
      bruto += (Number(r.cantidad) || 0) * p.precio;
      descuentos += Number(r.descuento) || 0;
    }
    const maximo = (bruto * (this.opciones()?.descuentoMaximoPorcentaje ?? 0)) / 100;
    return { bruto, descuentos, total: bruto - descuentos, maximo, excede: descuentos > maximo + 0.001 };
  });

  constructor() {
    this.load();
    this.api
      .options()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (o) => {
          this.opciones.set(o);
          this.sucursalId = o.sucursales[0]?.id ?? null;
        },
        error: (err) => this.fail(err),
      });
  }

  // ------------------------------------------------------------- listado --

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.estado, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          if (generation !== this.generation) return;
          this.cotizaciones.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  volver(): void {
    this.vista.set('lista');
    this.seleccionada.set(null);
    this.load();
  }

  // -------------------------------------------------------------- editor --

  nueva(): void {
    this.limpiarMensajes();
    this.editandoId.set(null);
    this.clienteId = null;
    this.clienteNombre = '';
    this.clienteBusqueda = '';
    this.clientes.set([]);
    this.validezDias = 15;
    this.observaciones = '';
    this.descuentoGlobal.set(0);
    this.renglones.set([{ productoId: null, cantidad: 1, descuento: 0 }]);
    this.vista.set('editor');
  }

  editar(c: Cotizacion): void {
    this.limpiarMensajes();
    this.editandoId.set(c.id);
    this.clienteId = c.clienteId;
    this.clienteNombre = c.clienteRazonSocial;
    this.clienteBusqueda = '';
    this.clientes.set([]);
    this.validezDias = c.validezDias;
    this.observaciones = c.observaciones ?? '';
    this.descuentoGlobal.set(Number(c.descuentoGlobal) || 0);
    this.renglones.set(
      c.items.map((i) => ({ productoId: i.productoId, cantidad: Number(i.cantidad), descuento: Number(i.descuento) })),
    );
    this.vista.set('editor');
  }

  buscarClientes(): void {
    this.clientesApi
      .list(this.clienteBusqueda.trim(), 'true', 0)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => this.clientes.set(r.content),
        error: (err) => this.fail(err),
      });
  }

  elegirCliente(c: Cliente): void {
    this.clienteId = c.id;
    this.clienteNombre = `${c.razonSocial} · NIT/CI ${c.nitCi}`;
    this.clientes.set([]);
  }

  agregarRenglon(): void {
    this.renglones.update((r) => [...r, { productoId: null, cantidad: 1, descuento: 0 }]);
  }

  quitarRenglon(i: number): void {
    this.renglones.update((r) => r.filter((_, k) => k !== i));
  }

  cambiarRenglon(i: number, campo: keyof RenglonFormulario, valor: unknown): void {
    this.renglones.update((r) => r.map((x, k) => (k === i ? { ...x, [campo]: valor } : x)));
  }

  precioDe(productoId: number | null): number {
    return this.opciones()?.productos.find((p) => p.id === Number(productoId))?.precio ?? 0;
  }

  guardar(borrador: boolean): void {
    if (this.busy()) return;
    const items = this.renglones().filter((r) => r.productoId);
    if (!this.clienteId || items.length === 0) {
      this.error.set('Elige un cliente y al menos un producto.');
      return;
    }
    const body: CotizacionRequest = {
      clienteId: this.clienteId,
      validezDias: Number(this.validezDias),
      descuentoGlobal: Number(this.descuentoGlobal()) || 0,
      observaciones: this.observaciones.trim() || null,
      borrador,
      items: items.map((r) => ({
        productoId: Number(r.productoId),
        cantidad: Number(r.cantidad),
        descuento: Number(r.descuento) || 0,
      })),
    };
    const id = this.editandoId();
    this.ejecutar(id ? this.api.update(id, body) : this.api.create(body), (c) => {
      this.mostrar(c, borrador ? 'Borrador guardado.' : `Cotización ${c.numeroCotizacion} emitida.`);
    });
  }

  // ------------------------------------------------------------- detalle --

  abrir(c: CotizacionResumen): void {
    this.limpiarMensajes();
    this.busy.set(true);
    this.api
      .detail(c.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (value) => {
          this.busy.set(false);
          this.mostrar(value);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  emitir(c: Cotizacion): void {
    this.ejecutar(this.api.emit(c.id), (r) => this.mostrar(r, 'Cotización emitida.'));
  }

  aceptar(c: Cotizacion): void {
    this.ejecutar(this.api.accept(c.id), (r) => this.mostrar(r, 'El cliente aceptó la cotización.'));
  }

  anular(): void {
    const c = this.seleccionada();
    if (!c || !this.motivo.trim()) return;
    this.ejecutar(this.api.cancel(c.id, this.motivo.trim()), (r) => {
      this.anulando.set(false);
      this.mostrar(r, 'Cotización anulada.');
    });
  }

  convertir(): void {
    const c = this.seleccionada();
    if (!c || !this.sucursalId) return;
    this.ejecutar(this.api.convert(c.id, Number(this.sucursalId)), (pedido) => {
      this.convirtiendo.set(false);
      this.pedidoGenerado.set(pedido);
      this.api
        .detail(c.id)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe((r) => this.mostrar(r, `Se generó el pedido ${pedido.numeroPedido}.`));
    });
  }

  imprimir(c: Cotizacion): void {
    imprimirProforma(c);
  }

  etiqueta(estado: EstadoCotizacion): string {
    return this.estados.find((e) => e.valor === estado)?.etiqueta ?? estado;
  }

  // --------------------------------------------------------------- apoyo --

  private mostrar(c: Cotizacion, mensaje = ''): void {
    this.seleccionada.set(c);
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
    this.pedidoGenerado.set(null);
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && this.vista() === 'lista');
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para esta operación sobre cotizaciones.'
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
