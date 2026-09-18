import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Asignacion, Permiso, Rol, UsuarioResumen } from './roles.model';

/** Acceso a los endpoints de seguridad del backend. */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/seguridad`;

  listarRoles(): Observable<Rol[]> {
    return this.http.get<Rol[]>(`${this.base}/roles`);
  }

  catalogoDePermisos(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>(`${this.base}/permisos`);
  }

  /** Reemplaza la matriz completa del rol. */
  guardarMatriz(rolId: string, permisos: string[]): Observable<Rol> {
    return this.http.put<Rol>(`${this.base}/roles/${rolId}/permisos`, { permisos });
  }

  cambiarEstado(rolId: string, activo: boolean): Observable<Rol> {
    return this.http.patch<Rol>(`${this.base}/roles/${rolId}/estado`, { activo });
  }

  usuarios(): Observable<UsuarioResumen[]> {
    return this.http.get<UsuarioResumen[]>(`${this.base}/usuarios`);
  }

  rolesDeUsuario(usuarioId: string): Observable<Asignacion[]> {
    return this.http.get<Asignacion[]>(`${this.base}/usuarios/${usuarioId}/roles`);
  }

  asignar(usuarioId: string, rolId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/usuarios/${usuarioId}/roles`, { rolId });
  }

  quitar(usuarioId: string, rolId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/usuarios/${usuarioId}/roles/${rolId}`);
  }
}
