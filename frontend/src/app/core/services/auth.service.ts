import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RefreshTokenRequest, TokenResponse } from '../models/auth.model';
import { UserSession } from '../models/user-session.model';

const STORAGE_KEY = 'erp_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSignal = signal<UserSession | null>(this.readStoredSession());

  readonly usuario = computed(() => this.sessionSignal()?.usuario ?? null);
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {}

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.persistSession(response)));
  }

  refresh(): Observable<TokenResponse> {
    const refreshToken = this.sessionSignal()?.refreshToken;
    if (!refreshToken) {
      throw new Error('No hay sesión activa para refrescar');
    }
    const body: RefreshTokenRequest = { refreshToken };
    return this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/refresh`, body)
      .pipe(tap((response) => this.persistSession(response)));
  }

  logout(): void {
    const refreshToken = this.sessionSignal()?.refreshToken;
    this.clearSession();
    if (refreshToken) {
      this.http
        .post(`${environment.apiUrl}/auth/logout`, { refreshToken } as RefreshTokenRequest)
        .subscribe({ error: () => {} });
    }
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return this.sessionSignal()?.accessToken ?? null;
  }

  getRefreshToken(): string | null {
    return this.sessionSignal()?.refreshToken ?? null;
  }

  hasPermission(_permission: string): boolean {
    // HU-01 todavía no emite permisos granulares en el JWT (eso llega con HU-03).
    // Hasta entonces, cualquier usuario autenticado pasa el chequeo.
    return this.isAuthenticated();
  }

  private persistSession(response: TokenResponse): void {
    const session: UserSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      usuario: response.usuario,
    };
    this.sessionSignal.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Almacenamiento no disponible (p. ej. modo incógnito estricto); la sesión sigue viva en memoria.
    }
  }

  private clearSession(): void {
    this.sessionSignal.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignorado
    }
  }

  private readStoredSession(): UserSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as UserSession) : null;
    } catch {
      return null;
    }
  }
}
