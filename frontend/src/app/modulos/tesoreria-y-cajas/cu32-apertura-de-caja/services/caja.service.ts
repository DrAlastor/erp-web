import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { CajaTurno, CierreCaja, EstadoCaja } from '../models/caja.model';

@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cajas`;

  estado(sucursalId?: number) {
    let params = new HttpParams();
    if (sucursalId) params = params.set('sucursalId', sucursalId);
    return this.http.get<EstadoCaja>(`${this.url}/estado`, { params });
  }

  abrir(sucursalId: number, montoApertura: number, observaciones: string) {
    return this.http.post<CajaTurno>(`${this.url}/apertura`, {
      sucursalId,
      montoApertura,
      observaciones: observaciones || null,
    });
  }

  cerrar(turnoId: number, montoCierre: number, observaciones: string) {
    return this.http.post<CierreCaja>(`${this.url}/${turnoId}/cierre`, {
      montoCierre,
      observaciones: observaciones || null,
    });
  }
}
