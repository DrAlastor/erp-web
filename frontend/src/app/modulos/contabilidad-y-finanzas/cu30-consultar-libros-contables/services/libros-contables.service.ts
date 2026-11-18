import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  CuentaContableSimple,
  FiltroDiario,
  FiltroMayor,
  LibroDiarioResponse,
  LibroMayorResponse,
  PeriodoContableSimple
} from '../models/libros-contables.model';

@Injectable({ providedIn: 'root' })
export class LibrosContablesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/contabilidad/libros`;

  listarPeriodos(): Observable<PeriodoContableSimple[]> {
    return this.http.get<PeriodoContableSimple[]>(`${this.baseUrl}/periodos`);
  }

  listarCuentas(): Observable<CuentaContableSimple[]> {
    return this.http.get<CuentaContableSimple[]>(`${this.baseUrl}/cuentas`);
  }

  listarTiposAsiento(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/tipos-asiento`);
  }

  consultarLibroDiario(filtro: FiltroDiario): Observable<LibroDiarioResponse> {
    let params = new HttpParams();
    if (filtro.periodoId) params = params.set('periodoId', filtro.periodoId);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);
    if (filtro.tipoAsiento && filtro.tipoAsiento !== 'TODOS') params = params.set('tipoAsiento', filtro.tipoAsiento);
    if (filtro.estado && filtro.estado !== 'TODOS') params = params.set('estado', filtro.estado);
    if (filtro.busqueda) params = params.set('busqueda', filtro.busqueda.trim());

    return this.http.get<LibroDiarioResponse>(`${this.baseUrl}/diario`, { params });
  }

  consultarLibroMayor(filtro: FiltroMayor): Observable<LibroMayorResponse> {
    let params = new HttpParams();
    if (filtro.cuentaId) params = params.set('cuentaId', filtro.cuentaId);
    if (filtro.periodoId) params = params.set('periodoId', filtro.periodoId);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);

    return this.http.get<LibroMayorResponse>(`${this.baseUrl}/mayor`, { params });
  }

  exportarDiarioCsv(filtro: FiltroDiario): Observable<Blob> {
    let params = new HttpParams();
    if (filtro.periodoId) params = params.set('periodoId', filtro.periodoId);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);
    if (filtro.tipoAsiento && filtro.tipoAsiento !== 'TODOS') params = params.set('tipoAsiento', filtro.tipoAsiento);
    if (filtro.estado && filtro.estado !== 'TODOS') params = params.set('estado', filtro.estado);
    if (filtro.busqueda) params = params.set('busqueda', filtro.busqueda.trim());

    return this.http.get(`${this.baseUrl}/diario/exportar-csv`, {
      params,
      responseType: 'blob'
    });
  }

  exportarMayorCsv(filtro: FiltroMayor): Observable<Blob> {
    let params = new HttpParams();
    if (filtro.cuentaId) params = params.set('cuentaId', filtro.cuentaId);
    if (filtro.periodoId) params = params.set('periodoId', filtro.periodoId);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);

    return this.http.get(`${this.baseUrl}/mayor/exportar-csv`, {
      params,
      responseType: 'blob'
    });
  }
}
