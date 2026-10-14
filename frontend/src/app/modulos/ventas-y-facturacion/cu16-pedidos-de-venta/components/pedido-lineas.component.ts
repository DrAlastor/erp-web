import {
  ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, model, signal, untracked,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PedidosService } from '../services/pedidos.service';
import { LineaPedido, ProductoOpcion } from '../models/pedido.model';
import { mensajeError, sinStockSuficiente, subtotalLinea } from '../utils/pedido.utils';

/** Editor de líneas del pedido: busca productos de la sucursal y edita cantidad/precio/descuento. */
@Component({
  selector: 'app-pedido-lineas',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './pedido-lineas.component.html',
  styleUrls: ['./selector.component.scss', './pedido-lineas.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidoLineasComponent {
  private readonly api = inject(PedidosService);
  private readonly destroy = inject(DestroyRef);

  readonly lineas = model.required<LineaPedido[]>();
  readonly sucursalId = input<number | null>(null);
  readonly resultados = signal<ProductoOpcion[]>([]);
  readonly buscando = signal(false);
  readonly buscado = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly subtotalLinea = subtotalLinea;
  readonly sinStock = sinStockSuficiente;
  busqueda = '';

  constructor() {
    // Al cambiar de sucursal el stock mostrado deja de ser válido: se vuelve a consultar.
    effect(() => {
      const sucursal = this.sucursalId();
      untracked(() => this.refrescarStock(sucursal));
    });
  }

  buscar(): void {
    const sucursal = this.sucursalId();
    if (!sucursal) return;
    this.buscando.set(true);
    this.error.set('');
    this.api
      .productos(this.busqueda.trim(), sucursal)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          this.resultados.set(lista);
          this.buscado.set(true);
          this.buscando.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.buscando.set(false);
          this.error.set(mensajeError(err));
        },
      });
  }

  agregar(producto: ProductoOpcion): void {
    const index = this.lineas().findIndex((linea) => linea.productoId === producto.id);
    if (index >= 0) {
      this.patch(index, { cantidad: this.lineas()[index].cantidad + 1 });
      this.aviso.set(`${producto.nombre} ya estaba en el pedido: se incrementó su cantidad.`);
    } else {
      this.aviso.set('');
      this.lineas.update((lineas) => [
        ...lineas,
        {
          productoId: producto.id,
          codigo: producto.codigo,
          nombre: producto.nombre,
          stock: producto.stockDisponible,
          cantidad: 1,
          precioUnitario: producto.precioVenta,
          descuento: 0,
        },
      ]);
    }
    this.buscado.set(false);
  }

  patch(index: number, cambios: Partial<LineaPedido>): void {
    this.lineas.update((lineas) => lineas.map((linea, i) => (i === index ? { ...linea, ...cambios } : linea)));
  }

  num(value: unknown): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  quitar(index: number): void {
    this.lineas.update((lineas) => lineas.filter((_, i) => i !== index));
  }

  private refrescarStock(sucursal: number | null): void {
    this.resultados.set([]);
    this.buscado.set(false);
    if (!this.lineas().length) return;
    if (!sucursal) {
      this.lineas.update((lineas) => lineas.map((linea) => ({ ...linea, stock: null })));
      return;
    }
    this.api
      .productos('', sucursal)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          const stock = new Map(lista.map((p) => [p.id, p.stockDisponible]));
          this.lineas.update((lineas) => lineas.map((l) => ({ ...l, stock: stock.get(l.productoId) ?? null })));
        },
        error: () => this.lineas.update((lineas) => lineas.map((linea) => ({ ...linea, stock: null }))),
      });
  }
}
