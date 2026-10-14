import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KardexService } from '../services/kardex.service';
import { MovimientoInventarioService } from '../../cu11-movimientos-de-inventario/services/movimiento-inventario.service';
import { AlmacenSimple, ProductoSimple } from '../../cu11-movimientos-de-inventario/models/movimiento-inventario.model';
import {
  KardexConsulta,
  KardexFiltros,
  KardexValoracion,
  MetodoCosteo
} from '../models/kardex.model';

@Component({
  selector: 'app-consultar-kardex',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './consultar-kardex.component.html',
  styleUrl: './consultar-kardex.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConsultarKardexComponent implements OnInit {
  private readonly api = inject(KardexService);
  private readonly inventario = inject(MovimientoInventarioService);
  private readonly destroy = inject(DestroyRef);

  readonly tab = signal<'kardex' | 'valoracion'>('kardex');

  readonly productos = signal<ProductoSimple[]>([]);
  readonly almacenes = signal<AlmacenSimple[]>([]);
  readonly kardex = signal<KardexConsulta | null>(null);
  readonly valoracion = signal<KardexValoracion | null>(null);

  readonly loading = signal(false);
  readonly exportando = signal(false);
  readonly error = signal('');

  // Filtros del Kardex
  productoId: number | '' = '';
  almacenId: number | '' = '';
  metodo: MetodoCosteo = 'PROMEDIO';
  fechaInicio = '';
  fechaFin = '';
  tipoMovimiento = '';

  // Filtros de la valoración
  valAlmacenId: number | '' = '';
  valMetodo: MetodoCosteo = 'PROMEDIO';

  ngOnInit(): void {
    this.inventario.listarProductos()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (lista) => this.productos.set(lista), error: (e) => this.manejarError(e) });

    this.inventario.listarAlmacenes()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => {
          this.almacenes.set(lista);
          if (lista.length > 0) {
            this.almacenId = lista[0].id;
            this.valAlmacenId = lista[0].id;
          }
        },
        error: (e) => this.manejarError(e)
      });
  }

  cambiarTab(tab: 'kardex' | 'valoracion'): void {
    this.tab.set(tab);
    this.error.set('');
    if (tab === 'valoracion' && !this.valoracion() && this.valAlmacenId) this.cargarValoracion();
  }

  consultar(): void {
    const filtros = this.filtros();
    if (!filtros) return;
    this.loading.set(true);
    this.error.set('');
    this.api.consultar(filtros)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (resultado) => {
          this.kardex.set(resultado);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.kardex.set(null);
          this.manejarError(e);
        }
      });
  }

  limpiar(): void {
    this.fechaInicio = '';
    this.fechaFin = '';
    this.tipoMovimiento = '';
    this.kardex.set(null);
    this.error.set('');
  }

  exportar(): void {
    const filtros = this.filtros();
    if (!filtros) return;
    this.exportando.set(true);
    this.api.exportar(filtros)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const enlace = document.createElement('a');
          enlace.href = url;
          enlace.download = `kardex-${filtros.productoId}-${filtros.almacenId}.csv`;
          enlace.click();
          URL.revokeObjectURL(url);
          this.exportando.set(false);
        },
        error: () => {
          this.exportando.set(false);
          this.error.set('No se pudo exportar el Kardex.');
        }
      });
  }

  cargarValoracion(): void {
    if (!this.valAlmacenId) {
      this.error.set('Selecciona un almacén para valorar sus existencias.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.api.valorar(Number(this.valAlmacenId), this.valMetodo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (resultado) => {
          this.valoracion.set(resultado);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.manejarError(e);
        }
      });
  }

  claseTipo(tipo: string): string {
    if (tipo === 'ENTRADA' || tipo === 'AJUSTE_POSITIVO') return 'tipo-entrada';
    if (tipo === 'SALIDA' || tipo === 'AJUSTE_NEGATIVO') return 'tipo-salida';
    return 'tipo-ajuste';
  }

  private filtros(): KardexFiltros | null {
    if (!this.productoId || !this.almacenId) {
      this.error.set('Selecciona un producto y un almacén para consultar el Kardex.');
      return null;
    }
    if (this.fechaInicio && this.fechaFin && this.fechaInicio > this.fechaFin) {
      this.error.set('La fecha de inicio no puede ser posterior a la fecha de fin.');
      return null;
    }
    return {
      productoId: Number(this.productoId),
      almacenId: Number(this.almacenId),
      metodo: this.metodo,
      fechaInicio: this.fechaInicio || undefined,
      fechaFin: this.fechaFin || undefined,
      tipoMovimiento: this.tipoMovimiento || undefined
    };
  }

  private manejarError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const msg = err.error?.message || err.error?.error;
      this.error.set(msg || 'Ocurrió un error inesperado al consultar el Kardex.');
    } else {
      this.error.set('No se pudo establecer conexión con el servidor.');
    }
  }
}
