import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ActualizarDescuentoRequest,
  ActualizarListaPrecioRequest,
  ActualizarPreciosMatrizRequest,
  CrearDescuentoRequest,
  CrearListaPrecioRequest,
  Descuento,
  ItemPrecioProducto,
  ListaPrecio,
  PreciosListasService
} from '../services/precios-listas.service';

interface ItemMatrizEditable extends ItemPrecioProducto {
  nuevoPrecio: number;
  esInvalido: boolean;
}

@Component({
  selector: 'app-precios-y-listas',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './precios-y-listas.component.html',
  styleUrl: './precios-y-listas.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreciosYListasComponent {
  private readonly api = inject(PreciosListasService);
  private readonly destroy = inject(DestroyRef);

  readonly activeTab = signal<'listas' | 'descuentos'>('listas');
  readonly listas = signal<ListaPrecio[]>([]);
  readonly descuentos = signal<Descuento[]>([]);
  readonly matrizProductos = signal<ItemMatrizEditable[]>([]);

  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  // Modales
  readonly modalListaOpen = signal(false);
  readonly selectedLista = signal<ListaPrecio | null>(null);

  readonly modalMatrizOpen = signal(false);
  readonly listaParaMatriz = signal<ListaPrecio | null>(null);
  filtroMatriz = '';

  readonly modalDescuentoOpen = signal(false);
  readonly selectedDescuento = signal<Descuento | null>(null);

  readonly statusConfirmLista = signal<ListaPrecio | null>(null);
  readonly deleteConfirmDescuento = signal<Descuento | null>(null);

  // Formulario Lista
  listaCodigo = '';
  listaNombre = '';
  listaDescripcion = '';
  listaEsPrincipal = false;

  // Formulario Descuento
  descNombre = '';
  descTipo: 'PORCENTAJE' | 'MONTO_FIJO' = 'PORCENTAJE';
  descValor: number = 0;
  descFechaInicio = '';
  descFechaFin = '';

  constructor() {
    this.cargarDatos();
  }

  setTab(tab: 'listas' | 'descuentos'): void {
    this.activeTab.set(tab);
    this.error.set('');
    this.success.set('');
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set('');

    this.api.listarListas()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (data) => {
          this.listas.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        }
      });

    this.api.listarDescuentos()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (data) => {
          this.descuentos.set(data);
        },
        error: () => {}
      });
  }

  // -------------------------------------------------------------------------
  // OPERACIONES: LISTAS DE PRECIOS
  // -------------------------------------------------------------------------
  openNuevaLista(): void {
    this.error.set('');
    this.success.set('');
    this.selectedLista.set(null);
    this.listaCodigo = '';
    this.listaNombre = '';
    this.listaDescripcion = '';
    this.listaEsPrincipal = false;
    this.modalListaOpen.set(true);
  }

  openEditarLista(l: ListaPrecio): void {
    this.error.set('');
    this.success.set('');
    this.selectedLista.set(l);
    this.listaCodigo = l.codigo;
    this.listaNombre = l.nombre;
    this.listaDescripcion = l.descripcion ?? '';
    this.listaEsPrincipal = l.esPrincipal;
    this.modalListaOpen.set(true);
  }

  guardarLista(): void {
    if (this.busy()) return;
    if (!this.listaNombre.trim()) {
      this.error.set('El nombre de la lista de precios es obligatorio.');
      return;
    }

    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    const sel = this.selectedLista();
    if (sel) {
      const body: ActualizarListaPrecioRequest = {
        nombre: this.listaNombre.trim(),
        descripcion: this.listaDescripcion.trim() || undefined,
        esPrincipal: this.listaEsPrincipal,
      };
      this.api.actualizarLista(sel.id, body)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({
          next: () => {
            this.busy.set(false);
            this.modalListaOpen.set(false);
            this.success.set('Lista de precios actualizada correctamente.');
            this.cargarDatos();
          },
          error: (err) => {
            this.busy.set(false);
            this.fail(err);
          }
        });
    } else {
      if (!this.listaCodigo.trim()) {
        this.busy.set(false);
        this.error.set('El código de la lista es obligatorio.');
        return;
      }
      const body: CrearListaPrecioRequest = {
        codigo: this.listaCodigo.trim().toUpperCase(),
        nombre: this.listaNombre.trim(),
        descripcion: this.listaDescripcion.trim() || undefined,
        esPrincipal: this.listaEsPrincipal,
      };
      this.api.crearLista(body)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({
          next: () => {
            this.busy.set(false);
            this.modalListaOpen.set(false);
            this.success.set('Nueva lista de precios creada exitosamente.');
            this.cargarDatos();
          },
          error: (err) => {
            this.busy.set(false);
            this.fail(err);
          }
        });
    }
  }

  confirmarEstadoLista(l: ListaPrecio): void {
    this.error.set('');
    this.success.set('');
    this.statusConfirmLista.set(l);
  }

  ejecutarCambioEstadoLista(): void {
    const l = this.statusConfirmLista();
    if (!l || this.busy()) return;
    this.busy.set(true);
    this.error.set('');

    this.api.cambiarEstadoLista(l.id, !l.activo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.statusConfirmLista.set(null);
          this.success.set(`La lista '${l.nombre}' ahora está ${!l.activo ? 'ACTIVA' : 'INACTIVA'}.`);
          this.cargarDatos();
        },
        error: (err) => {
          this.busy.set(false);
          this.statusConfirmLista.set(null);
          this.fail(err);
        }
      });
  }

  // -------------------------------------------------------------------------
  // OPERACIONES: MATRIZ DE PRECIOS POR PRODUCTO (RN19)
  // -------------------------------------------------------------------------
  openMatriz(l: ListaPrecio): void {
    this.error.set('');
    this.success.set('');
    this.listaParaMatriz.set(l);
    this.filtroMatriz = '';
    this.busy.set(true);

    this.api.obtenerMatrizProductos(l.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (items) => {
          const editables: ItemMatrizEditable[] = items.map(it => ({
            ...it,
            nuevoPrecio: it.precioAsignado,
            esInvalido: it.precioAsignado < it.costoCompra
          }));
          this.matrizProductos.set(editables);
          this.modalMatrizOpen.set(true);
          this.busy.set(false);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        }
      });
  }

  onPrecioChange(item: ItemMatrizEditable): void {
    const p = Number(item.nuevoPrecio) || 0;
    item.esInvalido = item.costoCompra > 0 && p < item.costoCompra;
    if (item.costoCompra > 0) {
      item.margenCalculadoPorc = Number((((p - item.costoCompra) / item.costoCompra) * 100).toFixed(2));
    } else {
      item.margenCalculadoPorc = 0;
    }
  }

  aplicarPreciosCatalogo(): void {
    const actualizados = this.matrizProductos().map(item => {
      const p = item.precioCatalogo;
      const invalido = item.costoCompra > 0 && p < item.costoCompra;
      const margen = item.costoCompra > 0
        ? Number((((p - item.costoCompra) / item.costoCompra) * 100).toFixed(2))
        : 0;
      return {
        ...item,
        nuevoPrecio: p,
        margenCalculadoPorc: margen,
        esInvalido: invalido
      };
    });
    this.matrizProductos.set(actualizados);
  }

  guardarMatrizPrecios(): void {
    const l = this.listaParaMatriz();
    if (!l || this.busy()) return;

    const items = this.matrizProductos();

    // Validar Regla RN19
    const violaciones = items.filter(it => it.esInvalido);
    if (violaciones.length > 0) {
      this.error.set(`Regla RN19: Existen ${violaciones.length} producto(s) con precio inferior al costo de compra (${violaciones[0].nombre}). Corrige las tarifas antes de guardar.`);
      return;
    }

    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    const body: ActualizarPreciosMatrizRequest = {
      items: items.map(it => ({
        productoId: it.productoId,
        precio: Number(it.nuevoPrecio)
      }))
    };

    this.api.actualizarPreciosMatriz(l.id, body)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.modalMatrizOpen.set(false);
          this.success.set(`Tarifas de la lista '${l.nombre}' actualizadas exitosamente (RN19 validada).`);
          this.cargarDatos();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        }
      });
  }

  get matrizFiltrada(): ItemMatrizEditable[] {
    const q = this.filtroMatriz.trim().toLowerCase();
    if (!q) return this.matrizProductos();
    return this.matrizProductos().filter(it =>
      it.nombre.toLowerCase().includes(q) ||
      it.codigoSku.toLowerCase().includes(q) ||
      it.categoriaNombre.toLowerCase().includes(q)
    );
  }

  // -------------------------------------------------------------------------
  // OPERACIONES: DESCUENTOS Y PROMOCIONES
  // -------------------------------------------------------------------------
  openNuevoDescuento(): void {
    this.error.set('');
    this.success.set('');
    this.selectedDescuento.set(null);
    this.descNombre = '';
    this.descTipo = 'PORCENTAJE';
    this.descValor = 5;
    this.descFechaInicio = '';
    this.descFechaFin = '';
    this.modalDescuentoOpen.set(true);
  }

  openEditarDescuento(d: Descuento): void {
    this.error.set('');
    this.success.set('');
    this.selectedDescuento.set(d);
    this.descNombre = d.nombre;
    this.descTipo = d.tipo;
    this.descValor = d.valor;
    this.descFechaInicio = d.fechaInicio ?? '';
    this.descFechaFin = d.fechaFin ?? '';
    this.modalDescuentoOpen.set(true);
  }

  guardarDescuento(): void {
    if (this.busy()) return;
    if (!this.descNombre.trim()) {
      this.error.set('El nombre de la promoción o descuento es obligatorio.');
      return;
    }
    if (!this.descValor || this.descValor <= 0) {
      this.error.set('El valor del descuento debe ser mayor a 0.');
      return;
    }

    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    const sel = this.selectedDescuento();
    if (sel) {
      const body: ActualizarDescuentoRequest = {
        nombre: this.descNombre.trim(),
        tipo: this.descTipo,
        valor: Number(this.descValor),
        fechaInicio: this.descFechaInicio || undefined,
        fechaFin: this.descFechaFin || undefined
      };
      this.api.actualizarDescuento(sel.id, body)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({
          next: () => {
            this.busy.set(false);
            this.modalDescuentoOpen.set(false);
            this.success.set('Regla de descuento actualizada correctamente.');
            this.cargarDatos();
          },
          error: (err) => {
            this.busy.set(false);
            this.fail(err);
          }
        });
    } else {
      const body: CrearDescuentoRequest = {
        nombre: this.descNombre.trim(),
        tipo: this.descTipo,
        valor: Number(this.descValor),
        fechaInicio: this.descFechaInicio || undefined,
        fechaFin: this.descFechaFin || undefined
      };
      this.api.crearDescuento(body)
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({
          next: () => {
            this.busy.set(false);
            this.modalDescuentoOpen.set(false);
            this.success.set('Nueva regla de descuento registrada correctamente.');
            this.cargarDatos();
          },
          error: (err) => {
            this.busy.set(false);
            this.fail(err);
          }
        });
    }
  }

  confirmarBorradoDescuento(d: Descuento): void {
    this.error.set('');
    this.success.set('');
    this.deleteConfirmDescuento.set(d);
  }

  ejecutarBorradoDescuento(): void {
    const d = this.deleteConfirmDescuento();
    if (!d || this.busy()) return;
    this.busy.set(true);
    this.error.set('');

    this.api.eliminarDescuento(d.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.deleteConfirmDescuento.set(null);
          this.success.set(`Promoción '${d.nombre}' eliminada correctamente.`);
          this.cargarDatos();
        },
        error: (err) => {
          this.busy.set(false);
          this.deleteConfirmDescuento.set(null);
          this.fail(err);
        }
      });
  }

  cambiarEstadoDescuentoDirecto(d: Descuento): void {
    this.busy.set(true);
    this.error.set('');
    this.api.cambiarEstadoDescuento(d.id, !d.activo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.cargarDatos();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        }
      });
  }

  closeModal(): void {
    this.modalListaOpen.set(false);
    this.modalMatrizOpen.set(false);
    this.modalDescuentoOpen.set(false);
  }

  private fail(err: HttpErrorResponse): void {
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para gestionar tarifas o descuentos.'
        : err.status === 400
          ? err.error?.message || 'Los datos de la tarifa no cumplen las reglas de negocio (RN19).'
          : err.status === 409
            ? err.error?.message || 'Conflicto: El código ya existe o hay dependencia activa.'
            : err.error?.message || 'Ocurrió un error al procesar la solicitud.'
    );
  }
}
