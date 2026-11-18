import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CajaService } from '../../cu32-cuentas-bancarias-y-cajas/services/caja.service';
import { TurnoResponse } from '../../cu32-cuentas-bancarias-y-cajas/models/caja.model';
import { CerrarTurnoRequest, FiltrosHistorialCaja, TurnoDetalleResponse, TurnoPage } from '../models/arqueo-caja.model';

/** HU-35 / CU35 — cierre, historial y detalle de turnos sobre `/api/caja` (misma base URL que CU34). */
@Injectable({ providedIn: 'root' })
export class ArqueoCajaService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(CajaService).url;

  cerrar(turnoId: number, body: CerrarTurnoRequest) {
    return this.http.post<TurnoResponse>(`${this.url}/turnos/${turnoId}/cierre`, body);
  }

  historial(filtros: FiltrosHistorialCaja, page: number, size = 10) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filtros.sucursalId) params = params.set('sucursalId', filtros.sucursalId);
    if (filtros.estado) params = params.set('estado', filtros.estado);
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);
    return this.http.get<TurnoPage>(`${this.url}/turnos`, { params });
  }

  detalle(turnoId: number) {
    return this.http.get<TurnoDetalleResponse>(`${this.url}/turnos/${turnoId}`);
  }
}
