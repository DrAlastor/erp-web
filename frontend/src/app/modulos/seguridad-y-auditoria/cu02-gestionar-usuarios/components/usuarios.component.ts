import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UsuariosService, Role, User } from '../services/usuarios.service';

import { ActivatedRoute } from '@angular/router';

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
  private readonly route = inject(ActivatedRoute);
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

  // Modal Crear Usuario
  readonly modalCrear = signal(false);
  readonly empleadosDisponibles = signal<any[]>([]);
  empleadoSeleccionadoId: number | null = null;
  crearFullname = '';
  crearEmail = '';
  crearUsername = '';
  crearPassword = '';
  crearRolId = '';

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
      .subscribe({
        next: (roles) => {
          this.roles.set(roles);
          if (roles.length > 0 && !this.crearRolId) {
            this.crearRolId = roles[0].id;
          }
        },
        error: () => {}
      });

    // Precargar colaboradores disponibles para que el modal abra en 0ms
    this.api
      .empleadosDisponibles()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (emps) => this.empleadosDisponibles.set(emps),
        error: () => {}
      });

    this.load();

    // Si viene desde Gestionar Empleados con un empleado preseleccionado
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroy)).subscribe((params) => {
      const empIdParam = params.get('empleadoId');
      if (empIdParam) {
        this.abrirModalCrear(Number(empIdParam));
      }
    });
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

  abrirModalCrear(empIdPreseleccionado?: number): void {
    this.error.set('');
    this.success.set('');
    this.empleadoSeleccionadoId = empIdPreseleccionado ?? null;
    this.crearFullname = '';
    this.crearEmail = '';
    this.crearUsername = '';
    this.crearPassword = '';
    if (this.roles().length > 0 && !this.crearRolId) {
      this.crearRolId = this.roles()[0].id;
    }

    // ABRIR DE INMEDIATO (0ms de latencia)
    this.modalCrear.set(true);

    // Si ya tenemos colaboradores en memoria, autocompletar si viene con preselección
    if (this.empleadosDisponibles().length > 0 && empIdPreseleccionado) {
      const encontrado = this.empleadosDisponibles().find(e => e.id === empIdPreseleccionado);
      if (encontrado) {
        this.onEmpleadoSeleccionado(encontrado);
      }
    }

    // Refrescar en segundo plano sin bloquear la apertura
    this.api.empleadosDisponibles()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (emps) => {
          this.empleadosDisponibles.set(emps);
          if (empIdPreseleccionado) {
            const encontrado = emps.find(e => e.id === empIdPreseleccionado);
            if (encontrado) {
              this.onEmpleadoSeleccionado(encontrado);
            }
          }
        },
        error: () => {}
      });
  }

  cerrarModalCrear(): void {
    this.modalCrear.set(false);
  }

  onEmpleadoChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const empId = target.value ? Number(target.value) : null;
    this.empleadoSeleccionadoId = empId;
    if (!empId) return;

    const emp = this.empleadosDisponibles().find(e => e.id === empId);
    if (emp) {
      this.onEmpleadoSeleccionado(emp);
    }
  }

  private onEmpleadoSeleccionado(emp: any): void {
    this.empleadoSeleccionadoId = emp.id;
    this.crearFullname = emp.nombreCompleto || `${emp.nombre} ${emp.apellido}`;
    if (emp.email) {
      this.crearEmail = emp.email;
      const partes = emp.email.split('@');
      this.crearUsername = partes[0] || emp.ci;
    } else {
      const limpio = (emp.nombre || 'usuario').toLowerCase().replace(/\s+/g, '.');
      this.crearEmail = `${limpio}@asustech.bo`;
      this.crearUsername = limpio;
    }
    if (!this.crearPassword) {
      this.crearPassword = 'Erp' + (emp.ci ? emp.ci.substring(0, 4) : '2026') + '!';
    }
  }

  guardarNuevoUsuario(): void {
    if (!this.crearFullname.trim() || !this.crearEmail.trim() || !this.crearUsername.trim() || !this.crearPassword) {
      this.error.set('Complete todos los campos obligatorios para dar de alta al usuario.');
      return;
    }
    if (!this.crearRolId) {
      this.error.set('Debe seleccionar un rol para el nuevo usuario.');
      return;
    }

    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    const body = {
      username: this.crearUsername.trim(),
      email: this.crearEmail.trim(),
      password: this.crearPassword,
      fullname: this.crearFullname.trim(),
      rolesIds: [this.crearRolId],
      empleadoId: this.empleadoSeleccionadoId ?? undefined
    };

    this.api.create(body)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (nuevo) => {
          this.busy.set(false);
          this.cerrarModalCrear();
          this.success.set(`Cuenta para '${nuevo.fullname}' creada y vinculada exitosamente.`);
          this.load(true);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        }
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
    let msg = '';
    if (err.status === 403) {
      msg = 'No tienes autorización para gestionar usuarios.';
    } else if (err.status === 0) {
      msg = 'No se pudo conectar con el servidor. Intenta nuevamente.';
    } else if (err.status === 401) {
      msg = 'La sesión ha expirado. Inicia sesión nuevamente.';
    } else if (err.error?.errors && Array.isArray(err.error.errors) && err.error.errors.length > 0) {
      msg = err.error.errors.map((e: any) => e.defaultMessage || e.message).join(' · ');
    } else if (err.error?.message) {
      msg = err.error.message;
    } else if (err.error?.detail) {
      msg = err.error.detail;
    } else if (typeof err.error === 'string') {
      msg = err.error;
    } else if (err.status >= 500) {
      msg = 'El servidor no pudo completar la operación.';
    } else {
      msg = 'No se pudo completar la operación.';
    }
    this.error.set(msg);
  }
}
