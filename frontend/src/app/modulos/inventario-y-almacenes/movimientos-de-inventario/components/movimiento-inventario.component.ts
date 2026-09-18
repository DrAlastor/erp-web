import { PermisosService } from '../../../seguridad-y-auditoria/roles-y-permisos/services/permisos.service';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MovimientoInventarioService } from '../services/movimiento-inventario.service';
import {
  AlertaStockMinimoResponse,
  AlmacenSimple,
  MovimientoInventarioRequest,
  MovimientoInventarioResponse,
  ProductoSimple,
  StockAlmacenResponse
} from '../models/movimiento-inventario.model';

@Component({
  selector: 'app-movimiento-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './movimiento-inventario.component.html',
  styleUrl: './movimiento-inventario.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientoInventarioComponent implements OnInit {
  private readonly permisos = inject(PermisosService);
  readonly puedeRegistrar = computed(() => this.permisos.tieneAlguno(['INVENTARIO_CREAR', 'INVENTARIO_MODIFICAR']));
  private readonly api = inject(MovimientoInventarioService);
  private readonly destroy = inject(DestroyRef);

  // Tabs de navegación
  readonly tabActiva = signal<'movimientos' | 'stock' | 'alertas'>('movimientos');

  // Estados de datos (Signals)
  readonly movimientos = signal<MovimientoInventarioResponse[]>([]);
  readonly stockList = signal<StockAlmacenResponse[]>([]);
  readonly alertasList = signal<AlertaStockMinimoResponse[]>([]);
  readonly productos = signal<ProductoSimple[]>([]);
  readonly almacenes = signal<AlmacenSimple[]>([]);

  // Estados visuales y de carga
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  // Modales
  readonly modalNuevoAbierto = signal(false);
  readonly movimientoSeleccionado = signal<MovimientoInventarioResponse | null>(null);

  // Paginación
  readonly pageMovimientos = signal(0);
  readonly totalMovimientos = signal(0);
  readonly paginasMovimientos = signal(0);

  readonly pageStock = signal(0);
  readonly totalStock = signal(0);
  readonly paginasStock = signal(0);

  // Filtros de Movimientos
  filtroMovProductoId: number | '' = '';
  filtroMovAlmacenId: number | '' = '';
  filtroMovTipo: string = '';

  // Filtros de Stock
  filtroStockAlmacenId: number | '' = '';
  filtroStockSearch: string = '';

  // Filtros de Alertas
  filtroAlertaAlmacenId: number | '' = '';

  // Formulario Nuevo Movimiento
  formProductoId: number | null = null;
  formAlmacenId: number | null = null;
  formTipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE_POSITIVO' | 'AJUSTE_NEGATIVO' = 'ENTRADA';
  formCantidad: number | null = null;
  formCostoUnitario: number | null = null;
  formReferenciaDoc: string = '';
  formMotivo: string = '';

  // Consulta de stock en tiempo real para el formulario
  readonly stockDisponibleForm = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarCatalogos();
    this.cargarMovimientos();
    this.cargarStock();
    this.cargarAlertas();
  }

  cambiarTab(tab: 'movimientos' | 'stock' | 'alertas'): void {
    this.tabActiva.set(tab);
    this.error.set('');
    this.success.set('');
    if (tab === 'movimientos') this.cargarMovimientos();
    if (tab === 'stock') this.cargarStock();
    if (tab === 'alertas') this.cargarAlertas();
  }

  cargarCatalogos(): void {
    this.api.listarProductos()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (prods) => this.productos.set(prods),
        error: () => {}
      });

    this.api.listarAlmacenes()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (alms) => {
          this.almacenes.set(alms);
          if (alms.length > 0 && !this.formAlmacenId) {
            this.formAlmacenId = alms[0].id;
          }
        },
        error: () => {}
      });
  }

  // ==========================================
  // ① GESTIÓN DE MOVIMIENTOS (KARDEX)
  // ==========================================
  cargarMovimientos(resetPage = false): void {
    if (resetPage) this.pageMovimientos.set(0);
    this.loading.set(true);
    this.error.set('');

    const prodId = this.filtroMovProductoId ? Number(this.filtroMovProductoId) : undefined;
    const almId = this.filtroMovAlmacenId ? Number(this.filtroMovAlmacenId) : undefined;

    this.api.listarMovimientos(
      prodId,
      almId,
      this.filtroMovTipo || undefined,
      undefined,
      undefined,
      this.pageMovimientos(),
      15
    )
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (page) => {
          this.movimientos.set(page.content);
          this.totalMovimientos.set(page.totalElements);
          this.paginasMovimientos.set(page.totalPages);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.manejarError(err);
        }
      });
  }

  abrirModalNuevo(): void {
    if (!this.puedeRegistrar()) return;
    this.modalNuevoAbierto.set(true);
    this.error.set('');
    this.success.set('');
    this.formCantidad = null;
    this.formReferenciaDoc = '';
    this.formMotivo = '';
    this.formTipo = 'ENTRADA';
    if (this.productos().length > 0) {
      this.formProductoId = this.productos()[0].id;
      this.actualizarCostoYStockForm();
    }
  }

  cerrarModalNuevo(): void {
    this.modalNuevoAbierto.set(false);
    this.stockDisponibleForm.set(null);
  }

  onProductoOAlmacenChange(): void {
    this.actualizarCostoYStockForm();
  }

  actualizarCostoYStockForm(): void {
    if (!this.formProductoId) return;

    const prod = this.productos().find(p => p.id === Number(this.formProductoId));
    if (prod) {
      this.formCostoUnitario = prod.costoPromedio;
    }

    if (this.formProductoId && this.formAlmacenId) {
      this.api.consultarStockDisponible(Number(this.formProductoId), Number(this.formAlmacenId))
        .pipe(takeUntilDestroyed(this.destroy))
        .subscribe({
          next: (res) => this.stockDisponibleForm.set(res.stockDisponible),
          error: () => this.stockDisponibleForm.set(0)
        });
    }
  }

  guardarMovimiento(): void {
    if (!this.formProductoId || !this.formAlmacenId) {
      this.error.set('Selecciona un producto y un almacén válidos.');
      return;
    }

    if (!this.formCantidad || this.formCantidad <= 0) {
      this.error.set('La cantidad debe ser mayor a cero.');
      return;
    }

    if (this.formTipo.startsWith('AJUSTE') && (!this.formMotivo || !this.formMotivo.trim())) {
      this.error.set('El motivo o justificación es obligatorio para los ajustes de inventario.');
      return;
    }

    if (this.formTipo === 'SALIDA' || this.formTipo === 'AJUSTE_NEGATIVO') {
      const disponible = this.stockDisponibleForm() ?? 0;
      if (this.formCantidad > disponible) {
        this.error.set(`Stock insuficiente. Disponible: ${disponible}, Solicitado: ${this.formCantidad}`);
        return;
      }
    }

    const request: MovimientoInventarioRequest = {
      productoId: Number(this.formProductoId),
      almacenId: Number(this.formAlmacenId),
      tipoMovimiento: this.formTipo,
      cantidad: this.formCantidad,
      costoUnitario: this.formCostoUnitario ?? undefined,
      referenciaDoc: this.formReferenciaDoc.trim() || undefined,
      motivo: this.formMotivo.trim() || undefined
    };

    this.busy.set(true);
    this.error.set('');

    this.api.registrarMovimiento(request)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (resp) => {
          this.busy.set(false);
          this.cerrarModalNuevo();
          this.success.set(`¡Movimiento #${resp.id} (${resp.tipoMovimiento}) registrado con éxito! Nuevo saldo: ${resp.saldoCantidad}`);
          this.cargarMovimientos(true);
          this.cargarStock();
          this.cargarAlertas();
        },
        error: (err) => {
          this.busy.set(false);
          this.manejarError(err);
        }
      });
  }

  // ==========================================
  // ② ADMINISTRAR STOCK POR ALMACÉN
  // ==========================================
  cargarStock(resetPage = false): void {
    if (resetPage) this.pageStock.set(0);
    this.loading.set(true);

    const almId = this.filtroStockAlmacenId ? Number(this.filtroStockAlmacenId) : undefined;

    this.api.listarStock(
      almId,
      undefined,
      this.filtroStockSearch,
      this.pageStock(),
      15
    )
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (page) => {
          this.stockList.set(page.content);
          this.totalStock.set(page.totalElements);
          this.paginasStock.set(page.totalPages);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.manejarError(err);
        }
      });
  }

  // ==========================================
  // ③ ALERTAS DE STOCK MÍNIMO
  // ==========================================
  cargarAlertas(): void {
    const almId = this.filtroAlertaAlmacenId ? Number(this.filtroAlertaAlmacenId) : undefined;

    this.api.listarAlertasStockMinimo(almId)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (alertas) => this.alertasList.set(alertas),
        error: (err) => this.manejarError(err)
      });
  }

  // Helper visual para criticidad
  claseCriticidad(criticidad: string): string {
    switch (criticidad) {
      case 'AGOTADO': return 'badge-danger';
      case 'CRITICO': return 'badge-warning';
      default: return 'badge-alert';
    }
  }

  claseTipoMovimiento(tipo: string): string {
    if (tipo === 'ENTRADA' || tipo === 'AJUSTE_POSITIVO') return 'tipo-entrada';
    if (tipo === 'SALIDA' || tipo === 'AJUSTE_NEGATIVO') return 'tipo-salida';
    return 'tipo-ajuste';
  }

  private manejarError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const msg = err.error?.message || err.error?.error || err.message;
      this.error.set(msg || 'Ocurrió un error inesperado al procesar el inventario.');
    } else {
      this.error.set('No se pudo establecer conexión con el servidor.');
    }
  }
}
