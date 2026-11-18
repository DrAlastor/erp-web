import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { CajaService } from '../../cu32-apertura-de-caja/services/caja.service';
import { CajaTurno, EstadoCaja } from '../../cu32-apertura-de-caja/models/caja.model';
import { VentaPosService } from '../services/venta-pos.service';
import { ClientePos, ComprobantePos, LineaPos, ProductoPos, VentaPosResponse } from '../models/venta-pos.model';
import { agregarProducto, calcularCambio, cambiarCantidad, totalCarrito } from '../utils/pos.utils';

/**
 * CU33 - Venta en Caja (POS).
 * Venta rápida al contado ligada al turno abierto del cajero (CU32): sin turno no se vende
 * y cada cobro queda en el arqueo de ese turno.
 */
@Component({
  selector: 'app-venta-pos',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './venta-pos.component.html',
  styleUrl: './venta-pos.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VentaPosComponent {
  private readonly api = inject(VentaPosService);
  private readonly cajaApi = inject(CajaService);
  private readonly destroy = inject(DestroyRef);
  private readonly busqueda$ = new Subject<string>();
  private readonly busquedaCliente$ = new Subject<string>();

  readonly estadoCaja = signal<EstadoCaja | null>(null);
  readonly turno = signal<CajaTurno | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly ultimaVenta = signal<VentaPosResponse | null>(null);

  readonly productos = signal<ProductoPos[]>([]);
  readonly buscandoProductos = signal(false);
  readonly clientes = signal<ClientePos[]>([]);
  readonly cliente = signal<ClientePos | null>(null);
  readonly carrito = signal<LineaPos[]>([]);
  readonly formaPagoId = signal<number | null>(null);
  readonly montoRecibido = signal<number | null>(null);

  textoProducto = '';
  textoCliente = '';
  tipoComprobante: ComprobantePos = 'NOTA_VENTA';

  readonly sucursalNombre = computed(() => this.turno()?.sucursalNombre ?? '');
  readonly facturaDisponible = computed(() => this.estadoCaja()?.facturaDisponible ?? false);
  /** En caja solo se cobra presencialmente: la pasarela en línea se usa desde Facturación. */
  readonly formasPago = computed(() =>
    (this.estadoCaja()?.formasPago ?? []).filter((f) => f.codigo.toUpperCase() !== 'PASARELA_ONLINE'));
  readonly total = computed(() => totalCarrito(this.carrito()));
  readonly items = computed(() => this.carrito().reduce((acc, l) => acc + l.cantidad, 0));
  readonly esEfectivo = computed(() =>
    this.formasPago().find((f) => f.id === this.formaPagoId())?.codigo.toUpperCase() === 'EFECTIVO');
  readonly cambio = computed(() => calcularCambio(this.total(), this.montoRecibido()));
  readonly puedeCobrar = computed(() => {
    if (!this.turno() || !this.carrito().length || this.total() <= 0 || !this.formaPagoId() || this.busy()) return false;
    return !this.esEfectivo() || this.cambio() >= 0;
  });

  constructor() {
    this.busqueda$.pipe(
      debounceTime(250),
      switchMap((texto) => {
        this.buscandoProductos.set(true);
        return this.api.productos(texto).pipe(catchError(() => of([] as ProductoPos[])));
      }),
      takeUntilDestroyed(this.destroy),
    ).subscribe((lista) => {
      this.productos.set(lista);
      this.buscandoProductos.set(false);
    });

    this.busquedaCliente$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((texto) => texto.trim().length < 2
        ? of([] as ClientePos[])
        : this.api.clientes(texto).pipe(catchError(() => of([] as ClientePos[])))),
      takeUntilDestroyed(this.destroy),
    ).subscribe((lista) => this.clientes.set(lista));

    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    // Sin sucursal, el backend devuelve el turno abierto del cajero autenticado.
    this.cajaApi.estado().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (estado) => {
        this.estadoCaja.set(estado);
        this.turno.set(estado.turno);
        this.tipoComprobante = estado.facturaDisponible ? 'FACTURA' : 'NOTA_VENTA';
        const efectivo = this.formasPago().find((f) => f.codigo.toUpperCase() === 'EFECTIVO');
        this.formaPagoId.set(efectivo?.id ?? this.formasPago()[0]?.id ?? null);
        this.loading.set(false);
        if (estado.turno) this.buscarProductos('');
      },
      error: (err) => this.fail(err),
    });
  }

  buscarProductos(texto: string): void {
    this.busqueda$.next(texto);
  }

  buscarClientes(texto: string): void {
    this.busquedaCliente$.next(texto);
  }

  elegirCliente(c: ClientePos | null): void {
    this.cliente.set(c);
    this.clientes.set([]);
    this.textoCliente = '';
  }

  agregar(p: ProductoPos): void {
    const r = agregarProducto(this.carrito(), p);
    if (!r.ok) {
      this.error.set(r.error);
      return;
    }
    this.error.set('');
    this.carrito.set(r.lineas);
  }

  cambiarCantidad(linea: LineaPos, cantidad: number): void {
    this.carrito.set(cambiarCantidad(this.carrito(), linea.productoId, cantidad));
  }

  quitar(linea: LineaPos): void {
    this.carrito.set(cambiarCantidad(this.carrito(), linea.productoId, 0));
  }

  cobrar(): void {
    if (!this.puedeCobrar()) return;
    this.busy.set(true);
    this.error.set('');
    this.api.vender({
      clienteId: this.cliente()?.id ?? null,
      tipoComprobante: this.tipoComprobante,
      formaPagoId: this.formaPagoId()!,
      montoRecibido: this.esEfectivo() ? this.montoRecibido() : null,
      detalles: this.carrito().map((l) => ({ productoId: l.productoId, cantidad: l.cantidad })),
    }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (venta) => {
        this.busy.set(false);
        this.ultimaVenta.set(venta);
        this.nuevaVenta();
        this.buscarProductos(this.textoProducto); // refresca el stock disponible
      },
      error: (err) => this.fail(err),
    });
  }

  nuevaVenta(): void {
    this.carrito.set([]);
    this.cliente.set(null);
    this.montoRecibido.set(null);
  }

  private fail(err: HttpErrorResponse): void {
    this.loading.set(false);
    this.busy.set(false);
    this.error.set(err.error?.message || err.error?.error || 'No se pudo completar la operación.');
  }
}
