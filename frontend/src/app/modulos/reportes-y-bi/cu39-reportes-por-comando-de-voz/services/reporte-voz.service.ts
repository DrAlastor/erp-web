import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AsistenteRespuesta,
  ConsultaReporteVoz,
  OpcionesReporteVoz,
  ReporteVoz,
  TurnoConversacion
} from '../models/reporte-voz.model';

@Injectable({ providedIn: 'root' })
export class ReporteVozService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reportes/voz`;

  /** Métricas, agrupaciones, sucursales y frases de ejemplo. */
  opciones(): Observable<OpcionesReporteVoz> {
    return this.http.get<OpcionesReporteVoz>(`${this.baseUrl}/opciones`);
  }

  /** Envía el mensaje al asistente con el historial y el último reporte, para el seguimiento. */
  conversar(
    mensaje: string,
    historial: TurnoConversacion[],
    consultaAnterior: ConsultaReporteVoz | null
  ): Observable<AsistenteRespuesta> {
    return this.http.post<AsistenteRespuesta>(`${this.baseUrl}/conversar`, { mensaje, historial, consultaAnterior });
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
