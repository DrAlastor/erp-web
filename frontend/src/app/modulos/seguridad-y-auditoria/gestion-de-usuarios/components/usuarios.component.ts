import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UsuariosService, Role, User } from '../services/usuarios.service';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsuariosComponent {
  private readonly api = inject(UsuariosService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly users = signal<User[]>([]);
  readonly roles = signal<Role[]>([]);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly selected = signal<User | null>(null);
  readonly confirmation = signal<User | null>(null);
  readonly total = signal(0);
  readonly pages = signal(0);

  search = '';
  role = '';
  enable = '';
  page = 0;
  fullname = '';
  email = '';

  constructor() {
    this.api
      .roles()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (roles) => this.roles.set(roles), error: () => {} });
    this.load();
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.role, this.enable, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          if (generation !== this.generation) return;
          this.users.set(result.content);
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

  open(user: User): void {
    this.error.set('');
    this.selected.set(null);
    this.busy.set(true);
    this.api
      .detail(user.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (value) => {
          this.selected.set(value);
          this.fullname = value.fullname;
          this.email = value.email;
          this.busy.set(false);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  save(): void {
    const user = this.selected();
    if (!user || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .update(user.id, { fullname: this.fullname.trim(), email: this.email.trim() })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.selected.set(null);
          this.success.set('Información actualizada correctamente.');
          this.load();
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  changeStatus(): void {
    const user = this.confirmation();
    if (!user || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .status(user.id, !user.enable)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.confirmation.set(null);
          this.success.set('Estado de la cuenta actualizado.');
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

  roleNames(user: User): string {
    return user.roles.map((role) => role.nombre).join(', ') || 'Sin rol';
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403);
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para gestionar usuarios.'
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
