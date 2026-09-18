import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Cliente, ClienteRequest, ClientesService } from '../services/clientes.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientesComponent {
  private readonly api = inject(ClientesService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly clientes = signal<Cliente[]>([]);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly selected = signal<Cliente | null>(null);
  readonly confirmation = signal<Cliente | null>(null);
  readonly editorOpen = signal(false);
  readonly total = signal(0);
  readonly pages = signal(0);

  search = '';
  activo = '';
  page = 0;
  razonSocial = '';
  nitCi = '';
  telefono = '';
  direccion = '';

  constructor() {
    this.load();
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.activo, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          if (generation !== this.generation) return;
          this.clientes.set(result.content);
          this.total.set(result.totalElements);
          this.pages.set(result.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  openNew(): void {
    this.error.set('');
    this.success.set('');
    this.selected.set(null);
    this.clearForm();
    this.editorOpen.set(true);
  }

  open(cliente: Cliente): void {
    this.error.set('');
    this.success.set('');
    this.selected.set(null);
    this.busy.set(true);
    this.api
      .detail(cliente.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (value) => {
          this.selected.set(value);
          this.fillForm(value);
          this.editorOpen.set(true);
          this.busy.set(false);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  save(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    const body: ClienteRequest = {
      razonSocial: this.razonSocial.trim(),
      nitCi: this.nitCi.trim(),
      telefono: this.telefono.trim(),
      direccion: this.direccion.trim(),
    };
    const request = this.selected()
      ? this.api.update(this.selected()!.id, body)
      : this.api.create(body);
    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.busy.set(false);
        this.editorOpen.set(false);
        this.selected.set(null);
        this.success.set('Cliente guardado correctamente.');
        this.load();
      },
      error: (err) => {
        this.busy.set(false);
        this.fail(err);
      },
    });
  }

  changeStatus(): void {
    const cliente = this.confirmation();
    if (!cliente || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .status(cliente.id, !cliente.activo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.confirmation.set(null);
          this.success.set('Estado del cliente actualizado.');
          this.load();
        },
        error: (err) => {
          this.busy.set(false);
          this.confirmation.set(null);
          this.fail(err);
        },
      });
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  private fillForm(cliente: Cliente): void {
    this.razonSocial = cliente.razonSocial;
    this.nitCi = cliente.nitCi;
    this.telefono = cliente.telefono ?? '';
    this.direccion = cliente.direccion ?? '';
  }

  private clearForm(): void {
    this.razonSocial = '';
    this.nitCi = '';
    this.telefono = '';
    this.direccion = '';
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403);
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para gestionar clientes.'
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
