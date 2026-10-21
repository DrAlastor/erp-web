import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { esAdministrador } from '../../compras-roles';
import { OrdenesCompraService } from '../services/ordenes-compra.service';
import { imprimirOrdenCompra } from './orden-impresa';
import {
  ESTADOS_ORDEN,
  EstadoOrdenCompra,
  OpcionesOrdenCompra,
  OrdenCompra,
  OrdenCompraRequest,
  OrdenCompraResumen,
  RenglonOrden,
  SugerenciaReposicion,
} from '../models/orden-compra.model';

type Vista = 'lista' | 'editor' | 'detalle';
type Dialogo = 'rechazar' | 'anular' | 'sugerencias' | null;

/**
 * Gestionar Órdenes de Compra (CU22 del documento del Sprint Especial).
 *
 * <p>El Almacenero registra y corrige; aprobar, rechazar, enviar y anular son del
 * Administrador. Los totales que se ven al armarla son una vista previa: el backend los
 * vuelve a calcular al guardar.
 */
@Component({
  selector: 'app-ordenes-compra',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './ordenes-compra.component.html',
  styleUrl: './ordenes-compra.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdenesCompraComponent {
  private readonly api = inject(OrdenesCompraService);
  private readonly permisos = inject(PermisosService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly estados = ESTADOS_ORDEN;
  readonly vista = signal<Vista>('lista');
  readonly ordenes = signal<OrdenCompraResumen[]>([]);
  readonly opciones = signal<OpcionesOrdenCompra | null>(null);
  readonly seleccionada = signal<OrdenCompra | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly admin = computed(() => esAdministrador(this.permisos));
  readonly hoy = new Date().toLocaleDateString('en-CA');

  // filtros
  search = '';
  estado = '';
  proveedorFiltro: number | null = null;
  desde = '';
  hasta = '';
  page = 0;

  // formulario
  readonly editandoId = signal<number | null>(null);
  readonly renglones = signal<RenglonOrden[]>([]);
  readonly descuento = signal(0);
  proveedorId: number | null = null;
  fechaEntrega = '';
  observaciones = '';

  // diálogos
  readonly dialogo = signal<Dialogo>(null);
  readonly sugerencias = signal<SugerenciaReposicion[]>([]);
  readonly elegidas = signal<ReadonlySet<number>>(new Set());
  motivo = '';
  sucursalId: number | null = null;

  /** Vista previa de los importes mientras se arma la orden. */
  readonly vistaPrevia = computed(() => {
    let subtotal = 0;
    for (const r of this.renglones()) {
      if (!r.productoId) continue;
      subtotal += this.redondear((Number(r.cantidad) || 0) * (Number(r.costoUnitario) || 0));
    }
    const descuento = Number(this.descuento()) || 0;
    return { subtotal, descuento, total: subtotal - descuento, excede: descuento > subtotal + 0.001 };
  });

  /** Lo que impide guardar, para avisarlo antes de ir al servidor. */
  readonly problemas = computed(() => {
    const p: string[] = [];
    const usados = new Set<number>();
    for (const r of this.renglones()) {
      if (!r.productoId) continue;
      if (usados.has(Number(r.productoId))) p.push('Hay un producto repetido: suma las cantidades en un solo renglón.');
      usados.add(Number(r.productoId));
      if (!(Number(r.cantidad) > 0) || !(Number(r.costoUnitario) > 0)) {
        p.push('Cantidades y costos deben ser mayores a cero.');
      }
    }
    if (this.vistaPrevia().excede) p.push('El descuento no puede superar el subtotal.');
    if (Number(this.descuento()) < 0) p.push('El descuento no puede ser negativo.');
    return [...new Set(p)];
  });

  constructor() {
    this.permisos.asegurarCargados().pipe(takeUntilDestroyed(this.destroy)).subscribe({ error: () => undefined });
    this.load();
    this.cargarOpciones();
  }

  // ------------------------------------------------------------- listado --

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(
        { search: this.search.trim(), estado: this.estado, proveedorId: this.proveedorFiltro, desde: this.desde, hasta: this.hasta },
        this.page,
      )
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          if (generation !== this.generation) return;
          this.ordenes.set(r.content);
          this.total.set(r.totalElements);
          this.pages.set(r.totalPages);
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
    this.dialogo.set(null);
    this.load();
  }

  // -------------------------------------------------------------- editor --

  nueva(): void {
    this.limpiarMensajes();
    this.cargarOpciones();
    this.editandoId.set(null);
    this.proveedorId = null;
    this.fechaEntrega = '';
    this.observaciones = '';
    this.descuento.set(0);
    this.renglones.set([{ productoId: null, cantidad: 1, costoUnitario: 0 }]);
    this.vista.set('editor');
  }

  editar(o: OrdenCompra): void {
    this.limpiarMensajes();
    this.cargarOpciones();
    this.editandoId.set(o.id);
    this.proveedorId = o.proveedorId;
    this.fechaEntrega = o.fechaEntrega ?? '';
    this.observaciones = '';
    this.descuento.set(Number(o.descuento) || 0);
    this.renglones.set(
      o.detalles.map((d) => ({
        productoId: d.productoId,
        cantidad: Number(d.cantidad),
        costoUnitario: Number(d.costoUnitario),
      })),
    );
    this.vista.set('editor');
  }

  agregarRenglon(): void {
    this.renglones.update((r) => [...r, { productoId: null, cantidad: 1, costoUnitario: 0 }]);
  }

  quitarRenglon(i: number): void {
    this.renglones.update((r) => r.filter((_, k) => k !== i));
  }

  cambiarRenglon(i: number, campo: keyof RenglonOrden, valor: unknown): void {
    this.renglones.update((r) =>
      r.map((x, k) => {
        if (k !== i) return x;
        const nuevo = { ...x, [campo]: valor };
        // Al elegir un producto se propone el costo de su última compra.
        if (campo === 'productoId' && valor && !(Number(x.costoUnitario) > 0)) {
          nuevo.costoUnitario = this.ultimoCosto(Number(valor));
        }
        return nuevo;
      }),
    );
  }

  subtotalDe(r: RenglonOrden): number {
    return this.redondear((Number(r.cantidad) || 0) * (Number(r.costoUnitario) || 0));
  }

  abrirSugerencias(): void {
    this.sucursalId ??= this.opciones()?.sucursales[0]?.id ?? null;
    this.sugerencias.set([]);
    this.elegidas.set(new Set());
    this.dialogo.set('sugerencias');
    this.buscarSugerencias();
  }

  buscarSugerencias(): void {
    if (!this.sucursalId) return;
    this.api
      .suggestions(Number(this.sucursalId))
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (s) => {
          this.sugerencias.set(s);
          this.elegidas.set(new Set(s.map((x) => x.productoId)));
        },
        error: (err) => this.fail(err),
      });
  }

  alternarSugerencia(productoId: number): void {
    this.elegidas.update((actual) => {
      const nuevo = new Set(actual);
      if (nuevo.has(productoId)) nuevo.delete(productoId);
      else nuevo.add(productoId);
      return nuevo;
    });
  }

  aceptarSugerencias(): void {
    const elegidas = this.sugerencias().filter((s) => this.elegidas().has(s.productoId));
    this.renglones.update((actuales) => {
      const conProducto = actuales.filter((r) => r.productoId);
      for (const s of elegidas) {
        const existente = conProducto.find((r) => Number(r.productoId) === s.productoId);
        if (existente) continue;
        conProducto.push({ productoId: s.productoId, cantidad: Number(s.cantidadSugerida), costoUnitario: Number(s.ultimoCosto) });
      }
      return conProducto.length ? conProducto : actuales;
    });
    this.dialogo.set(null);
  }

  guardar(borrador: boolean): void {
    if (this.busy()) return;
    const items = this.renglones().filter((r) => r.productoId);
    if (!this.proveedorId || items.length === 0) {
      this.error.set('Elige un proveedor y al menos un producto.');
      return;
    }
    if (this.problemas().length) {
      this.error.set(this.problemas()[0]);
      return;
    }
    const body: OrdenCompraRequest = {
      proveedorId: Number(this.proveedorId),
      fechaEntrega: this.fechaEntrega || null,
      descuento: Number(this.descuento()) || 0,
      observaciones: this.observaciones.trim() || null,
      borrador,
      items: items.map((r) => ({
        productoId: Number(r.productoId),
        cantidad: Number(r.cantidad),
        costoUnitario: Number(r.costoUnitario),
      })),
    };
    const id = this.editandoId();
    this.ejecutar(id ? this.api.update(id, body) : this.api.create(body), (o) => {
      this.mostrar(o, borrador ? 'Borrador guardado.' : `Orden ${o.numeroOrden} registrada y enviada a aprobación.`);
    });
  }

  // ------------------------------------------------------------- detalle --

  abrir(o: OrdenCompraResumen): void {
    this.limpiarMensajes();
    this.busy.set(true);
    this.api
      .detail(o.id)
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

  solicitarAprobacion(o: OrdenCompra): void {
    this.ejecutar(this.api.requestApproval(o.id), (r) => this.mostrar(r, 'Orden enviada a aprobación.'));
  }

  aprobar(o: OrdenCompra): void {
    this.ejecutar(this.api.approve(o.id), (r) => this.mostrar(r, `Orden ${r.numeroOrden} aprobada.`));
  }

  enviar(o: OrdenCompra): void {
    this.ejecutar(this.api.send(o.id), (r) => this.mostrar(r, 'Orden marcada como enviada al proveedor.'));
  }

  confirmarMotivo(): void {
    const o = this.seleccionada();
    const tipo = this.dialogo();
    if (!o || !this.motivo.trim() || (tipo !== 'rechazar' && tipo !== 'anular')) return;
    const peticion = tipo === 'rechazar' ? this.api.reject(o.id, this.motivo.trim()) : this.api.cancel(o.id, this.motivo.trim());
    this.ejecutar(peticion, (r) => {
      this.dialogo.set(null);
      this.mostrar(r, tipo === 'rechazar' ? 'Orden rechazada: volvió a borrador para corregirla.' : 'Orden anulada.');
    });
  }

  pedirMotivo(tipo: 'rechazar' | 'anular'): void {
    this.motivo = '';
    this.dialogo.set(tipo);
  }

  imprimir(o: OrdenCompra): void {
    imprimirOrdenCompra(o);
  }

  etiqueta(estado: EstadoOrdenCompra): string {
    return this.estados.find((e) => e.valor === estado)?.etiqueta ?? estado;
  }

  modificable(o: OrdenCompra): boolean {
    return o.estado === 'BORRADOR' || o.estado === 'PENDIENTE';
  }

  // --------------------------------------------------------------- apoyo --

  private ultimoCosto(productoId: number): number {
    return Number(this.opciones()?.productos.find((p) => p.id === productoId)?.ultimoCosto) || 0;
  }

  private redondear(n: number): number {
    return Math.round(n * 100) / 100;
  }

  private cargarOpciones(): void {
    this.api
      .options()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (o) => {
          this.opciones.set(o);
          this.sucursalId ??= o.sucursales[0]?.id ?? null;
        },
        error: (err) => this.fail(err),
      });
  }

  private mostrar(o: OrdenCompra, mensaje = ''): void {
    this.seleccionada.set(o);
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
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && this.vista() === 'lista');
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para esta operación sobre órdenes de compra.'
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
