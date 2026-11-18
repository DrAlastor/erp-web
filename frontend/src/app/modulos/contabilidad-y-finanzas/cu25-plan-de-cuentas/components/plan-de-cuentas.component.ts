import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { PermisosService } from '../../../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { mensajeError, puedeContabilidad } from '../../contabilidad-permisos';
import { PlanCuentasService } from '../services/plan-cuentas.service';
import {
  Cuenta,
  CuentaNodo,
  FiltroCuentas,
  Naturaleza,
  TIPOS_CUENTA,
  TipoCuenta,
} from '../models/plan-cuentas.model';
import {
  aplanar,
  buscarNodo,
  codigoValido,
  esHijoDirecto,
  idsExpandibles,
  siguienteCodigoHijo,
  siguienteCodigoRaiz,
} from '../utils/plan-cuentas.utils';

type Editor =
  | { modo: 'raiz' }
  | { modo: 'hijo'; padre: Cuenta }
  | { modo: 'editar'; cuenta: Cuenta };

interface Formulario {
  codigo: string;
  nombre: string;
  tipo: TipoCuenta;
  naturaleza: Naturaleza;
  permiteMovimiento: boolean;
}

/**
 * CU25 - Gestionar Plan de Cuentas.
 *
 * Árbol jerárquico de cuentas. Las cuentas nunca se borran: se desactivan. Los botones se
 * esconden según el permiso, pero quien decide es el backend.
 */
@Component({
  selector: 'app-plan-de-cuentas',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './plan-de-cuentas.component.html',
  styleUrl: './plan-de-cuentas.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanDeCuentasComponent {
  private readonly api = inject(PlanCuentasService);
  private readonly permisos = inject(PermisosService);
  private readonly destroy = inject(DestroyRef);

  readonly tipos = TIPOS_CUENTA;

  readonly arbol = signal<CuentaNodo[]>([]);
  readonly expandidas = signal<ReadonlySet<number>>(new Set<number>());
  readonly filtro = signal<FiltroCuentas>({ texto: '', tipo: '', activo: 'true' });
  readonly filas = computed(() => aplanar(this.arbol(), this.expandidas(), this.filtro()));
  readonly total = computed(() => idsContados(this.arbol()));

  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly puedeCrear = computed(() => this.permisos.cargado() && puedeContabilidad(this.permisos, 'CREAR'));
  readonly puedeModificar = computed(
    () => this.permisos.cargado() && puedeContabilidad(this.permisos, 'MODIFICAR'),
  );

  readonly editor = signal<Editor | null>(null);
  readonly errores = signal<Partial<Record<keyof Formulario, string>>>({});
  form: Formulario = this.formularioVacio();

  readonly confirmando = signal<Cuenta | null>(null);

  constructor() {
    this.permisos
      .asegurarCargados()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({ error: () => undefined });
    this.load(true);
  }

  // ------------------------------------------------------------- árbol --

  load(primeraVez = false): void {
    this.loading.set(true);
    this.api
      .arbol()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (arbol) => {
          this.arbol.set(arbol);
          if (primeraVez) {
            // Abre los dos primeros niveles para que se vea la estructura.
            const ids = arbol.flatMap((r) => [r.cuenta.id, ...r.hijos.map((h) => h.cuenta.id)]);
            this.expandidas.set(new Set(ids));
          }
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  alternar(id: number): void {
    const nuevas = new Set(this.expandidas());
    if (nuevas.has(id)) nuevas.delete(id);
    else nuevas.add(id);
    this.expandidas.set(nuevas);
  }

  expandirTodo(): void {
    this.expandidas.set(new Set(idsExpandibles(this.arbol())));
  }

  contraerTodo(): void {
    this.expandidas.set(new Set());
  }

  filtrar(cambio: Partial<FiltroCuentas>): void {
    this.filtro.set({ ...this.filtro(), ...cambio });
  }

  // ------------------------------------------------------------ editor --

  nuevaRaiz(): void {
    this.limpiarMensajes();
    this.form = { ...this.formularioVacio(), codigo: siguienteCodigoRaiz(this.arbol()), permiteMovimiento: false };
    this.errores.set({});
    this.editor.set({ modo: 'raiz' });
  }

  nuevaSubcuenta(padre: Cuenta): void {
    this.limpiarMensajes();
    const nodo = buscarNodo(this.arbol(), padre.id);
    this.form = {
      ...this.formularioVacio(),
      codigo: nodo ? siguienteCodigoHijo(nodo) : `${padre.codigo}.1`,
      tipo: padre.tipo,
      naturaleza: padre.naturaleza,
    };
    this.errores.set({});
    this.editor.set({ modo: 'hijo', padre });
  }

  editar(cuenta: Cuenta): void {
    this.limpiarMensajes();
    this.form = {
      codigo: cuenta.codigo,
      nombre: cuenta.nombre,
      tipo: cuenta.tipo,
      naturaleza: cuenta.naturaleza,
      permiteMovimiento: cuenta.permiteMovimiento,
    };
    this.errores.set({});
    this.editor.set({ modo: 'editar', cuenta });
  }

  cerrarEditor(): void {
    this.editor.set(null);
  }

  cambiarTipo(tipo: TipoCuenta): void {
    this.form.tipo = tipo;
    this.form.naturaleza = tipo === 'ACTIVO' || tipo === 'EGRESO' ? 'DEUDORA' : 'ACREEDORA';
  }

  guardar(): void {
    const editor = this.editor();
    if (!editor || this.busy() || !this.validar(editor)) return;
    const nombre = this.form.nombre.trim();

    if (editor.modo === 'editar') {
      this.ejecutar(
        this.api.actualizar(editor.cuenta.id, { nombre, permiteMovimiento: this.form.permiteMovimiento }),
        (c) => this.terminar(`Cuenta ${c.codigo} actualizada.`),
      );
      return;
    }
    const padre = editor.modo === 'hijo' ? editor.padre : null;
    this.ejecutar(
      this.api.crear({
        codigo: this.form.codigo.trim(),
        nombre,
        padreId: padre?.id ?? null,
        tipo: padre ? null : this.form.tipo,
        naturaleza: padre ? null : this.form.naturaleza,
        permiteMovimiento: this.form.permiteMovimiento,
      }),
      (c) => {
        if (padre) this.expandidas.set(new Set([...this.expandidas(), padre.id]));
        this.terminar(`Cuenta ${c.codigo} - ${c.nombre} registrada.`);
      },
    );
  }

  // ---------------------------------------------------- activar / desactivar --

  pedirDesactivar(cuenta: Cuenta): void {
    this.limpiarMensajes();
    this.confirmando.set(cuenta);
  }

  desactivar(): void {
    const cuenta = this.confirmando();
    if (!cuenta) return;
    this.ejecutar(this.api.desactivar(cuenta.id), (c) => {
      this.confirmando.set(null);
      this.terminar(`Cuenta ${c.codigo} desactivada.`);
    });
  }

  reactivar(cuenta: Cuenta): void {
    this.ejecutar(this.api.reactivar(cuenta.id), (c) => this.terminar(`Cuenta ${c.codigo} reactivada.`));
  }

  // ------------------------------------------------------------- apoyo --

  sangria(profundidad: number): string {
    return `${profundidad * 20}px`;
  }

  private terminar(mensaje: string): void {
    this.editor.set(null);
    this.load();
    this.success.set(mensaje);
  }

  private validar(editor: Editor): boolean {
    const e: Partial<Record<keyof Formulario, string>> = {};
    const codigo = this.form.codigo.trim();
    if (!this.form.nombre.trim()) e.nombre = 'El nombre es obligatorio.';
    else if (this.form.nombre.trim().length > 150) e.nombre = 'Máximo 150 caracteres.';
    if (editor.modo !== 'editar') {
      if (!codigoValido(codigo)) e.codigo = 'Solo números separados por puntos (ej. 1.1.1.01).';
      else if (editor.modo === 'hijo' && !esHijoDirecto(editor.padre.codigo, codigo))
        e.codigo = `Debe ser ${editor.padre.codigo}.<n> (un segmento más que la cuenta padre).`;
      else if (editor.modo === 'raiz' && codigo.includes('.'))
        e.codigo = 'Una cuenta raíz tiene un solo segmento (ej. 6).';
    }
    this.errores.set(e);
    if (Object.keys(e).length) this.error.set('Revisa los datos marcados.');
    return Object.keys(e).length === 0;
  }

  private formularioVacio(): Formulario {
    return { codigo: '', nombre: '', tipo: 'ACTIVO', naturaleza: 'DEUDORA', permiteMovimiento: true };
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
    this.forbidden.set(err.status === 403 && !this.arbol().length);
    this.error.set(mensajeError(err.status, err.error?.message, 'el plan de cuentas'));
  }
}

function idsContados(nodos: readonly CuentaNodo[]): number {
  return nodos.reduce((n, nodo) => n + 1 + idsContados(nodo.hijos), 0);
}
