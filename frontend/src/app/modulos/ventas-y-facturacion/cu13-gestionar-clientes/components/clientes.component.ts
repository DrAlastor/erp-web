import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe, DatePipe } from '@angular/common';
import { Cliente, ClienteRequest, ClientesService } from '../services/clientes.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
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

  // Modales
  readonly selected = signal<Cliente | null>(null);
  readonly viewOpen = signal(false);
  readonly editorOpen = signal(false);
  readonly statusConfirmation = signal<Cliente | null>(null);
  readonly deleteConfirmation = signal<Cliente | null>(null);

  // Paginación
  readonly total = signal(0);
  readonly pages = signal(0);
  page = 0;

  // Filtros
  search = '';
  condicionPagoFilter = '';
  activoFilter = '';

  // Campos de formulario
  razonSocial = '';
  nitCi = '';
  telefono = '';
  email = '';
  direccion = '';
  ciudad = '';
  condicionPago: 'CONTADO' | 'CREDITO' = 'CONTADO';
  limiteCredito: number = 0;
  diasCredito: number = 0;

  constructor() {
    this.load();
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.condicionPagoFilter, this.activoFilter, this.page)
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
    this.viewOpen.set(false);
    this.editorOpen.set(true);
  }

  openView(cliente: Cliente): void {
    this.error.set('');
    this.success.set('');
    this.selected.set(cliente);
    this.editorOpen.set(false);
    this.viewOpen.set(true);

    // Consultar detalle fresco de API
    this.api.detail(cliente.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (fresh) => {
          this.selected.set(fresh);
        },
        error: () => {
          // Mantener el que ya se tenía
        }
      });
  }

  openEdit(cliente: Cliente): void {
    this.error.set('');
    this.success.set('');
    this.selected.set(cliente);
    this.fillForm(cliente);
    this.viewOpen.set(false);
    this.editorOpen.set(true);

    // Cargar versión fresca si es posible
    this.api.detail(cliente.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (fresh) => {
          this.selected.set(fresh);
          this.fillForm(fresh);
        }
      });
  }

  switchToEditFromView(): void {
    const c = this.selected();
    if (!c) return;
    this.viewOpen.set(false);
    this.openEdit(c);
  }

  closeModal(): void {
    this.viewOpen.set(false);
    this.editorOpen.set(false);
    this.selected.set(null);
  }

  save(): void {
    if (this.busy()) return;

    if (!this.razonSocial.trim()) {
      this.error.set('La razón social es obligatoria.');
      return;
    }
    if (!this.nitCi.trim()) {
      this.error.set('El NIT o documento de identidad es obligatorio.');
      return;
    }

    if (this.condicionPago === 'CREDITO') {
      if (!this.limiteCredito || this.limiteCredito <= 0) {
        this.error.set('Para clientes con condición CRÉDITO, el límite de crédito debe ser mayor a 0 Bs.');
        return;
      }
      if (!this.diasCredito || this.diasCredito <= 0) {
        this.error.set('Para clientes con condición CRÉDITO, los días de plazo deben ser mayores a 0.');
        return;
      }
    }

    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    const body: ClienteRequest = {
      razonSocial: this.razonSocial.trim(),
      nitCi: this.nitCi.trim(),
      telefono: this.telefono.trim() || undefined,
      email: this.email.trim() || undefined,
      direccion: this.direccion.trim() || undefined,
      ciudad: this.ciudad.trim() || undefined,
      condicionPago: this.condicionPago,
      limiteCredito: this.condicionPago === 'CREDITO' ? Number(this.limiteCredito) : 0,
      diasCredito: this.condicionPago === 'CREDITO' ? Number(this.diasCredito) : 0,
    };

    const request = this.selected()
      ? this.api.update(this.selected()!.id, body)
      : this.api.create(body);

    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.busy.set(false);
        this.editorOpen.set(false);
        this.selected.set(null);
        this.success.set('Cliente comercial guardado exitosamente.');
        this.load();
      },
      error: (err) => {
        this.busy.set(false);
        this.fail(err);
      },
    });
  }

  confirmStatus(cliente: Cliente): void {
    this.error.set('');
    this.success.set('');
    this.statusConfirmation.set(cliente);
  }

  executeStatus(): void {
    const cliente = this.statusConfirmation();
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
          this.statusConfirmation.set(null);
          this.success.set(`El cliente '${cliente.razonSocial}' ahora está ${!cliente.activo ? 'ACTIVO' : 'INACTIVO'}.`);
          this.load();
        },
        error: (err) => {
          this.busy.set(false);
          this.statusConfirmation.set(null);
          this.fail(err);
        },
      });
  }

  confirmDelete(cliente: Cliente): void {
    this.error.set('');
    this.success.set('');
    this.deleteConfirmation.set(cliente);
  }

  executeDelete(): void {
    const cliente = this.deleteConfirmation();
    if (!cliente || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');

    this.api.delete(cliente.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.deleteConfirmation.set(null);
          this.success.set(`Cliente '${cliente.razonSocial}' eliminado correctamente.`);
          this.load();
        },
        error: (err) => {
          this.busy.set(false);
          this.deleteConfirmation.set(null);
          this.fail(err);
          this.load(); // Para reflejar si se inactivó lógicamente
        }
      });
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  getIniciales(razonSocial: string): string {
    if (!razonSocial) return 'CL';
    const partes = razonSocial.trim().split(/\s+/);
    if (partes.length >= 2) {
      return (partes[0][0] + partes[1][0]).toUpperCase();
    }
    return razonSocial.slice(0, 2).toUpperCase();
  }

  private fillForm(cliente: Cliente): void {
    this.razonSocial = cliente.razonSocial;
    this.nitCi = cliente.nitCi;
    this.telefono = cliente.telefono ?? '';
    this.email = cliente.email ?? '';
    this.direccion = cliente.direccion ?? '';
    this.ciudad = cliente.ciudad ?? '';
    this.condicionPago = cliente.condicionPago ?? 'CONTADO';
    this.limiteCredito = cliente.limiteCredito ?? 0;
    this.diasCredito = cliente.diasCredito ?? 0;
  }

  private clearForm(): void {
    this.razonSocial = '';
    this.nitCi = '';
    this.telefono = '';
    this.email = '';
    this.direccion = '';
    this.ciudad = '';
    this.condicionPago = 'CONTADO';
    this.limiteCredito = 0;
    this.diasCredito = 0;
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
            : err.status === 409
              ? err.error?.message || 'Conflicto: El registro tiene dependencias o el NIT/CI ya existe.'
              : err.error?.message || 'No se pudo completar la operación.',
    );
  }
}
