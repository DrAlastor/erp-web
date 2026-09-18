import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MisPermisos } from '../auth/auth.model';

/**
 * Permisos efectivos del usuario de la sesión.
 *
 * El backend sigue siendo la autoridad: esto solo sirve para ocultar o deshabilitar lo que
 * el usuario no puede usar. Los permisos se piden al backend y no salen del token, así que
 * un cambio que haga el administrador se refleja al recargar sin necesidad de volver a
 * iniciar sesión.
 */
@Injectable({ providedIn: 'root' })
export class PermisosService {
  private readonly http = inject(HttpClient);

  private readonly _permisos = signal<ReadonlySet<string>>(new Set<string>());
  private readonly _roles = signal<readonly string[]>([]);
  private readonly _cargado = signal(false);

  readonly permisos = this._permisos.asReadonly();
  readonly roles = this._roles.asReadonly();
  readonly cargado = this._cargado.asReadonly();
  readonly cantidad = computed(() => this._permisos().size);

  /** Pide los permisos al backend. Se llama al entrar y después de iniciar sesión. */
  cargar(): Observable<MisPermisos> {
    return this.http.get<MisPermisos>(`${environment.apiUrl}/seguridad/mis-permisos`).pipe(
      tap((respuesta) => {
        this._permisos.set(new Set(respuesta.permisos));
        this._roles.set(respuesta.roles);
        this._cargado.set(true);
      }),
    );
  }

  /** Igual que `cargar`, pero pensado para los guards: solo interesa que ya estén. */
  asegurarCargados(): Observable<boolean> {
    if (this._cargado()) {
      return new Observable<boolean>((observador) => {
        observador.next(true);
        observador.complete();
      });
    }
    return this.cargar().pipe(map(() => true));
  }

  /** ¿El usuario tiene este permiso? */
  tiene(codigo: string): boolean {
    return this._permisos().has(codigo);
  }

  /** ¿Tiene al menos uno de estos permisos? */
  tieneAlguno(codigos: readonly string[]): boolean {
    return codigos.some((codigo) => this.tiene(codigo));
  }

  limpiar(): void {
    this._permisos.set(new Set<string>());
    this._roles.set([]);
    this._cargado.set(false);
  }
}
