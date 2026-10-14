import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { VentaService } from '../services/venta.service';
import {
  CatalogosVenta,
  ClienteVenta,
  Dosificacion,
  ProductoVenta,
  TipoComprobante,
  TipoVenta,
  Venta,
  VentaPagoRequest,
  VentaRequest,
  VentaResumen,
} from '../models/venta.model';

interface LineaCarrito {
  producto: ProductoVenta;
  cantidad: number;
  descuentoId: number | null;
  descuento: number;
}

interface PagoForm {
  formaPagoId: number | null;
  importe: number | null;
  transaccionExterna: string;
}

/** Venta guardada mientras el cliente paga en Stripe, para retomarla al volver. */
interface BorradorStripe {
  sessionId: string;
  total: number;
  tipoComprobante: TipoComprobante;
  sucursalId: number;
  turnoCajaId: number | null;
  cliente: ClienteVenta;
  carrito: LineaCarrito[];
}

type Pestana = 'nueva' | 'historial' | 'dosificacion';

const PASARELA_ONLINE = 'PASARELA_ONLINE';
const CLAVE_BORRADOR_STRIPE = 'erp_venta_stripe';
const FUNCION_FACTURACION = { modulo: 'ventas-y-facturacion', funcion: 'cu19-registrar-venta-facturacion-y-anulacion' };

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './facturacion.component.html',
  styleUrl: './facturacion.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FacturacionComponent {
  private readonly api = inject(VentaService);
  private readonly destroy = inject(DestroyRef);
  private readonly router = inject(Router);
  /** Resultado del retorno desde Stripe Checkout, pendiente hasta que carguen los catálogos. */
  private retornoStripe: { pago: string; sessionId: string | null } | null = null;

  readonly tab = signal<Pestana>('nueva');
  readonly catalogos = signal<CatalogosVenta | null>(null);
  readonly clientes = signal<ClienteVenta[]>([]);
  readonly productos = signal<ProductoVenta[]>([]);
  readonly ventas = signal<VentaResumen[]>([]);
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly factura = signal<Venta | null>(null);
  readonly anulando = signal(false);
  readonly dosificaciones = signal<Dosificacion[]>([]);
  readonly nuevaDosificacion = signal(false);

  // Formulario de venta
  sucursalId: number | null = null;
  turnoCajaId: number | null = null;
  tipoComprobante: TipoComprobante = 'FACTURA';
  tipoVenta: TipoVenta = 'CONTADO';
  diasCredito = 30;
  clienteBusqueda = '';
  cliente: ClienteVenta | null = null;
  productoBusqueda = '';
  carrito: LineaCarrito[] = [];
  pagos: PagoForm[] = [];

  // Historial
  search = '';
  estado = '';
  comprobanteFiltro = '';
  page = 0;
  motivoAnulacion = '';

  // Dosificación del SIN
  dosAutorizacion = '';
  dosLeyenda = '';
  dosInicial: number | null = 1;
  dosFinal: number | null = null;
  dosFechaLimite = '';

  constructor() {
    const params = inject(ActivatedRoute).snapshot.queryParamMap;
    const pago = params.get('pago');
    if (pago === 'exito' || pago === 'cancelado') {
      this.retornoStripe = { pago, sessionId: params.get('session_id') };
    }
    this.cargarCatalogos();
  }

  cambiarTab(tab: Pestana): void {
    this.tab.set(tab);
    this.error.set('');
    this.success.set('');
    if (tab === 'historial') this.cargarVentas(true);
    if (tab === 'dosificacion') this.cargarDosificaciones();
  }

  /** "Factura N° 1003" o "Nota de venta N° 12". */
  nombreComprobante(v: { tipoComprobante: TipoComprobante; numeroFactura: number }): string {
    return `${v.tipoComprobante === 'NOTA_VENTA' ? 'Nota de venta' : 'Factura'} N° ${v.numeroFactura}`;
  }

  // ---------- Catálogos ----------

  cargarCatalogos(): void {
    this.api
      .catalogos()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (catalogos) => {
          this.catalogos.set(catalogos);
          this.forbidden.set(false);
          if (!this.sucursalId && catalogos.sucursales.length) {
            this.sucursalId = catalogos.sucursales[0].id;
            this.cambiarSucursal();
          }
          if (!this.pagos.length && this.formasManuales().length) {
            this.pagos = [{ formaPagoId: this.formasManuales()[0].id, importe: null, transaccionExterna: '' }];
          }
          if (this.retornoStripe) {
            const retorno = this.retornoStripe;
            this.retornoStripe = null;
            this.retomarCobroStripe(retorno.pago, retorno.sessionId);
          }
        },
        error: (err) => this.fail(err),
      });
  }

  turnosDeSucursal() {
    return (this.catalogos()?.turnosAbiertos ?? []).filter((t) => t.sucursalId === this.sucursalId);
  }

  /** Formas de pago que se registran a mano; la tarjeta en línea solo entra por Stripe. */
  formasManuales() {
    return (this.catalogos()?.formasPago ?? []).filter((f) => f.codigo !== PASARELA_ONLINE);
  }

  cambiarSucursal(): void {
    const turnos = this.turnosDeSucursal();
    this.turnoCajaId = turnos.length ? turnos[0].id : null;
    this.carrito = [];
    this.productos.set([]);
    this.buscarProductos();
  }

  // ---------- Cliente y productos ----------

  buscarClientes(): void {
    this.api
      .clientes(this.clienteBusqueda.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (clientes) => this.clientes.set(clientes),
        error: (err) => this.fail(err),
      });
  }

  elegirCliente(cliente: ClienteVenta): void {
    this.cliente = cliente;
    this.clientes.set([]);
    this.clienteBusqueda = '';
  }

  buscarProductos(): void {
    if (!this.sucursalId) return;
    this.api
      .productos(this.sucursalId, this.productoBusqueda.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (productos) => this.productos.set(productos),
        error: (err) => this.fail(err),
      });
  }

  agregar(producto: ProductoVenta): void {
    const existente = this.carrito.find((l) => l.producto.id === producto.id);
    if (existente) {
      existente.cantidad = Math.min(existente.cantidad + 1, producto.stockDisponible);
      return;
    }
    this.carrito = [...this.carrito, { producto, cantidad: 1, descuentoId: null, descuento: 0 }];
  }

  quitar(linea: LineaCarrito): void {
    this.carrito = this.carrito.filter((l) => l !== linea);
  }

  // ---------- Cálculos (estimados; el backend recalcula con los precios del catálogo) ----------

  bruto(linea: LineaCarrito): number {
    return this.redondear((linea.cantidad || 0) * linea.producto.precioVenta);
  }

  descuentoLinea(linea: LineaCarrito): number {
    const bruto = this.bruto(linea);
    if (linea.descuentoId) {
      const promo = this.catalogos()?.descuentos.find((d) => d.id === linea.descuentoId);
      if (!promo) return 0;
      const valor = promo.tipo === 'PORCENTAJE' ? (bruto * promo.valor) / 100 : promo.valor;
      return this.redondear(Math.min(valor, bruto));
    }
    return this.redondear(Math.min(linea.descuento || 0, bruto));
  }

  subtotal(): number {
    return this.redondear(this.carrito.reduce((acc, l) => acc + this.bruto(l), 0));
  }

  descuentoTotal(): number {
    return this.redondear(this.carrito.reduce((acc, l) => acc + this.descuentoLinea(l), 0));
  }

  totalVenta(): number {
    return this.redondear(this.subtotal() - this.descuentoTotal());
  }

  /** La nota de venta no genera IVA ni IT; la factura usa las tasas de la tabla impuestos. */
  iva(): number {
    if (this.tipoComprobante === 'NOTA_VENTA') return 0;
    return this.redondear((this.totalVenta() * (this.catalogos()?.tasaIva ?? 0)) / 100);
  }

  it(): number {
    if (this.tipoComprobante === 'NOTA_VENTA') return 0;
    return this.redondear((this.totalVenta() * (this.catalogos()?.tasaIt ?? 0)) / 100);
  }

  totalPagado(): number {
    return this.redondear(this.pagos.reduce((acc, p) => acc + (Number(p.importe) || 0), 0));
  }

  pendiente(): number {
    return this.redondear(this.totalVenta() - this.totalPagado());
  }

  agregarPago(): void {
    const forma = this.formasManuales()[0];
    this.pagos = [
      ...this.pagos,
      { formaPagoId: forma?.id ?? null, importe: this.pendiente() > 0 ? this.pendiente() : null, transaccionExterna: '' },
    ];
  }

  quitarPago(pago: PagoForm): void {
    this.pagos = this.pagos.filter((p) => p !== pago);
  }

  completarSaldo(pago: PagoForm): void {
    pago.importe = this.redondear((Number(pago.importe) || 0) + this.pendiente());
  }

  /** Venta completa (cliente, productos y stock), sin mirar todavía cómo se paga. */
  private ventaLista(): boolean {
    if (this.busy() || !this.catalogos()) return false;
    // Solo la factura necesita una dosificación del SIN vigente.
    if (this.tipoComprobante === 'FACTURA' && !this.catalogos()!.dosificacion) return false;
    if (!this.sucursalId || !this.cliente || !this.carrito.length || this.totalVenta() <= 0) return false;
    return !this.carrito.some((l) => !l.cantidad || l.cantidad <= 0 || l.cantidad > l.producto.stockDisponible);
  }

  puedeCobrarConTarjeta(): boolean {
    return !!this.catalogos()?.cobroEnLinea && this.tipoVenta === 'CONTADO' && this.ventaLista();
  }

  puedeEmitir(): boolean {
    if (!this.ventaLista()) return false;
    if (this.tipoVenta === 'CONTADO') {
      return this.pagos.length > 0 && this.pagos.every((p) => p.formaPagoId && Number(p.importe) > 0)
        && Math.abs(this.pendiente()) < 0.005;
    }
    return this.diasCredito >= 1 && this.diasCredito <= 365;
  }

  // ---------- Emisión ----------

  emitir(): void {
    if (!this.puedeEmitir()) return;
    const pagos =
      this.tipoVenta === 'CONTADO'
        ? this.pagos.map((p) => ({
            formaPagoId: p.formaPagoId!,
            importe: Number(p.importe),
            transaccionExterna: p.transaccionExterna.trim() || null,
          }))
        : [];
    this.enviarVenta(this.armarVenta(pagos));
  }

  limpiarVenta(): void {
    this.cliente = null;
    this.carrito = [];
    this.tipoComprobante = this.catalogos()?.dosificacion ? 'FACTURA' : 'NOTA_VENTA';
    this.tipoVenta = 'CONTADO';
    this.diasCredito = 30;
    const forma = this.formasManuales()[0];
    this.pagos = forma ? [{ formaPagoId: forma.id, importe: null, transaccionExterna: '' }] : [];
  }

  private armarVenta(pagos: VentaPagoRequest[]): VentaRequest {
    return {
      sucursalId: this.sucursalId!,
      clienteId: this.cliente!.id,
      turnoCajaId: this.turnoCajaId,
      tipoVenta: this.tipoVenta,
      tipoComprobante: this.tipoComprobante,
      diasCredito: this.tipoVenta === 'CREDITO' ? this.diasCredito : null,
      detalles: this.carrito.map((l) => ({
        productoId: l.producto.id,
        cantidad: l.cantidad,
        descuentoId: l.descuentoId,
        descuento: l.descuentoId ? null : l.descuento || 0,
      })),
      pagos,
    };
  }

  private enviarVenta(body: VentaRequest, mensajeExito = 'emitida correctamente.'): void {
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .registrar(body)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (venta) => {
          this.busy.set(false);
          this.factura.set(venta);
          this.success.set(`${this.nombreComprobante(venta)} ${mensajeExito}`);
          this.limpiarVenta();
          this.cargarCatalogos();
          this.buscarProductos();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  // ---------- Cobro con tarjeta (Stripe Checkout) ----------

  /**
   * Valida la venta en el servidor, guarda el borrador en este navegador y redirige a la
   * página de pago de Stripe. La factura se emite al volver, cuando el pago está confirmado.
   */
  cobrarConTarjeta(): void {
    if (!this.puedeCobrarConTarjeta()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    const base = `${window.location.origin}/app?modulo=${FUNCION_FACTURACION.modulo}&funcion=${FUNCION_FACTURACION.funcion}`;
    this.api
      .iniciarCobroStripe({
        venta: this.armarVenta([]),
        successUrl: `${base}&pago=exito&session_id={CHECKOUT_SESSION_ID}`,
        cancelUrl: `${base}&pago=cancelado`,
      })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (cobro) => {
          const borrador: BorradorStripe = {
            sessionId: cobro.sessionId,
            total: cobro.total,
            tipoComprobante: this.tipoComprobante,
            sucursalId: this.sucursalId!,
            turnoCajaId: this.turnoCajaId,
            cliente: this.cliente!,
            carrito: this.carrito,
          };
          if (!this.guardarBorrador(borrador)) {
            this.busy.set(false);
            this.error.set('El navegador no permite guardar la venta mientras se paga. Habilita el almacenamiento del sitio.');
            return;
          }
          window.location.assign(cobro.checkoutUrl);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  /** Al volver de Stripe: restaura la venta y, si el pago se completó, emite la factura. */
  private retomarCobroStripe(pago: string, sessionId: string | null): void {
    const borrador = this.leerBorrador();
    this.borrarBorrador();
    this.router.navigate([], {
      queryParams: { ...FUNCION_FACTURACION },
      replaceUrl: true,
    });
    if (!borrador) {
      if (pago === 'exito') {
        this.error.set(
          'Volviste de Stripe pero esta pestaña no tiene la venta guardada. Si el pago se completó, emite la factura desde la pestaña donde iniciaste el cobro.',
        );
      }
      return;
    }

    this.tipoVenta = 'CONTADO';
    this.tipoComprobante = borrador.tipoComprobante ?? 'FACTURA';
    this.sucursalId = borrador.sucursalId;
    this.turnoCajaId = borrador.turnoCajaId;
    this.cliente = borrador.cliente;
    this.carrito = borrador.carrito;
    this.buscarProductos();

    if (pago !== 'exito' || sessionId !== borrador.sessionId) {
      this.error.set('El pago con tarjeta fue cancelado. La venta sigue en pantalla para cobrarla de otra forma.');
      return;
    }
    const pasarela = this.catalogos()?.formasPago.find((f) => f.codigo === PASARELA_ONLINE);
    if (!pasarela) {
      this.error.set('La forma de pago en línea ya no está activa; no se pudo emitir la factura.');
      return;
    }
    // El importe es el total que calculó el servidor al abrir el cobro; el backend lo vuelve
    // a verificar contra Stripe antes de facturar.
    this.enviarVenta(
      this.armarVenta([{ formaPagoId: pasarela.id, importe: borrador.total, transaccionExterna: sessionId }]),
      'emitida: pago con tarjeta confirmado en Stripe.',
    );
  }

  private guardarBorrador(borrador: BorradorStripe): boolean {
    try {
      sessionStorage.setItem(CLAVE_BORRADOR_STRIPE, JSON.stringify(borrador));
      return true;
    } catch {
      return false;
    }
  }

  private leerBorrador(): BorradorStripe | null {
    try {
      const guardado = sessionStorage.getItem(CLAVE_BORRADOR_STRIPE);
      return guardado ? (JSON.parse(guardado) as BorradorStripe) : null;
    } catch {
      return null;
    }
  }

  private borrarBorrador(): void {
    try {
      sessionStorage.removeItem(CLAVE_BORRADOR_STRIPE);
    } catch {
      /* Almacenamiento no disponible. */
    }
  }

  // ---------- Historial y anulación ----------

  cargarVentas(reset = false): void {
    if (reset) this.page = 0;
    this.loading.set(true);
    this.error.set('');
    this.api
      .listar(this.search.trim(), this.estado, this.comprobanteFiltro, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          this.ventas.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  navegar(delta: number): void {
    this.page += delta;
    this.cargarVentas();
  }

  ver(venta: VentaResumen): void {
    this.busy.set(true);
    this.api
      .detalle(venta.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (detalle) => {
          this.busy.set(false);
          this.anulando.set(false);
          this.motivoAnulacion = '';
          this.factura.set(detalle);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  anular(): void {
    const venta = this.factura();
    if (!venta || this.busy() || !this.motivoAnulacion.trim()) return;
    this.busy.set(true);
    this.error.set('');
    this.api
      .anular(venta.id, this.motivoAnulacion.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (anulada) => {
          this.busy.set(false);
          this.anulando.set(false);
          this.factura.set(anulada);
          this.success.set(
            `${this.nombreComprobante(anulada)} anulada. Stock restituido y asiento ${anulada.asientoContable} registrado.`,
          );
          this.cargarVentas();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  cerrarFactura(): void {
    this.factura.set(null);
    this.anulando.set(false);
  }

  // ---------- Dosificación del SIN ----------

  cargarDosificaciones(): void {
    this.loading.set(true);
    this.api
      .dosificaciones()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          this.loading.set(false);
          this.dosificaciones.set(lista);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  registrarDosificacion(): void {
    if (this.busy() || !this.dosInicial || !this.dosFinal) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .registrarDosificacion({
        numeroAutorizacion: this.dosAutorizacion.trim(),
        leyendaSin: this.dosLeyenda.trim(),
        numeroInicial: this.dosInicial,
        numeroFinal: this.dosFinal,
        fechaLimite: this.dosFechaLimite,
      })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (dosificacion) => {
          this.busy.set(false);
          this.nuevaDosificacion.set(false);
          this.dosAutorizacion = '';
          this.dosLeyenda = '';
          this.dosInicial = 1;
          this.dosFinal = null;
          this.dosFechaLimite = '';
          this.success.set(`Dosificación ${dosificacion.numeroAutorizacion} registrada y activada.`);
          this.cargarDosificaciones();
          this.cargarCatalogos();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  cambiarEstadoDosificacion(dosificacion: Dosificacion): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .cambiarEstadoDosificacion(dosificacion.id, !dosificacion.activo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (actualizada) => {
          this.busy.set(false);
          this.success.set(
            `Dosificación ${actualizada.numeroAutorizacion} ${actualizada.activo ? 'activada' : 'desactivada'}.`,
          );
          this.cargarDosificaciones();
          this.cargarCatalogos();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  private redondear(valor: number): number {
    return Math.round((valor + Number.EPSILON) * 100) / 100;
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403);
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para gestionar la facturación.'
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
