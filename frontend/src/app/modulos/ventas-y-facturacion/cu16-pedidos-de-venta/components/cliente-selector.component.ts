import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PedidosService } from '../services/pedidos.service';
import { ClienteOpcion } from '../models/pedido.model';
import { mensajeError } from '../utils/pedido.utils';

/** Buscador de clientes activos para el pedido (GET /pedidos/opciones/clientes). */
@Component({
  selector: 'app-cliente-selector',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (cliente(); as actual) {
      <div class="selected">
        <div>
          <strong>{{ actual.razonSocial }}</strong>
          <span class="cell-sub">NIT/CI {{ actual.nitCi }}</span>
        </div>
        <button type="button" [disabled]="disabled()" (click)="cambiar()">Cambiar</button>
      </div>
    } @else {
      <div class="search-row">
        <input
          [(ngModel)]="busqueda"
          maxlength="150"
          aria-label="Buscar cliente"
          placeholder="Razón social o NIT/CI"
          [class.invalid]="invalid()"
          (keydown.enter)="$event.preventDefault(); buscar()" />
        <button type="button" [disabled]="buscando()" (click)="buscar()">
          {{ buscando() ? 'Buscando...' : 'Buscar' }}
        </button>
      </div>
      @if (error()) {
        <p class="message error" role="alert">{{ error() }}</p>
      }
      @if (buscado()) {
        <ul class="results" role="listbox" aria-label="Clientes encontrados">
          @for (item of resultados(); track item.id) {
            <li>
              <button type="button" role="option" (click)="elegir(item)">
                <strong>{{ item.razonSocial }}</strong>
                <span class="cell-sub">NIT/CI {{ item.nitCi }}</span>
              </button>
            </li>
          } @empty {
            <li class="none">No se encontraron clientes activos.</li>
          }
        </ul>
      }
    }
  `,
  styleUrl: './selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClienteSelectorComponent {
  private readonly api = inject(PedidosService);
  private readonly destroy = inject(DestroyRef);

  readonly cliente = model<ClienteOpcion | null>(null);
  readonly disabled = input(false);
  readonly invalid = input(false);
  readonly resultados = signal<ClienteOpcion[]>([]);
  readonly buscando = signal(false);
  readonly buscado = signal(false);
  readonly error = signal('');
  busqueda = '';

  buscar(): void {
    this.buscando.set(true);
    this.error.set('');
    this.api
      .clientes(this.busqueda.trim())
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

  elegir(item: ClienteOpcion): void {
    this.cliente.set(item);
    this.buscado.set(false);
    this.resultados.set([]);
  }

  cambiar(): void {
    this.busqueda = '';
    this.cliente.set(null);
  }
}
