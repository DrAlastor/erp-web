import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { DashboardEjecutivoResponse, SucursalOpcion } from '../models/dashboard-ejecutivo.model';

/** HU-36 / CU39 — cliente HTTP del Dashboard Ejecutivo (REPORTES_CONSULTAR). */
@Injectable({ providedIn: 'root' })
export class DashboardEjecutivoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/dashboard/ejecutivo`;

  obtener(sucursalId: number | null) {
    let params = new HttpParams();
    if (sucursalId) params = params.set('sucursalId', sucursalId);
    return this.http.get<DashboardEjecutivoResponse>(this.url, { params });
  }

  /**
   * Sucursales activas para el selector (REPORTES_CONSULTAR). Si la llamada falla,
   * el selector se oculta y el dashboard muestra todas las sucursales.
   */
  sucursales() {
    return this.http.get<SucursalOpcion[]>(`${environment.apiUrl}/dashboard/ejecutivo/opciones/sucursales`);
  }
}
