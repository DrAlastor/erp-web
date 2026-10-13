import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Cliente, ClientesService } from '../services/clientes.service';

@Component({
  selector: 'app-cliente-portal',
  standalone: true,
  templateUrl: './cliente-portal.component.html',
  styleUrl: './cliente-portal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientePortalComponent {
  private readonly api = inject(ClientesService);
  private readonly destroy = inject(DestroyRef);

  readonly cliente = signal<Cliente | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor() {
    this.api
      .current()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (cliente) => {
          this.cliente.set(cliente);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(
            err.status === 0
              ? 'No se pudo conectar con el servidor.'
              : 'No se pudo cargar tu perfil de cliente.',
          );
        },
      });
  }
}
