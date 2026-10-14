import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { ValidacionComercialService } from '../services/validacion-comercial.service';
import { PedidoResumen, ValidacionPedido } from '../models/validacion-comercial.model';

@Component({
  selector: 'app-validacion-comercial',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './validacion-comercial.component.html',
  styleUrl: './validacion-comercial.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidacionComercialComponent implements OnInit {
  private readonly api = inject(ValidacionComercialService);
  private readonly permisos = inject(PermisosService);
  private readonly destroy = inject(DestroyRef);

  /** Aprobar o rechazar pide modificar el módulo comercial. */
  readonly puedeDecidir = computed(() => this.permisos.tiene('COMERCIAL_MODIFICAR'));

  readonly pedidos = signal<PedidoResumen[]>([]);
  readonly seleccionado = signal<ValidacionPedido | null>(null);

  readonly loading = signal(false);
  readonly cargandoDetalle = signal(false);
  readonly procesando = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalPedidos = signal(0);

  estado = 'PENDIENTE';
  observacion = '';

  /** Reglas que impiden aprobar el pedido seleccionado. */
  readonly bloqueos = computed(() =>
    (this.seleccionado()?.reglas ?? []).filter((r) => !r.cumple && r.bloqueante)
  );

  /** El pedido seleccionado sigue esperando decisión. */
  readonly pendiente = computed(() => this.seleccionado()?.pedido.estado === 'PENDIENTE');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(reiniciar = false): void {
    if (reiniciar) this.pagina.set(0);
    this.loading.set(true);
    this.error.set('');
    this.api.listar(this.estado, this.pagina())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (page) => {
          this.pedidos.set(page.content);
          this.totalPaginas.set(page.totalPages);
          this.totalPedidos.set(page.totalElements);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.manejarError(e);
        }
      });
  }

  cambiarPagina(delta: number): void {
    const nueva = this.pagina() + delta;
    if (nueva < 0 || nueva >= this.totalPaginas()) return;
    this.pagina.set(nueva);
    this.cargar();
  }

  abrir(pedido: PedidoResumen): void {
    this.cargandoDetalle.set(true);
    this.error.set('');
    this.success.set('');
    this.observacion = '';
    this.api.evaluar(pedido.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (validacion) => {
          this.seleccionado.set(validacion);
          this.cargandoDetalle.set(false);
        },
        error: (e) => {
          this.cargandoDetalle.set(false);
          this.manejarError(e);
        }
      });
  }

  cerrar(): void {
    this.seleccionado.set(null);
    this.observacion = '';
  }

  aprobar(): void {
    const actual = this.seleccionado();
    if (!actual || !this.puedeDecidir()) return;
    if (actual.requiereJustificacion && !this.observacion.trim()) {
      this.error.set('Hay reglas no cumplidas: escribe la justificación para aprobar el pedido.');
      return;
    }
    this.decidir(actual, 'aprobar');
  }

  rechazar(): void {
    const actual = this.seleccionado();
    if (!actual || !this.puedeDecidir()) return;
    if (!this.observacion.trim()) {
      this.error.set('Escribe el motivo para rechazar el pedido.');
      return;
    }
    this.decidir(actual, 'rechazar');
  }

  claseEstado(estado: string): string {
    switch (estado) {
      case 'APROBADO':
      case 'COMPLETADO':
        return 'estado-ok';
      case 'RECHAZADO':
        return 'estado-no';
      default:
        return 'estado-pendiente';
    }
  }

  private decidir(validacion: ValidacionPedido, accion: 'aprobar' | 'rechazar'): void {
    this.procesando.set(true);
    this.error.set('');
    const peticion = accion === 'aprobar'
      ? this.api.aprobar(validacion.pedido.id, this.observacion.trim())
      : this.api.rechazar(validacion.pedido.id, this.observacion.trim());

    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (resultado) => {
        this.procesando.set(false);
        this.observacion = '';
        this.seleccionado.set(resultado);
        this.success.set(
          `Pedido ${resultado.pedido.numeroPedido} ${accion === 'aprobar' ? 'aprobado' : 'rechazado'} correctamente.`
        );
        this.cargar();
      },
      error: (e) => {
        this.procesando.set(false);
        this.manejarError(e);
        // El pedido pudo cambiar mientras se miraba: se vuelve a evaluar.
        this.api.evaluar(validacion.pedido.id)
          .pipe(takeUntilDestroyed(this.destroy))
          .subscribe({ next: (v) => this.seleccionado.set(v), error: () => {} });
      }
    });
  }

  private manejarError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const msg = err.error?.message || err.error?.error;
      this.error.set(msg || 'Ocurrió un error inesperado en la validación comercial.');
    } else {
      this.error.set('No se pudo establecer conexión con el servidor.');
    }
  }
}
