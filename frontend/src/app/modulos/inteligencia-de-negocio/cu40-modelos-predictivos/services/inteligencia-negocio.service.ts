import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  Anomalia, ClientesRiesgo, EntrenarResponse, Frecuencia, Lista, Opciones, Pronostico, ReglaAsociacion,
  Resumen, Segmentos, SugerenciaCompra,
} from '../models/inteligencia-negocio.model';

type Filtros = Record<string, string | number | null | undefined>;

/** CU40 — cliente HTTP de Inteligencia de Negocio (REPORTES_CONSULTAR; entrenar: REPORTES_MODIFICAR). */
@Injectable({ providedIn: 'root' })
export class InteligenciaNegocioService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/inteligencia-negocio`;

  resumen() {
    return this.http.get<Resumen>(`${this.url}/resumen`);
  }

  entrenar() {
    return this.http.post<EntrenarResponse>(`${this.url}/entrenar`, {});
  }

  opciones() {
    return this.http.get<Opciones>(`${this.url}/opciones`);
  }

  pronostico(frecuencia: Frecuencia, sucursalId: number | null, categoria: string | null, horizonte: number | null) {
    return this.http.get<Pronostico>(`${this.url}/pronostico`, {
      params: params({ frecuencia, sucursalId, categoria, horizonte }),
    });
  }

  clientesRiesgo(nivel: string | null, limite = 100) {
    return this.http.get<ClientesRiesgo>(`${this.url}/clientes-riesgo`, { params: params({ nivel, limite }) });
  }

  segmentos(segmento: string | null, limite = 100) {
    return this.http.get<Segmentos>(`${this.url}/segmentos`, { params: params({ segmento, limite }) });
  }

  canasta(producto: string | null, limite = 50) {
    return this.http.get<Lista<ReglaAsociacion>>(`${this.url}/canasta`, { params: params({ producto, limite }) });
  }

  anomalias(desde: string | null, hasta: string | null, sucursalId: number | null, limite = 100) {
    return this.http.get<Lista<Anomalia>>(`${this.url}/anomalias`, {
      params: params({ desde, hasta, sucursalId, limite }),
    });
  }

  sugerenciaCompra(sucursalId: number | null, categoria: string | null, limite = 100) {
    return this.http.get<Lista<SugerenciaCompra>>(`${this.url}/sugerencia-compra`, {
      params: params({ sucursalId, categoria, limite }),
    });
  }
}

/** Omite filtros vacíos para no mandar "null" como texto. */
export function params(filtros: Filtros): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(filtros)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
}
