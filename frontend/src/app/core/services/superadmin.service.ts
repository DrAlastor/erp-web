import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SuperAdminLoginRequest {
  email: string;
  password: string;
}

export interface SuperAdminLoginResponse {
  token: string;
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  rolGlobal: string;
}

export interface TenantMonitorItem {
  id: number;
  codigoEmpresa: string;
  razonSocial: string;
  nombreComercial: string;
  nit: string;
  email: string;
  ciudadNombre: string;
  paisNombre: string;
  estadoEmpresa: string;
  fechaCreacion: string | null;
  fechaSuspension: string | null;
  nombreDatabase: string;
  host: string;
  port: number;
  estadoDb: string;
  versionEsquema: string;
  ultimaMigracion: string | null;
  planCodigo: string;
  planNombre: string;
  estadoSuscripcion: string;
  esPrueba: boolean;
  fechaFin: string | null;
}

export interface BackupItem {
  id: number;
  empresaId: number;
  codigoEmpresa: string;
  razonSocial: string;
  nombreDatabase: string;
  nombreArchivo: string;
  rutaStorage: string;
  tamanoBytes: number;
  tamanoFormateado?: string;
  estado: string;
  tipoBackup?: string;
  solicitadoPor?: string;
  checksumSha256?: string;
  jobId?: string;
  estadoVerificacion?: string;
  fechaCreacion: string;
}

export interface BitacoraItem {
  id: number;
  fecha: string;
  accion: string;
  modulo: string;
  detalle: string;
  usuario: string;
  codigoEmpresa: string;
  estado: string;
}

export interface BitacoraInmutableItem {
  id: number;
  evento_id: string;
  fecha_hora_utc: string;
  fecha_hora_bolivia?: string;
  empresa_id: number;
  codigo_empresa: string;
  actor: string;
  tipo_operacion: string;
  modulo: string;
  detalle: string;
  ip_origen: string;
  resultado: string;
  severidad?: string;
  cloudwatch_ingested?: boolean;
  hash_previo: string;
  hash_actual: string;
  metadatos?: string;
}


const SUPERADMIN_SESSION_KEY = 'erp_superadmin_session';
const SUPERADMIN_TOKEN_KEY = 'erp_superadmin_token';

@Injectable({ providedIn: 'root' })
export class SuperAdminService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly currentAdmin = signal<SuperAdminLoginResponse | null>(this.readStoredSession());

  login(request: SuperAdminLoginRequest): Observable<SuperAdminLoginResponse> {
    return this.http.post<SuperAdminLoginResponse>(`${environment.apiUrl}/superadmin/auth/login`, request).pipe(
      tap(response => {
        this.saveSession(response);
      })
    );
  }

  private isBrowser(): boolean {
    return typeof window !== 'undefined';
  }

  logout(): void {
    if (this.isBrowser()) {
      sessionStorage.removeItem(SUPERADMIN_TOKEN_KEY);
      sessionStorage.removeItem(SUPERADMIN_SESSION_KEY);
      localStorage.removeItem(SUPERADMIN_TOKEN_KEY);
      localStorage.removeItem(SUPERADMIN_SESSION_KEY);
    }
    this.currentAdmin.set(null);
    this.router.navigate(['/superadmin/login']);
  }

  isAuthenticated(): boolean {
    if (!this.isBrowser()) return false;
    const token = sessionStorage.getItem(SUPERADMIN_TOKEN_KEY) || localStorage.getItem(SUPERADMIN_TOKEN_KEY);
    return !!token && !!this.currentAdmin();
  }

  getToken(): string | null {
    if (!this.isBrowser()) return null;
    return sessionStorage.getItem(SUPERADMIN_TOKEN_KEY) || localStorage.getItem(SUPERADMIN_TOKEN_KEY);
  }

  listarTenants(): Observable<TenantMonitorItem[]> {
    return this.http.get<TenantMonitorItem[]>(`${environment.apiUrl}/superadmin/tenants`);
  }

  suspenderTenant(empresaId: number): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${environment.apiUrl}/superadmin/tenants/${empresaId}/suspender`, {});
  }

  reactivarTenant(empresaId: number): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${environment.apiUrl}/superadmin/tenants/${empresaId}/reactivar`, {});
  }

  listarBackups(): Observable<BackupItem[]> {
    return this.http.get<BackupItem[]>(`${environment.apiUrl}/superadmin/backups`);
  }

  generarBackup(empresaId: number): Observable<BackupItem> {
    return this.http.post<BackupItem>(`${environment.apiUrl}/superadmin/backups/generar`, { empresaId });
  }

  listarBitacora(): Observable<BitacoraItem[]> {
    return this.http.get<BitacoraItem[]>(`${environment.apiUrl}/superadmin/bitacora`);
  }

  verificarIntegridadBackup(id: number): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}/superadmin/backups/${id}/verificar`);
  }

  restaurarBackup(id: number, motivo?: string): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}/superadmin/backups/${id}/restaurar`, {
      tipoRestauracion: 'COMPLETA',
      confirmacion: true,
      motivo: motivo || 'Restauración autorizada por Super Administrador'
    });
  }

  listarBitacoraInmutable(filtros?: any): Observable<BitacoraInmutableItem[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.empresaId) params = params.set('empresaId', filtros.empresaId.toString());
      if (filtros.modulo && filtros.modulo !== 'TODOS') params = params.set('modulo', filtros.modulo);
      if (filtros.tipoOperacion && filtros.tipoOperacion !== 'TODOS') params = params.set('tipoOperacion', filtros.tipoOperacion);
      if (filtros.severidad && filtros.severidad !== 'TODAS') params = params.set('severidad', filtros.severidad);
      if (filtros.resultado && filtros.resultado !== 'TODOS') params = params.set('resultado', filtros.resultado);
      if (filtros.busqueda) params = params.set('busqueda', filtros.busqueda.trim());
      if (filtros.desde) params = params.set('desde', filtros.desde);
      if (filtros.hasta) params = params.set('hasta', filtros.hasta);
    }
    return this.http.get<BitacoraInmutableItem[]>(`${environment.apiUrl}/superadmin/bitacora/inmutable`, { params });
  }

  obtenerEstadoCloudWatch(): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}/superadmin/bitacora/cloudwatch-status`);
  }

  verificarCadenaBitacora(): Observable<{ valida: boolean; mensaje: string; totalBloques?: number; ultimoHash?: string }> {
    return this.http.get<{ valida: boolean; mensaje: string; totalBloques?: number; ultimoHash?: string }>(`${environment.apiUrl}/superadmin/bitacora/verificar-cadena`);
  }


  private saveSession(response: SuperAdminLoginResponse): void {
    if (this.isBrowser()) {
      sessionStorage.setItem(SUPERADMIN_TOKEN_KEY, response.token);
      sessionStorage.setItem(SUPERADMIN_SESSION_KEY, JSON.stringify(response));
    }
    this.currentAdmin.set(response);
  }

  private readStoredSession(): SuperAdminLoginResponse | null {
    if (!this.isBrowser()) return null;
    try {
      const data = sessionStorage.getItem(SUPERADMIN_SESSION_KEY) || localStorage.getItem(SUPERADMIN_SESSION_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }
}
