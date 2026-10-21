import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  CatalogoQbe,
  DefinicionQbe,
  FormatoExportacion,
  PlantillaQbe,
  PlantillaQbeRequest,
  ResultadoQbe
} from '../models/reporte-qbe.model';

@Injectable({ providedIn: 'root' })
export class ReporteQbeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reportes/qbe`;

  /** Entidades, campos, operadores por tipo de dato y agregaciones disponibles. */
  catalogo(): Observable<CatalogoQbe> {
    return this.http.get<CatalogoQbe>(`${this.baseUrl}/catalogo`);
  }

  /** Ejecuta la definición y devuelve una página del resultado. */
  vistaPrevia(definicion: DefinicionQbe, pagina: number, tamanio: number): Observable<ResultadoQbe> {
    const params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    return this.http.post<ResultadoQbe>(`${this.baseUrl}/vista-previa`, definicion, { params });
  }

  exportar(definicion: DefinicionQbe, formato: FormatoExportacion, nombre?: string): Observable<Blob> {
    let params = new HttpParams().set('formato', formato);
    if (nombre) params = params.set('nombre', nombre);
    return this.http.post(`${this.baseUrl}/exportar`, definicion, { params, responseType: 'blob' });
  }

  listarPlantillas(): Observable<PlantillaQbe[]> {
    return this.http.get<PlantillaQbe[]>(`${this.baseUrl}/plantillas`);
  }

  crearPlantilla(request: PlantillaQbeRequest): Observable<PlantillaQbe> {
    return this.http.post<PlantillaQbe>(`${this.baseUrl}/plantillas`, request);
  }

  actualizarPlantilla(id: number, request: PlantillaQbeRequest): Observable<PlantillaQbe> {
    return this.http.put<PlantillaQbe>(`${this.baseUrl}/plantillas/${id}`, request);
  }

  eliminarPlantilla(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/plantillas/${id}`);
  }
}
