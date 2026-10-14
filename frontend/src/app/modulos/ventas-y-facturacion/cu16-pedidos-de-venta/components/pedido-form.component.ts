import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, input, output, signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PedidosService } from '../services/pedidos.service';
import { ClienteOpcion, LineaPedido, Pedido, PedidoRequest, SucursalOpcion } from '../models/pedido.model';
import { mensajeError, sinStockSuficiente, subtotalPedido, validarPedido } from '../utils/pedido.utils';
import { ClienteSelectorComponent } from './cliente-selector.component';
import { PedidoLineasComponent } from './pedido-lineas.component';

function hoy(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Alta y edición (solo PENDIENTE) de un pedido de venta. */
@Component({
  selector: 'app-pedido-form',
  standalone: true,
  imports: [FormsModule, DecimalPipe, ClienteSelectorComponent, PedidoLineasComponent],
  templateUrl: './pedido-form.component.html',
  styleUrl: './pedido-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidoFormComponent implements OnInit {
  private readonly api = inject(PedidosService);
  private readonly destroy = inject(DestroyRef);

  readonly pedido = input<Pedido | null>(null);
  readonly saved = output<Pedido>();
  readonly closed = output<void>();

  readonly sucursales = signal<SucursalOpcion[]>([]);
  readonly cliente = signal<ClienteOpcion | null>(null);
  readonly sucursalId = signal<number | null>(null);
  readonly lineas = signal<LineaPedido[]>([]);
  readonly descuento = signal(0);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly intentado = signal(false);

  readonly subtotal = computed(() => subtotalPedido(this.lineas()));
  readonly total = computed(() => Math.round((this.subtotal() - this.descuento()) * 100) / 100);
  readonly sinStock = computed(() => this.lineas().some(sinStockSuficiente));
  readonly validacion = computed(() =>
    validarPedido(
      { clienteId: this.cliente()?.id, sucursalId: this.sucursalId() ?? undefined, descuento: this.descuento() },
      this.lineas(),
    ),
  );

  fecha = hoy();
  observaciones = '';

  constructor() {
    this.api
      .sucursales()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          this.sucursales.set(lista);
          if (!this.sucursalId() && lista.length === 1) this.sucursalId.set(lista[0].id);
        },
        error: (err: HttpErrorResponse) => this.error.set(mensajeError(err)),
      });
  }

  ngOnInit(): void {
    const pedido = this.pedido();
    if (!pedido) return;
    this.cliente.set({ id: pedido.clienteId, razonSocial: pedido.clienteRazonSocial, nitCi: pedido.clienteNitCi });
    this.sucursalId.set(pedido.sucursalId);
    this.fecha = pedido.fecha;
    this.observaciones = pedido.observaciones ?? '';
    this.descuento.set(pedido.descuento ?? 0);
    this.lineas.set(
      pedido.detalles.map((d) => ({
        productoId: d.productoId,
        codigo: d.productoCodigo,
        nombre: d.productoNombre,
        stock: null,
        cantidad: d.cantidad,
        precioUnitario: d.precioUnitario,
        descuento: d.descuento ?? 0,
      })),
    );
  }

  onSucursal(value: string | number | null): void {
    this.sucursalId.set(value ? Number(value) : null);
  }

  onDescuento(value: unknown): void {
    const parsed = Number(value);
    this.descuento.set(Number.isFinite(parsed) ? parsed : 0);
  }

  save(): void {
    if (this.busy()) return;
    this.intentado.set(true);
    this.error.set('');
    if (this.validacion()) return;
    const body: PedidoRequest = {
      clienteId: this.cliente()!.id,
      sucursalId: this.sucursalId()!,
      fecha: this.fecha || undefined,
      observaciones: this.observaciones.trim() || undefined,
      descuento: this.descuento(),
      detalles: this.lineas().map((l) => ({
        productoId: l.productoId,
        cantidad: l.cantidad,
        precioUnitario: l.precioUnitario,
        descuento: l.descuento,
      })),
    };
    const actual = this.pedido();
    const request = actual ? this.api.update(actual.id, body) : this.api.create(body);
    this.busy.set(true);
    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (pedido) => {
        this.busy.set(false);
        this.saved.emit(pedido);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
