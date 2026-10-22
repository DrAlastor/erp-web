import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClientesService } from '../../cu13-gestionar-clientes/services/clientes.service';
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
import { NotificacionesService } from '../../../../core/services/notificaciones.service';

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
  montoEntregado?: number | null;
  confirmado?: boolean;
}

/** Venta guardada mientras el cliente paga en Stripe, para retomarla al volver. */
interface BorradorStripe {
  sessionId: string;
  total: number;
  tipoComprobante: TipoComprobante;
  sucursalId: number;
  turnoCajaId: number | null;
  cotizacionId?: number | null;
  cliente: ClienteVenta;
  carrito: LineaCarrito[];
}

type Pestana = 'nueva' | 'historial' | 'dosificacion';

const PASARELA_ONLINE = 'PASARELA_ONLINE';
const CLAVE_BORRADOR_STRIPE = 'erp_venta_stripe';

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
  private readonly clientesService = inject(ClientesService);
  private readonly notificacionesService = inject(NotificacionesService);
  private readonly destroy = inject(DestroyRef);
  private readonly router = inject(Router);
  readonly cdr = inject(ChangeDetectorRef);
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

  // Modal para registrar cliente rápido
  readonly modalNuevoCliente = signal(false);
  readonly guardandoCliente = signal(false);
  nuevoClienteRazonSocial = '';
  nuevoClienteNitCi = '';
  nuevoClienteTelefono = '';
  nuevoClienteEmail = '';

  // Modal para pago QR
  readonly modalQr = signal(false);
  pagoQrSeleccionado: PagoForm | null = null;
  readonly modoQr = signal<'generar' | 'subir'>('generar');
  readonly bancoQr = signal('Banco Nacional de Bolivia (BNB)');
  readonly bancosDisponibles = [
    'Banco Nacional de Bolivia (BNB)',
    'Banco de Crédito (BCP)',
    'Banco Unión',
    'Banco Mercantil Santa Cruz',
    'Banco FIE',
    'Yape Bolivia',
  ];
  readonly cuentaQr = signal('150-109283-4');
  readonly titularQr = signal('ASUS Tech Bolivia S.R.L.');
  readonly imagenQrCustom = signal<string | null>(localStorage.getItem('erp_qr_custom_image'));

  // Formulario de venta
  sucursalId: number | null = null;
  turnoCajaId: number | null = null;
  tipoComprobante: TipoComprobante = 'FACTURA';
  tipoVenta: TipoVenta = 'CONTADO';
  diasCredito = 30;
  clienteBusqueda = '';
  readonly cliente = signal<ClienteVenta | null>(null);
  productoBusqueda = '';
  carrito: LineaCarrito[] = [];
  pagos: PagoForm[] = [];
  cotizacionSeleccionadaId: number | null = null;

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
          this.buscarClientes();
          this.cargarVentas(true);
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

  /** Formas de pago disponibles para registrar el cobro */
  formasManuales() {
    return (this.catalogos()?.formasPago ?? []).filter(
      (f) => f.codigo !== 'TRANSFERENCIA'
    );
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
        next: (clientes) => {
          this.clientes.set(clientes);
          if (!this.cliente() && clientes.length > 0) {
            this.cliente.set(clientes[0]);
          }
          this.cdr.markForCheck();
        },
        error: (err) => this.fail(err),
      });
  }

  elegirCliente(cliente: ClienteVenta): void {
    this.cliente.set(cliente);
    this.clientes.set([]);
    this.clienteBusqueda = '';
    this.cdr.markForCheck();
  }

  seleccionarClientePasante(): void {
    const pasante = this.clientes().find(c => c.nitCi === '0' || c.razonSocial.toLowerCase().includes('sin nombre') || c.razonSocial.toLowerCase().includes('pasante'));
    if (pasante) {
      this.cliente.set(pasante);
      this.clienteBusqueda = '';
      this.cdr.markForCheck();
      return;
    }
    this.api.clientes('0').pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (res) => {
        const found = res.find(c => c.nitCi === '0') || (res.length > 0 ? res[0] : null);
        this.cliente.set(found || { id: 11, razonSocial: 'Cliente Pasante / Sin Nombre', nitCi: '0' });
        this.clienteBusqueda = '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.cliente.set({ id: 11, razonSocial: 'Cliente Pasante / Sin Nombre', nitCi: '0' });
        this.clienteBusqueda = '';
        this.cdr.markForCheck();
      }
    });
  }

  seleccionarCotizacion(cotId: number | null): void {
    if (!cotId) {
      this.cotizacionSeleccionadaId = null;
      this.cdr.markForCheck();
      return;
    }
    const cot = this.catalogos()?.cotizacionesPendientes?.find((c) => c.id === cotId);
    if (!cot) return;
    this.cotizacionSeleccionadaId = cot.id;
    this.cliente.set({
      id: cot.clienteId,
      razonSocial: cot.clienteRazonSocial || 'Cliente',
      nitCi: cot.clienteNitCi || '0'
    });

    const items = cot.lineas || cot.detalles || [];
    // Cargar productos de la cotización al carrito
    this.carrito = items.map((d: any) => ({
      producto: {
        id: d.productoId || d.articuloId || 1,
        codigo: d.codigo || d.sku || ('PROD-' + (d.productoId || d.articuloId || 1)),
        nombre: d.nombre || d.articuloNombre || 'Producto',
        precioVenta: Number(d.precioUnitario || 0),
        stockDisponible: 999999
      },
      cantidad: Number(d.cantidad || 1),
      descuentoId: null,
      descuento: Number(d.descuento || 0)
    }));

    // Sincronizar con los productos de la sucursal actual si están cargados
    for (const item of this.carrito) {
      const prodReal = this.productos().find(p => p.id === item.producto.id);
      if (prodReal) {
        item.producto.stockDisponible = prodReal.stockDisponible;
        item.producto.nombre = prodReal.nombre;
        item.producto.codigo = prodReal.codigo;
      }
    }

    if (this.pagos.length > 0) {
      this.pagos[0].importe = this.totalVenta();
      if (this.esEfectivo(this.pagos[0])) {
        this.pagos[0].montoEntregado = this.pagos[0].importe;
      }
    }

    const nroCot = cot.numeroCotizacion || cot.numero || ('COT-' + cot.id);
    this.success.set(`Cotización ${nroCot} vinculada (${items.length} ítems) para el cliente ${cot.clienteRazonSocial}.`);
    this.cdr.markForCheck();
  }

  abrirModalNuevoCliente(): void {
    this.nuevoClienteRazonSocial = '';
    this.nuevoClienteNitCi = '';
    this.nuevoClienteTelefono = '';
    this.nuevoClienteEmail = '';
    this.modalNuevoCliente.set(true);
  }

  cerrarModalNuevoCliente(): void {
    this.modalNuevoCliente.set(false);
  }

  guardarNuevoCliente(): void {
    const razon = this.nuevoClienteRazonSocial.trim();
    const nit = this.nuevoClienteNitCi.trim();
    if (!razon || !nit || this.guardandoCliente()) return;

    this.guardandoCliente.set(true);
    this.clientesService.create({
      razonSocial: razon,
      nitCi: nit,
      telefono: this.nuevoClienteTelefono.trim() || undefined,
      email: this.nuevoClienteEmail.trim() || undefined,
      condicionPago: 'CONTADO',
      limiteCredito: 0,
      diasCredito: 0
    }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (nuevo) => {
        this.guardandoCliente.set(false);
        this.modalNuevoCliente.set(false);
        this.cliente.set({
          id: nuevo.id,
          razonSocial: nuevo.razonSocial,
          nitCi: nuevo.nitCi
        });
        this.success.set(`Cliente "${nuevo.razonSocial}" (NIT: ${nuevo.nitCi}) registrado y asignado.`);
        this.buscarClientes();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.guardandoCliente.set(false);
        this.fail(err);
      }
    });
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

  /** Calcula el stock en tiempo real descontando lo añadido al carrito */
  stockDisponibleReal(producto: ProductoVenta): number {
    const enCarrito = this.carrito.find((l) => l.producto.id === producto.id)?.cantidad || 0;
    return Math.max(0, (producto.stockDisponible || 0) - enCarrito);
  }

  agregar(producto: ProductoVenta): void {
    const disponible = this.stockDisponibleReal(producto);
    if (disponible <= 0) return;
    const existente = this.carrito.find((l) => l.producto.id === producto.id);
    if (existente) {
      if (existente.cantidad < (producto.stockDisponible || 0)) {
        existente.cantidad += 1;
      }
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
    if (this.esEfectivo(pago) && !pago.montoEntregado) {
      pago.montoEntregado = pago.importe;
    }
  }

  vuelto(pago: PagoForm): number {
    if (!this.esEfectivo(pago) || !pago.montoEntregado || !pago.importe) return 0;
    return this.redondear(Math.max(0, Number(pago.montoEntregado) - Number(pago.importe)));
  }

  cambioTotal(): number {
    return this.redondear(this.pagos.reduce((acc, p) => acc + this.vuelto(p), 0));
  }

  esEfectivo(pago: PagoForm): boolean {
    const forma = this.catalogos()?.formasPago.find((f) => f.id === pago.formaPagoId);
    return forma ? forma.codigo === 'EFECTIVO' : false;
  }

  esQr(pago: PagoForm): boolean {
    const forma = this.catalogos()?.formasPago.find((f) => f.id === pago.formaPagoId);
    return forma ? forma.codigo === 'QR_SIMPLE' : false;
  }

  esTarjetaOPasarela(pago: PagoForm): boolean {
    const forma = this.catalogos()?.formasPago.find((f) => f.id === pago.formaPagoId);
    return forma ? (forma.codigo === 'TARJETA' || forma.codigo === PASARELA_ONLINE) : false;
  }

  montoExacto(pago: PagoForm): void {
    pago.montoEntregado = pago.importe;
  }

  establecerBillete(pago: PagoForm, valor: number): void {
    pago.montoEntregado = valor;
  }

  sumarEfectivo(pago: PagoForm, delta: number): void {
    pago.montoEntregado = this.redondear((Number(pago.montoEntregado) || 0) + delta);
  }

  pagoMixto5050(): void {
    const total = this.totalVenta();
    if (total <= 0) return;
    const mitad1 = this.redondear(total / 2);
    const mitad2 = this.redondear(total - mitad1);
    const fEfectivo = this.catalogos()?.formasPago.find((f) => f.codigo === 'EFECTIVO');
    const fQr = this.catalogos()?.formasPago.find((f) => f.codigo === 'QR_SIMPLE')
      || this.catalogos()?.formasPago.find((f) => f.codigo === 'TARJETA');

    this.pagos = [
      {
        formaPagoId: fEfectivo ? fEfectivo.id : (this.formasManuales()[0]?.id ?? null),
        importe: mitad1,
        transaccionExterna: '',
        montoEntregado: mitad1,
      },
      {
        formaPagoId: fQr ? fQr.id : (this.formasManuales()[1]?.id ?? null),
        importe: mitad2,
        transaccionExterna: 'QR-' + Date.now().toString().slice(-6),
      },
    ];
  }

  abrirModalQr(pago: PagoForm): void {
    this.pagoQrSeleccionado = pago;
    this.modalQr.set(true);
  }

  cerrarModalQr(): void {
    this.modalQr.set(false);
    this.pagoQrSeleccionado = null;
  }

  onSubirImagenQr(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      try {
        localStorage.setItem('erp_qr_custom_image', result);
      } catch {}
      this.imagenQrCustom.set(result);
    };
    reader.readAsDataURL(file);
  }

  eliminarImagenQr(): void {
    try {
      localStorage.removeItem('erp_qr_custom_image');
    } catch {}
    this.imagenQrCustom.set(null);
  }

  confirmarPagoQr(): void {
    if (this.pagoQrSeleccionado) {
      this.pagoQrSeleccionado.confirmado = true;
      if (!this.pagoQrSeleccionado.transaccionExterna) {
        const bancoPrefix = this.modoQr() === 'generar' ? this.bancoQr().slice(0, 3).toUpperCase() : 'QR';
        this.pagoQrSeleccionado.transaccionExterna = `${bancoPrefix}-${Date.now().toString().slice(-6)}`;
      }
    }
    this.modalQr.set(false);
    this.success.set('Pago QR Simple verificado y registrado con éxito.');
  }

  /** Venta completa (cliente, productos y stock), sin mirar todavía cómo se paga. */
  private ventaLista(): boolean {
    if (this.busy() || !this.catalogos()) return false;
    // Solo la factura necesita una dosificación del SIN vigente.
    if (this.tipoComprobante === 'FACTURA' && !this.catalogos()!.dosificacion) return false;
    if (!this.sucursalId || !this.cliente() || !this.carrito.length || this.totalVenta() <= 0) return false;
    return !this.carrito.some((l) => !l.cantidad || l.cantidad <= 0 || l.cantidad > l.producto.stockDisponible);
  }

  puedeCobrarConTarjeta(): boolean {
    return !!this.catalogos()?.cobroEnLinea && this.tipoVenta === 'CONTADO' && this.ventaLista();
  }

  puedeEmitir(): boolean {
    if (!this.ventaLista()) return false;
    if (this.tipoVenta === 'CONTADO') {
      if (!this.pagos.length) return false;
      if (Math.abs(this.pendiente()) > 0.005) return false;
      return this.pagos.every((p) => {
        if (!p.formaPagoId || !p.importe || Number(p.importe) <= 0) return false;
        // Para efectivo, se debe haber ingresado el monto entregado suficiente
        if (this.esEfectivo(p)) {
          return (p.montoEntregado || 0) >= p.importe;
        }
        // Para QR, primero se debe confirmar el cobro
        if (this.esQr(p)) {
          return !!p.confirmado;
        }
        // Para tarjeta / pasarela online, primero se realiza el pago y se registra la referencia confirmada
        if (this.esTarjetaOPasarela(p)) {
          return !!p.confirmado && !!p.transaccionExterna && p.transaccionExterna.trim().length > 0;
        }
        return true;
      });
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
    this.cliente.set(null);
    this.carrito = [];
    this.cotizacionSeleccionadaId = null;
    this.tipoComprobante = this.catalogos()?.dosificacion ? 'FACTURA' : 'NOTA_VENTA';
    this.tipoVenta = 'CONTADO';
    this.diasCredito = 30;
    const forma = this.formasManuales()[0];
    this.pagos = forma ? [{ formaPagoId: forma.id, importe: null, transaccionExterna: '' }] : [];
    this.cdr.markForCheck();
  }

  private armarVenta(pagos: VentaPagoRequest[]): VentaRequest {
    return {
      sucursalId: this.sucursalId!,
      clienteId: this.cliente()?.id || 1,
      turnoCajaId: this.turnoCajaId,
      cotizacionId: this.cotizacionSeleccionadaId,
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
    const itemsVendidos = [...this.carrito];
    this.api
      .registrar(body)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (venta) => {
          this.busy.set(false);
          this.factura.set(venta);
          this.success.set(`${this.nombreComprobante(venta)} ${mensajeExito}`);

          // Disparar alertas de stock mínimo para la campanita de notificaciones
          for (const item of itemsVendidos) {
            const restante = item.producto.stockDisponible - item.cantidad;
            if (restante <= 3) {
              this.notificacionesService.agregarNotificacion({
                titulo: `Alerta: Stock Mínimo (${item.producto.nombre})`,
                mensaje: `El producto ${item.producto.nombre} [${item.producto.codigo}] ha alcanzado su nivel de reposición (${Math.max(0, restante)} u. restantes).`,
                categoria: 'inventario',
                tipo: 'urgente',
                moduloId: 'catalogo-e-inventario',
                funcionId: 'cu10-catalogo-de-articulos',
              });
            }
          }

          this.limpiarVenta();
          this.cargarCatalogos();
          this.buscarProductos();
          this.notificacionesService.cargarNotificaciones();
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
   * página de pago de Stripe. El pago genera la referencia para el Paso 4 y habilita Emitir Factura.
   */
  cobrarConTarjeta(): void {
    if (!this.puedeCobrarConTarjeta()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    const base = `${window.location.origin}/app/facturacion`;
    this.api
      .iniciarCobroStripe({
        venta: this.armarVenta([]),
        successUrl: `${base}?pago=exito&session_id={CHECKOUT_SESSION_ID}`,
        cancelUrl: `${base}?pago=cancelado`,
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
            cotizacionId: this.cotizacionSeleccionadaId,
            cliente: this.cliente()!,
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

  /** Al volver de Stripe: restaura la venta y, si el pago se completó, coloca la referencia en Cobro y Medios de Pago. */
  private retomarCobroStripe(pago: string, sessionId: string | null): void {
    const borrador = this.leerBorrador();
    this.borrarBorrador();
    this.router.navigate(['/app', 'facturacion'], {
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
    this.cotizacionSeleccionadaId = borrador.cotizacionId || null;
    this.cliente.set(borrador.cliente);
    this.carrito = borrador.carrito;
    this.buscarProductos();
    this.cdr.markForCheck();

    if (pago !== 'exito' || sessionId !== borrador.sessionId) {
      this.error.set('El pago con tarjeta fue cancelado en la pasarela. La venta sigue en pantalla para reintentar o cobrar con otro medio.');
      return;
    }
    const pasarela = this.catalogos()?.formasPago.find((f) => f.codigo === PASARELA_ONLINE)
      || this.catalogos()?.formasPago.find((f) => f.codigo === 'TARJETA')
      || this.formasManuales()[0];
    if (!pasarela) {
      this.error.set('La forma de pago con tarjeta ya no está activa; no se pudo registrar el pago.');
      return;
    }

    // Se asigna el pago verificado con su número de referencia al Punto 4 Cobro y Medios de Pago
    this.pagos = [
      {
        formaPagoId: pasarela.id,
        importe: borrador.total,
        transaccionExterna: sessionId || ('STRIPE-' + Date.now().toString().slice(-6)),
        confirmado: true,
      },
    ];

    this.success.set(
      `✓ Pago con Tarjeta completado exitosamente en Stripe (Ref: ${this.pagos[0].transaccionExterna}). Monto: Bs ${borrador.total.toFixed(2)}. Ahora puedes emitir la factura oficial.`
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
          this.notificacionesService.cargarNotificaciones();
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

  imprimirComprobante(): void {
    const receiptEl = document.getElementById('printable-receipt');
    if (!receiptEl) {
      window.print();
      return;
    }
    const printWindow = window.open('', '_blank', 'width=850,height=950');
    if (!printWindow) {
      window.print();
      return;
    }
    printWindow.document.write(`<!DOCTYPE html>
<html>
  <head>
    <title>Comprobante de Venta - ${this.factura()?.numeroFactura || 'Documento'}</title>
    <meta charset="utf-8">
    <style>
      @page { size: auto; margin: 15mm; }
      * { box-sizing: border-box; }
      body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        color: #0f172a;
        background: #ffffff;
        margin: 0;
        padding: 12px;
        font-size: 12px;
        line-height: 1.4;
      }
      .receipt-header { display: flex; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
      .company-name { font-size: 18px; font-weight: 800; margin: 0 0 2px; }
      .company-sub { font-size: 11px; color: #475569; font-weight: 600; }
      .company-address { font-size: 10px; color: #64748b; margin: 2px 0; }
      .fiscal-box { border: 2px solid #0f172a; padding: 10px 16px; text-align: center; border-radius: 8px; min-width: 210px; }
      .fiscal-nit { font-size: 12px; }
      .fiscal-title { font-size: 16px; font-weight: 800; letter-spacing: 1px; margin: 4px 0; }
      .fiscal-number { font-size: 15px; }
      .fiscal-auth { font-size: 10px; margin-top: 4px; }
      .receipt-divider { border-top: 2px solid #0f172a; margin: 12px 0; }
      .receipt-client-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; font-size: 11.5px; margin-bottom: 14px; }
      .client-item .lbl { font-weight: 700; color: #475569; margin-right: 4px; }
      .client-item .val { font-weight: 600; }
      .font-bold { font-weight: 700; }
      .mono { font-family: monospace; font-size: 11px; }
      .receipt-items-table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
      .receipt-items-table th { border-bottom: 2px solid #0f172a; padding: 6px 8px; font-size: 11px; font-weight: 700; text-align: left; }
      .receipt-items-table td { padding: 6px 8px; border-bottom: 1px solid #e2e8f0; font-size: 11.5px; }
      .text-left { text-align: left; }
      .text-right { text-align: right; }
      .receipt-totals-section { display: flex; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
      .receipt-literal-wrap { flex: 1; font-size: 11px; }
      .literal-title { font-weight: 700; margin-right: 4px; }
      .tax-info-literal, .asiento-info { font-size: 10px; color: #64748b; margin: 4px 0 0; }
      .receipt-figures { min-width: 240px; }
      .figure-row { display: flex; justify-content: space-between; padding: 2px 0; font-size: 11.5px; }
      .figure-row.grand-total { font-size: 14px; font-weight: 800; border-top: 2px solid #0f172a; padding-top: 6px; margin-top: 4px; }
      .receipt-payments { border-top: 1px dashed #94a3b8; padding-top: 8px; margin-bottom: 12px; font-size: 11px; }
      .payment-chips { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
      .receipt-chip { background: #f1f5f9; padding: 3px 8px; border-radius: 4px; border: 1px solid #cbd5e1; font-size: 11px; }
      .receipt-sin-footer { border-top: 1px solid #0f172a; padding-top: 10px; margin-top: 10px; }
      .sin-qr-box { display: flex; gap: 14px; align-items: center; }
      .simulated-qr { width: 70px; height: 70px; border: 1px solid #000; display: flex; align-items: center; justify-content: center; font-size: 9px; font-weight: bold; }
      .sin-control-data { flex: 1; font-size: 10.5px; }
      .sin-control-data p { margin: 2px 0; }
      .leyenda-fiscal { font-size: 9px; font-style: italic; color: #475569; margin-top: 4px; }
      .center-note { text-align: center; }
      .anulacion-box { border: 2px solid #ef4444; color: #b91c1c; background: #fef2f2; padding: 8px; text-align: center; margin-top: 10px; font-weight: bold; }
      .no-print { display: none !important; }
    </style>
  </head>
  <body>
    ${receiptEl.innerHTML}
    <script>
      window.onload = function() {
        window.focus();
        window.print();
        setTimeout(function() { window.close(); }, 400);
      };
    </script>
  </body>
</html>`);
    printWindow.document.close();
  }

  descargarPdf(): void {
    this.imprimirComprobante();
  }

  numeroALetras(monto: number): string {
    const unidades = ['', 'UN', 'DOS', 'TRES', 'CUATRO', 'CINCO', 'SEIS', 'SIETE', 'OCHO', 'NUEVE'];
    const especiales = ['DIEZ', 'ONCE', 'DOCE', 'TRECE', 'CATORCE', 'QUINCE', 'DIECISEIS', 'DIECISIETE', 'DIECIOCHO', 'DIECINUEVE'];
    const decenas = ['', 'DIEZ', 'VEINTE', 'TREINTA', 'CUARENTA', 'CINCUENTA', 'SESENTA', 'SETENTA', 'OCHENTA', 'NOVENTA'];
    const centenas = ['', 'CIENTO', 'DOSCIENTOS', 'TRESCIENTOS', 'CUATROCIENTOS', 'QUINIENTOS', 'SEISCIENTOS', 'SETECIENTOS', 'OCHOCIENTOS', 'NOVECIENTOS'];

    const parteEntera = Math.floor(Math.abs(monto));
    const centavos = Math.round((Math.abs(monto) - parteEntera) * 100);
    const centavosStr = centavos < 10 ? '0' + centavos : centavos.toString();

    if (parteEntera === 0) return `CERO ${centavosStr}/100 BOLIVIANOS`;
    if (parteEntera === 100) return `CIEN ${centavosStr}/100 BOLIVIANOS`;

    const convertirGrupo = (n: number): string => {
      let resultado = '';
      if (n >= 100) {
        if (n === 100) return 'CIEN ';
        resultado += centenas[Math.floor(n / 100)] + ' ';
        n %= 100;
      }
      if (n >= 10 && n <= 19) {
        resultado += especiales[n - 10] + ' ';
      } else if (n >= 20 && n <= 29) {
        resultado += (n === 20 ? 'VEINTE ' : 'VEINTI' + unidades[n % 10] + ' ');
      } else if (n >= 30) {
        resultado += decenas[Math.floor(n / 10)];
        if (n % 10 > 0) resultado += ' Y ' + unidades[n % 10];
        resultado += ' ';
      } else if (n > 0) {
        resultado += unidades[n] + ' ';
      }
      return resultado;
    };

    let texto = '';
    const miles = Math.floor(parteEntera / 1000);
    const resto = parteEntera % 1000;

    if (miles > 0) {
      if (miles === 1) {
        texto += 'MIL ';
      } else {
        texto += convertirGrupo(miles) + 'MIL ';
      }
    }
    if (resto > 0) {
      texto += convertirGrupo(resto);
    }

    return `${texto.trim()} ${centavosStr}/100 BOLIVIANOS`;
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
