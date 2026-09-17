import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, defer, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { LoginRequest, RefreshTokenRequest, TokenResponse } from '../models/auth.model';
import { UserSession } from '../models/user-session.model';

const STORAGE_KEY = 'erp_session';
interface TokenClaims { exp: number; authorities?: string[]; }

@Injectable({ providedIn: 'root' })
export class AuthService {
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
        this.persistSession(response);
      })
    );
  }

  refresh(): Observable<TokenResponse> {
    if (this.refreshRequest) return this.refreshRequest;
    const session = this.sessionSignal();
    if (!session?.refreshToken) return throwError(() => new Error('No hay sesión activa para refrescar'));
    const body: RefreshTokenRequest = { refreshToken: session.refreshToken };
    const pending = this.http.post<TokenResponse>(`${environment.apiUrl}/auth/refresh`, body).pipe(
      tap(response => {
        // Una respuesta tardia no debe restaurar una sesion cerrada o reemplazar otro login.
        if (this.sessionSignal() !== session) throw new Error('La sesión ha cambiado');
        this.persistSession(response);
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
    const refreshToken = this.getRefreshToken();
    this.clearSession();
    if (refreshToken) {
      this.http.post(`${environment.apiUrl}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
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

  private tokenClaims(): TokenClaims | null {
    try {
      const payload = this.getAccessToken()?.split('.')[1];
      if (!payload) return null;
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const claims: unknown = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')));
      if (!claims || typeof claims !== 'object' || !('exp' in claims) ||
          typeof claims.exp !== 'number' || !Number.isFinite(claims.exp)) return null;
      return claims as TokenClaims;
    } catch { return null; }
  }

  private persistSession(response: TokenResponse): void {
    const session: UserSession = {
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
        typeof session.refreshToken === 'string' && session.usuario &&
        typeof session.usuario.username === 'string' ? session as UserSession : null;
    } catch { return null; }
  }
}
