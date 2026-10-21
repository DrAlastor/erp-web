import { PermisosService } from '../../cu03-gestionar-roles-y-permisos/services/permisos.service';
import { AparienciaService } from '../../cu06-personalizar-apariencia/services/apariencia.service';
﻿import { Injectable, computed, signal, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, defer, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ClienteRegisterRequest, LoginRequest, RefreshTokenRequest, TokenResponse } from '../models/auth.model';
import { UserSession } from '../models/user-session.model';

const STORAGE_KEY = 'erp_session';
interface TokenClaims { exp: number; authorities?: string[]; roles?: string[]; codigoEmpresa?: string; tenantId?: number; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly permisos = inject(PermisosService);
  private readonly apariencia = inject(AparienciaService);
  private rememberMe = false;
  private readonly sessionSignal = signal<UserSession | null>(this.readStoredSession());
  private refreshRequest?: Observable<TokenResponse>;
  readonly usuario = computed(() => this.sessionSignal()?.usuario ?? null);

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  isAuthenticated(): boolean {
    return (this.tokenClaims()?.exp ?? 0) * 1000 > Date.now();
  }

  login(request: LoginRequest, rememberMe = false): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap(response => {
        this.rememberMe = rememberMe;
        this.persistSession(response, request.codigoEmpresa);
      })
    );
  }

  registerCliente(request: ClienteRegisterRequest, rememberMe = false): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/register/cliente`, request).pipe(
      tap(response => {
        this.rememberMe = rememberMe;
        this.persistSession(response, request.codigoEmpresa);
      }),
    );
  }

  refresh(): Observable<TokenResponse> {
    if (this.refreshRequest) return this.refreshRequest;
    const session = this.sessionSignal();
    if (!session?.refreshToken) return throwError(() => new Error('No hay sesión activa para refrescar'));
    const body: RefreshTokenRequest = {
      codigoEmpresa: session.codigoEmpresa,
      refreshToken: session.refreshToken,
    };
    const pending = this.http.post<TokenResponse>(`${environment.apiUrl}/auth/refresh`, body).pipe(
      tap(response => {
        // Una respuesta tardia no debe restaurar una sesion cerrada o reemplazar otro login.
        if (this.sessionSignal() !== session) throw new Error('La sesión ha cambiado');
        this.persistSession(response, session.codigoEmpresa);
      }),
      catchError(error => {
        if (this.sessionSignal() === session && error instanceof HttpErrorResponse &&
            (error.status === 401 || error.status === 403)) this.clearSession();
        return throwError(() => error);
      }),
      finalize(() => { if (this.refreshRequest === pending) this.refreshRequest = undefined; }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.refreshRequest = pending;
    return pending;
  }

  getValidAccessToken(): Observable<string | null> {
    return defer(() => {
      if (!this.sessionSignal()) return of(null);
      if (this.isAuthenticated()) return of(this.getAccessToken());
      const refreshToken = this.getRefreshToken();
      return this.refresh().pipe(
        map(response => response.accessToken),
        catchError(error => {
          if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403) &&
              (!this.getRefreshToken() || this.getRefreshToken() === refreshToken)) this.logout();
          return throwError(() => error);
        })
      );
    });
  }

  logout(): void {
    const session = this.sessionSignal();
    this.clearSession();
    if (session?.refreshToken && session.codigoEmpresa) {
      this.http.post(`${environment.apiUrl}/auth/logout`, {
        codigoEmpresa: session.codigoEmpresa,
        refreshToken: session.refreshToken,
      }).subscribe({ error: () => {} });
    }
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null { return this.sessionSignal()?.accessToken ?? null; }
  getRefreshToken(): string | null { return this.sessionSignal()?.refreshToken ?? null; }

  hasPermission(permission: string): boolean {
    // Esto controla la interfaz; la autorizacion real se verifica en el backend.
    const authorities = this.tokenClaims()?.authorities;
    return this.isAuthenticated() && Array.isArray(authorities) && authorities.includes(permission);
  }

  isClient(): boolean {
    const roles = this.tokenClaims()?.roles;
    return this.isAuthenticated() && Array.isArray(roles) && roles.includes('CLIENTE');
  }

  private tokenClaims(): TokenClaims | null {
    return this.tokenClaimsFrom(this.getAccessToken());
  }

  private tokenClaimsFrom(token: string | null): TokenClaims | null {
    try {
      const payload = token?.split('.')[1];
      if (!payload) return null;
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const claims: unknown = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')));
      if (!claims || typeof claims !== 'object' || !('exp' in claims) ||
          typeof claims.exp !== 'number' || !Number.isFinite(claims.exp)) return null;
      return claims as TokenClaims;
    } catch { return null; }
  }

  private persistSession(response: TokenResponse, codigoEmpresa?: string): void {
    this.permisos.limpiar();
    const claims = this.tokenClaimsFrom(response.accessToken);
    const resolvedCompanyCode = (
      codigoEmpresa ||
      claims?.codigoEmpresa ||
      response.codigoEmpresa ||
      'ASUS-7K4P2M'
    ).trim().toUpperCase();

    const session: UserSession = {
      codigoEmpresa: resolvedCompanyCode,
      accessToken: response.accessToken, refreshToken: response.refreshToken, usuario: response.usuario
    };
    this.sessionSignal.set(session);
    try {
      localStorage.removeItem(STORAGE_KEY);
      sessionStorage.removeItem(STORAGE_KEY);
      (this.rememberMe ? localStorage : sessionStorage).setItem(STORAGE_KEY, JSON.stringify(session));
    } catch { /* La sesion sigue disponible en memoria si el almacenamiento no esta disponible. */ }
  }

  private clearSession(): void {
    this.permisos.limpiar();
    // CU06: la apariencia es de la empresa de esta sesión; no debe quedar para la siguiente.
    this.apariencia.limpiar();
    this.sessionSignal.set(null);
    this.refreshRequest = undefined;
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* Almacenamiento no disponible. */ }
    try { sessionStorage.removeItem(STORAGE_KEY); } catch { /* Almacenamiento no disponible. */ }
  }

  private readStoredSession(): UserSession | null {
    try {
      const temporary = sessionStorage.getItem(STORAGE_KEY);
      const stored = temporary ?? localStorage.getItem(STORAGE_KEY);
      this.rememberMe = !temporary && !!stored;
      if (!stored) return null;
      const session = JSON.parse(stored) as Partial<UserSession> | null;
      return session && typeof session.accessToken === 'string' &&
        typeof session.refreshToken === 'string' && typeof session.codigoEmpresa === 'string' &&
        session.codigoEmpresa.length > 0 && session.usuario &&
        typeof session.usuario.username === 'string' ? session as UserSession : null;
    } catch { return null; }
  }
}
