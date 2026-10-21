import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { esAdministrador } from '../../compras-roles';
import { ProveedoresService } from '../services/proveedores.service';
import { CondicionPago, HistorialProveedor, Proveedor, ProveedorRequest } from '../models/proveedor.model';

type Vista = 'lista' | 'editor' | 'ficha';

const NIT = /^\d{4,30}$/;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TELEFONO = /^[0-9+()\-\s]{6,30}$/;

/**
 * CU21 - Gestionar Proveedores.
 *
 * <p>El Almacenero sólo consulta; registrar, modificar, desactivar y reactivar son del
 * Administrador. Los botones se esconden según el rol, pero quien decide es el backend.
 */
@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './proveedores.component.html',
  styleUrl: './proveedores.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProveedoresComponent {
  private readonly api = inject(ProveedoresService);
  private readonly permisos = inject(PermisosService);
  private readonly destroy = inject(DestroyRef);
  private generation = 0;

  readonly vista = signal<Vista>('lista');
  readonly proveedores = signal<Proveedor[]>([]);
  readonly ciudades = signal<string[]>([]);
  readonly ficha = signal<HistorialProveedor | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);
  readonly admin = computed(() => esAdministrador(this.permisos));

  // filtros
  search = '';
  activo = 'true';
  ciudad = '';
  page = 0;

  // formulario
  readonly editandoId = signal<number | null>(null);
  readonly condicion = signal<CondicionPago>('CONTADO');
  form = this.formularioVacio();
  readonly errores = signal<Partial<Record<keyof ProveedorRequest, string>>>({});

  // diálogo de desactivación
  readonly desactivando = signal(false);
  motivo = '';

  constructor() {
    this.permisos.asegurarCargados().pipe(takeUntilDestroyed(this.destroy)).subscribe({ error: () => undefined });
    this.load();
    this.cargarCiudades();
  }

  // ------------------------------------------------------------- listado --

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.search.trim(), this.activo, this.ciudad, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          if (generation !== this.generation) return;
          this.proveedores.set(r.content);
          this.total.set(r.totalElements);
          this.pages.set(r.totalPages);
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

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  volver(): void {
    this.vista.set('lista');
    this.ficha.set(null);
    this.load();
    this.cargarCiudades();
  }

  // -------------------------------------------------------------- editor --

  nuevo(): void {
    this.limpiarMensajes();
    this.editandoId.set(null);
    this.form = this.formularioVacio();
    this.condicion.set('CONTADO');
    this.errores.set({});
    this.vista.set('editor');
  }

  editar(p: Proveedor): void {
    this.limpiarMensajes();
    this.editandoId.set(p.id);
    this.form = {
      razonSocial: p.razonSocial,
      nitCi: p.nitCi,
      contacto: p.contacto,
      telefono: p.telefono,
      email: p.email,
      direccion: p.direccion,
      ciudad: p.ciudad,
      condicionPago: p.condicionPago,
      diasCredito: p.diasCredito ?? 0,
    };
    this.condicion.set(p.condicionPago);
    this.errores.set({});
    this.vista.set('editor');
  }

  cambiarCondicion(valor: CondicionPago): void {
    this.form.condicionPago = valor;
    this.condicion.set(valor);
    if (valor === 'CONTADO') this.form.diasCredito = 0;
    else if (!this.form.diasCredito) this.form.diasCredito = 30;
  }

  guardar(): void {
    if (this.busy() || !this.validar()) return;
    const body: ProveedorRequest = {
      razonSocial: this.form.razonSocial.trim(),
      nitCi: this.form.nitCi.trim(),
      contacto: this.opcional(this.form.contacto),
      telefono: this.opcional(this.form.telefono),
      email: this.opcional(this.form.email),
      direccion: this.opcional(this.form.direccion),
      ciudad: this.opcional(this.form.ciudad),
      condicionPago: this.form.condicionPago,
      diasCredito: this.form.condicionPago === 'CONTADO' ? 0 : Number(this.form.diasCredito),
    };
    const id = this.editandoId();
    this.ejecutar(id ? this.api.update(id, body) : this.api.create(body), (p) => {
      this.abrir(p, id ? 'Proveedor actualizado.' : `Proveedor ${p.razonSocial} registrado.`);
    });
  }

  // --------------------------------------------------------------- ficha --

  abrir(p: Proveedor, mensaje = ''): void {
    if (!mensaje) this.limpiarMensajes();
    this.busy.set(true);
    this.api
      .historial(p.id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (h) => {
          this.busy.set(false);
          this.ficha.set(h);
          this.vista.set('ficha');
          if (mensaje) this.success.set(mensaje);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  desactivar(): void {
    const h = this.ficha();
    if (!h || !this.motivo.trim()) return;
    this.ejecutar(this.api.deactivate(h.proveedor.id, this.motivo.trim()), (p) => {
      this.desactivando.set(false);
      this.abrir(p, 'Proveedor desactivado. Ya no aparece en órdenes ni compras nuevas.');
    });
  }

  reactivar(p: Proveedor): void {
    this.ejecutar(this.api.reactivate(p.id), (r) => this.abrir(r, 'Proveedor reactivado.'));
  }

  // --------------------------------------------------------------- apoyo --

  private validar(): boolean {
    const f = this.form;
    const e: Partial<Record<keyof ProveedorRequest, string>> = {};
    if (!f.razonSocial?.trim()) e.razonSocial = 'La razón social es obligatoria.';
    if (!NIT.test(f.nitCi?.trim() ?? '')) e.nitCi = 'El NIT/CI debe ser numérico (4 a 30 dígitos).';
    if (f.email?.trim() && !EMAIL.test(f.email.trim())) e.email = 'El correo no tiene un formato válido.';
    if (f.telefono?.trim() && !TELEFONO.test(f.telefono.trim())) e.telefono = 'Sólo dígitos, espacios, +, - y paréntesis.';
    if (f.condicionPago === 'CREDITO' && !(Number(f.diasCredito) > 0 && Number(f.diasCredito) <= 365)) {
      e.diasCredito = 'A crédito, los días deben estar entre 1 y 365.';
    }
    this.errores.set(e);
    if (Object.keys(e).length) this.error.set('Revisa los datos marcados.');
    return Object.keys(e).length === 0;
  }

  private cargarCiudades(): void {
    this.api
      .ciudades()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ next: (c) => this.ciudades.set(c), error: () => undefined });
  }

  private formularioVacio(): ProveedorRequest {
    return {
      razonSocial: '',
      nitCi: '',
      contacto: '',
      telefono: '',
      email: '',
      direccion: '',
      ciudad: '',
      condicionPago: 'CONTADO',
      diasCredito: 0,
    };
  }

  private opcional(valor: string | null): string | null {
    return valor?.trim() ? valor.trim() : null;
  }

  private ejecutar<T>(peticion: Observable<T>, alTerminar: (r: T) => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.limpiarMensajes();
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.busy.set(false);
        alTerminar(r);
      },
      error: (err) => {
        this.busy.set(false);
        this.fail(err);
      },
    });
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.success.set('');
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && this.vista() === 'lista');
    this.error.set(
      err.status === 403
        ? 'No tienes autorización para esta operación sobre proveedores.'
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
