import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  KardexConsulta,
  KardexFiltros,
  KardexValoracion,
  MetodoCosteo
} from '../models/kardex.model';

@Injectable({ providedIn: 'root' })
export class KardexService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/kardex`;

  /** Kardex valorado de un producto en un almacén, con saldo inicial, movimientos y totales. */
  consultar(filtros: KardexFiltros): Observable<KardexConsulta> {
    return this.http.get<KardexConsulta>(this.baseUrl, { params: this.parametros(filtros) });
  }

  /** Existencias valoradas de un almacén según el método de costeo. */
  valorar(almacenId: number, metodo: MetodoCosteo): Observable<KardexValoracion> {
    const params = new HttpParams().set('almacenId', almacenId).set('metodo', metodo);
    return this.http.get<KardexValoracion>(`${this.baseUrl}/valoracion`, { params });
  }

  /** El mismo Kardex de la consulta, como CSV. */
  exportar(filtros: KardexFiltros): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/exportar`, {
      params: this.parametros(filtros),
      responseType: 'blob'
    });
  }

  private parametros(f: KardexFiltros): HttpParams {
    let params = new HttpParams()
      .set('productoId', f.productoId)
      .set('almacenId', f.almacenId)
      .set('metodo', f.metodo);
    if (f.fechaInicio) params = params.set('fechaInicio', `${f.fechaInicio}T00:00:00`);
    if (f.fechaFin) params = params.set('fechaFin', `${f.fechaFin}T23:59:59`);
    if (f.tipoMovimiento) params = params.set('tipoMovimiento', f.tipoMovimiento);
    return params;
  }
}
