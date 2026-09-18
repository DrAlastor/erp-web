import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RespuestaLogin, Sesion } from './auth.model';

const CLAVE_SESION = 'erp_sesion';

/**
 * Sesión del usuario: iniciar, cerrar y saber si hay alguien dentro.
 *
 * La aplicación se renderiza también en el servidor (SSR), donde `localStorage` no existe.
 * Por eso todo acceso al almacenamiento pasa por `leerSesion` / `guardarSesion`, que
 * comprueban la plataforma y toleran que el navegador tenga el almacenamiento bloqueado.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly _sesion = signal<Sesion | null>(this.leerSesion());

  readonly sesion = this._sesion.asReadonly();
  readonly estaAutenticado = computed(() => this._sesion() !== null);
  readonly nombre = computed(() => this._sesion()?.nombre ?? null);

  login(email: string, password: string): Observable<RespuestaLogin> {
    return this.http
      .post<RespuestaLogin>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(
        tap((respuesta) => {
          const sesion: Sesion = {
            token: respuesta.token,
            nombre: respuesta.nombre,
            empresaId: respuesta.empresaId,
          };
          this.guardarSesion(sesion);
          this._sesion.set(sesion);
        }),
      );
  }

  logout(): void {
    this.borrarSesion();
    this._sesion.set(null);
  }

  /** Token actual, o null si no hay sesión. Lo usa el interceptor. */
  token(): string | null {
    return this._sesion()?.token ?? null;
  }

  private leerSesion(): Sesion | null {
    if (!this.esNavegador) {
      return null;
    }
    try {
      const guardado = localStorage.getItem(CLAVE_SESION);
      return guardado ? (JSON.parse(guardado) as Sesion) : null;
    } catch {
      return null;
    }
  }

  private guardarSesion(sesion: Sesion): void {
    if (!this.esNavegador) {
      return;
    }
    try {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // Si el navegador no deja guardar, la sesión vive solo en memoria.
    }
  }

  private borrarSesion(): void {
    if (!this.esNavegador) {
      return;
    }
    try {
      localStorage.removeItem(CLAVE_SESION);
    } catch {
      // Nada que hacer: la sesión en memoria ya se limpió.
    }
  }
}
