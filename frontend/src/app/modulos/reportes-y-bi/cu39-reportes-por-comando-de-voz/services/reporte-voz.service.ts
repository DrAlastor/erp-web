import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ConsultaReporteVoz, OpcionesReporteVoz, ReporteVoz } from '../models/reporte-voz.model';

@Injectable({ providedIn: 'root' })
export class ReporteVozService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reportes/voz`;

  /** Métricas, agrupaciones, sucursales y frases de ejemplo. */
  opciones(): Observable<OpcionesReporteVoz> {
    return this.http.get<OpcionesReporteVoz>(`${this.baseUrl}/opciones`);
  }

  /** Envía la frase transcrita para que N8N la interprete y devuelve el reporte. */
  interpretar(texto: string): Observable<ReporteVoz> {
    return this.http.post<ReporteVoz>(`${this.baseUrl}/interpretar`, { texto });
  }

  /** Ejecuta una consulta ya estructurada (refinar el reporte). */
  ejecutar(consulta: ConsultaReporteVoz): Observable<ReporteVoz> {
    return this.http.post<ReporteVoz>(`${this.baseUrl}/ejecutar`, consulta);
  }

  /** El reporte de la consulta, como CSV. */
  exportar(consulta: ConsultaReporteVoz): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/exportar`, consulta, { responseType: 'blob' });
  }
}
