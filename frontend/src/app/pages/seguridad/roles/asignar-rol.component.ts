import { Component, computed, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RolesService } from './roles.service';
import { Asignacion, Rol, UsuarioResumen } from './roles.model';
import { ErrorApi } from '../../../core/auth/auth.model';

/**
 * Asignar un rol a un usuario, o quitárselo.
 *
 * Al elegir un usuario se cargan los roles que ya tiene, así el administrador ve el estado
 * antes de cambiar nada y no intenta asignar dos veces el mismo rol. Si igual lo intenta,
 * el backend responde que ya lo tiene y el mensaje se muestra tal cual.
 */
@Component({
  selector: 'app-asignar-rol',
  template: `
    <div class="fondo" (click)="cancelar()"></div>

    <section class="panel" role="dialog" aria-modal="true" aria-labelledby="titulo-asignar">
      <h2 id="titulo-asignar">Asignar un rol</h2>

      <label class="campo">
        <span class="etiqueta">Usuario</span>
        <select [value]="usuarioId() ?? ''" (change)="elegirUsuario($event)">
          <option value="">Elegí un usuario</option>
          @for (usuario of usuarios(); track usuario.id) {
            <option [value]="usuario.id">{{ usuario.nombre }} — {{ usuario.email }}</option>
          }
        </select>
      </label>

      @if (usuarioId()) {
        <div class="ya-tiene">
          <p class="etiqueta">Roles que ya tiene</p>
          @if (asignados().length === 0) {
            <p class="vacio">Ninguno. Todavía no puede operar ningún módulo.</p>
          } @else {
            <ul>
              @for (asignado of asignados(); track asignado.rolId) {
                <li>
                  <span>{{ asignado.nombre }}</span>
                  <button type="button" class="quitar" [disabled]="trabajando()" (click)="quitar(asignado)">
                    Quitar
                  </button>
                </li>
              }
            </ul>
          }
        </div>

        <label class="campo">
          <span class="etiqueta">Rol a asignar</span>
          <select [value]="rolId() ?? ''" (change)="elegirRol($event)">
            <option value="">Elegí un rol</option>
            @for (rol of rolesDisponibles(); track rol.id) {
              <option [value]="rol.id">{{ rol.nombre }}</option>
            }
          </select>
        </label>
      }

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <footer class="pie">
        <button type="button" class="boton-principal" [disabled]="!puedeAsignar() || trabajando()" (click)="asignar()">
          {{ trabajando() ? 'Guardando…' : 'Asignar' }}
        </button>
        <button type="button" class="boton-texto" (click)="cancelar()">Cancelar</button>
      </footer>
    </section>
  `,
  styleUrl: './asignar-rol.component.scss',
})
export class AsignarRolComponent {
  private readonly servicio = inject(RolesService);

  /** Los roles de la empresa, que la pantalla ya tenía cargados. */
  readonly roles = input.required<Rol[]>();

  /** Se emite al cerrar, con un mensaje para mostrar o null si se canceló. */
  readonly cerrado = output<string | null>();

  readonly usuarios = signal<UsuarioResumen[]>([]);
  readonly asignados = signal<Asignacion[]>([]);
  readonly usuarioId = signal<string | null>(null);
  readonly rolId = signal<string | null>(null);
  readonly trabajando = signal(false);
  readonly error = signal<string | null>(null);

  /** Roles activos que el usuario todavía no tiene. */
  readonly rolesDisponibles = computed(() => {
    const yaTiene = new Set(this.asignados().map((asignado) => asignado.rolId));
    return this.roles().filter((rol) => rol.activo && !yaTiene.has(rol.id));
  });

  readonly puedeAsignar = computed(() => this.usuarioId() !== null && this.rolId() !== null);

  constructor() {
    this.servicio.usuarios().subscribe({
      next: (usuarios) => this.usuarios.set(usuarios),
      error: (fallo: HttpErrorResponse) => this.error.set(this.mensajeDe(fallo)),
    });
  }

  elegirUsuario(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.rolId.set(null);
    this.error.set(null);
    this.asignados.set([]);
    this.usuarioId.set(valor || null);

    if (valor) {
      this.servicio.rolesDeUsuario(valor).subscribe({
        next: (asignados) => this.asignados.set(asignados),
        error: (fallo: HttpErrorResponse) => this.error.set(this.mensajeDe(fallo)),
      });
    }
  }

  elegirRol(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.rolId.set(valor || null);
    this.error.set(null);
  }

  asignar(): void {
    const usuarioId = this.usuarioId();
    const rolId = this.rolId();
    if (!usuarioId || !rolId || this.trabajando()) {
      return;
    }

    this.trabajando.set(true);
    this.error.set(null);

    this.servicio.asignar(usuarioId, rolId).subscribe({
      next: () => {
        this.trabajando.set(false);
        const rol = this.roles().find((candidato) => candidato.id === rolId);
        const usuario = this.usuarios().find((candidato) => candidato.id === usuarioId);
        this.cerrado.emit(`${rol?.nombre} asignado a ${usuario?.nombre}.`);
      },
      error: (fallo: HttpErrorResponse) => {
        this.trabajando.set(false);
        this.error.set(this.mensajeDe(fallo));
      },
    });
  }

  quitar(asignado: Asignacion): void {
    const usuarioId = this.usuarioId();
    if (!usuarioId || this.trabajando()) {
      return;
    }

    this.trabajando.set(true);
    this.error.set(null);

    this.servicio.quitar(usuarioId, asignado.rolId).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.asignados.update((lista) => lista.filter((item) => item.rolId !== asignado.rolId));
      },
      error: (fallo: HttpErrorResponse) => {
        this.trabajando.set(false);
        this.error.set(this.mensajeDe(fallo));
      },
    });
  }

  cancelar(): void {
    this.cerrado.emit(null);
  }

  private mensajeDe(fallo: HttpErrorResponse): string {
    if (fallo.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    const cuerpo = fallo.error as ErrorApi | null;
    return cuerpo?.mensaje ?? 'La operación no se pudo completar.';
  }
}
