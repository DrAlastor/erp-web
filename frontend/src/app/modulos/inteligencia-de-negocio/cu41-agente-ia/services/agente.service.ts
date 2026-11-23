import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  ConversacionDetalle, ConversacionResumen, EstadoAgente, GraficoDatos, GraficoGuardado, RespuestaAgente,
} from '../models/agente.model';

/** CU41 — cliente HTTP del agente de IA (REPORTES_CONSULTAR). */
@Injectable({ providedIn: 'root' })
export class AgenteService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/inteligencia-negocio/agente`;

  estado() {
    return this.http.get<EstadoAgente>(`${this.url}/estado`);
  }

  preguntar(pregunta: string, idConversacion: number | null) {
    return this.http.post<RespuestaAgente>(`${this.url}/preguntar`, { pregunta, idConversacion });
  }

  conversaciones() {
    return this.http.get<ConversacionResumen[]>(`${this.url}/conversaciones`);
  }

  conversacion(id: number) {
    return this.http.get<ConversacionDetalle>(`${this.url}/conversaciones/${id}`);
  }

  borrarConversacion(id: number) {
    return this.http.delete<void>(`${this.url}/conversaciones/${id}`);
  }

  guardarGrafico(herramienta: string, parametros: Record<string, unknown>, titulo: string | null) {
    return this.http.post<GraficoGuardado>(`${this.url}/graficos`, { herramienta, parametros, titulo });
  }

  graficos() {
    return this.http.get<GraficoGuardado[]>(`${this.url}/graficos`);
  }

  datosGrafico(id: number) {
    return this.http.get<GraficoDatos>(`${this.url}/graficos/${id}/datos`);
  }

  borrarGrafico(id: number) {
    return this.http.delete<void>(`${this.url}/graficos/${id}`);
  }
}
