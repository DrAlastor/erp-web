import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface BitacoraEvento {
  id: number;
  evento_id: string;
  fecha_hora_utc: string;
  fecha_hora_bolivia?: string;
  empresa_id?: number;
  codigo_empresa?: string;
  razon_social?: string;
  actor: string;
  tipo_operacion: string;
  modulo: string;
  detalle: string;
  ip_origen: string;
  resultado: string; // COMPLETADO, FALLIDO, RECHAZADO, EXITO, VERIFICADO
  severidad?: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  cloudwatch_ingested?: boolean;
  hash_previo: string;
  hash_actual: string;
  metadatos?: string;
}

export interface VerificacionCadena {
  valida: boolean;
  algoritmo?: string;
  totalBloques?: number;
  ultimoHash?: string;
  bloqueInconsistente?: number;
  eventoId?: string;
  mensaje: string;
}

export interface CloudWatchStatus {
  conectado: boolean;
  habilitado: boolean;
  region: string;
  logGroup: string;
  inmutableWORM?: boolean;
  almacenamientoExterno?: string;
  mensaje: string;
  streamsVerificados?: string[];
}

export interface BitacoraFiltros {
  empresaId?: number;
  modulo?: string;
  tipoOperacion?: string;
  severidad?: string;
  resultado?: string;
  busqueda?: string;
  desde?: string;
  hasta?: string;
}

@Injectable({ providedIn: 'root' })
export class BitacoraService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/bitacora`;
  private readonly superAdminUrl = `${environment.apiUrl}/superadmin/bitacora`;

  /** Nivel 1: Bitácora del Tenant */
  listarEventosTenant(filtros?: BitacoraFiltros): Observable<BitacoraEvento[]> {
    const params = this.construirParams(filtros);
    return this.http.get<BitacoraEvento[]>(this.baseUrl, { params });
  }

  verificarIntegridadCadena(): Observable<VerificacionCadena> {
    return this.http.get<VerificacionCadena>(`${this.baseUrl}/verificar-integridad`);
  }

  obtenerEstadoCloudWatch(): Observable<CloudWatchStatus> {
    return this.http.get<CloudWatchStatus>(`${this.baseUrl}/cloudwatch-status`);
  }

  exportarAuditoria(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/exportar`, { responseType: 'blob' });
  }

  /** Nivel 2: Bitácora del Super Admin */
  listarEventosSuperAdmin(filtros?: BitacoraFiltros): Observable<BitacoraEvento[]> {
    const params = this.construirParams(filtros);
    return this.http.get<BitacoraEvento[]>(`${this.superAdminUrl}/inmutable`, { params });
  }

  obtenerEstadoCloudWatchSuperAdmin(): Observable<CloudWatchStatus> {
    return this.http.get<CloudWatchStatus>(`${this.superAdminUrl}/cloudwatch-status`);
  }

  verificarIntegridadSuperAdmin(): Observable<VerificacionCadena> {
    return this.http.get<VerificacionCadena>(`${this.superAdminUrl}/verificar-cadena`);
  }

  private construirParams(filtros?: BitacoraFiltros): HttpParams {
    let params = new HttpParams();
    if (!filtros) return params;

    if (filtros.empresaId) params = params.set('empresaId', filtros.empresaId.toString());
    if (filtros.modulo && filtros.modulo !== 'TODOS') params = params.set('modulo', filtros.modulo);
    if (filtros.tipoOperacion && filtros.tipoOperacion !== 'TODOS') params = params.set('tipoOperacion', filtros.tipoOperacion);
    if (filtros.severidad && filtros.severidad !== 'TODAS') params = params.set('severidad', filtros.severidad);
    if (filtros.resultado && filtros.resultado !== 'TODOS') params = params.set('resultado', filtros.resultado);
    if (filtros.busqueda) params = params.set('busqueda', filtros.busqueda.trim());
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);

    return params;
  }
}
