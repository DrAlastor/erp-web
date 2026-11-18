import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  BalanceGeneralResponse,
  EstadoResultadosResponse,
  FiltroBalance,
  FiltroResultados
} from '../models/informacion-financiera.model';

@Injectable({ providedIn: 'root' })
export class InformacionFinancieraService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/contabilidad/financiera`;

  listarGestiones(): Observable<number[]> {
    return this.http.get<number[]>(`${this.baseUrl}/gestiones`);
  }

  consultarBalanceGeneral(filtro: FiltroBalance): Observable<BalanceGeneralResponse> {
    let params = new HttpParams();
    if (filtro.gestion) params = params.set('gestion', filtro.gestion);
    if (filtro.mesCorte) params = params.set('mesCorte', filtro.mesCorte);
    if (filtro.fechaCorte) params = params.set('fechaCorte', filtro.fechaCorte);
    if (filtro.nivel) params = params.set('nivel', filtro.nivel);

    return this.http.get<BalanceGeneralResponse>(`${this.baseUrl}/balance-general`, { params });
  }

  consultarEstadoResultados(filtro: FiltroResultados): Observable<EstadoResultadosResponse> {
    let params = new HttpParams();
    if (filtro.gestion) params = params.set('gestion', filtro.gestion);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);
    if (filtro.nivel) params = params.set('nivel', filtro.nivel);

    return this.http.get<EstadoResultadosResponse>(`${this.baseUrl}/estado-resultados`, { params });
  }

  exportarBalanceCsv(filtro: FiltroBalance): Observable<Blob> {
    let params = new HttpParams();
    if (filtro.gestion) params = params.set('gestion', filtro.gestion);
    if (filtro.mesCorte) params = params.set('mesCorte', filtro.mesCorte);
    if (filtro.fechaCorte) params = params.set('fechaCorte', filtro.fechaCorte);
    if (filtro.nivel) params = params.set('nivel', filtro.nivel);

    return this.http.get(`${this.baseUrl}/balance-general/exportar`, {
      params,
      responseType: 'blob'
    });
  }

  exportarEstadoResultadosCsv(filtro: FiltroResultados): Observable<Blob> {
    let params = new HttpParams();
    if (filtro.gestion) params = params.set('gestion', filtro.gestion);
    if (filtro.fechaInicio) params = params.set('fechaInicio', filtro.fechaInicio);
    if (filtro.fechaFin) params = params.set('fechaFin', filtro.fechaFin);
    if (filtro.nivel) params = params.set('nivel', filtro.nivel);

    return this.http.get(`${this.baseUrl}/estado-resultados/exportar`, {
      params,
      responseType: 'blob'
    });
  }
}
