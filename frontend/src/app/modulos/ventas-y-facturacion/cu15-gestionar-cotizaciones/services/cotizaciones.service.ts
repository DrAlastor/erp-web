import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  Cotizacion,
  CotizacionPage,
  CotizacionRequest,
  OpcionesCotizacion,
  PedidoGenerado,
} from '../models/cotizacion.model';

@Injectable({ providedIn: 'root' })
export class CotizacionesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cotizaciones`;

  list(search: string, estado: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    return this.http.get<CotizacionPage>(this.url, { params });
  }

  options() {
    return this.http.get<OpcionesCotizacion>(`${this.url}/opciones`);
  }

  detail(id: number) {
    return this.http.get<Cotizacion>(`${this.url}/${id}`);
  }

  create(body: CotizacionRequest) {
    return this.http.post<Cotizacion>(this.url, body);
  }

  update(id: number, body: CotizacionRequest) {
    return this.http.put<Cotizacion>(`${this.url}/${id}`, body);
  }

  emit(id: number) {
    return this.http.post<Cotizacion>(`${this.url}/${id}/emitir`, {});
  }

  accept(id: number) {
    return this.http.post<Cotizacion>(`${this.url}/${id}/aceptar`, {});
  }

  cancel(id: number, motivo: string) {
    return this.http.post<Cotizacion>(`${this.url}/${id}/anular`, { motivo });
  }

  convert(id: number, sucursalId: number) {
    return this.http.post<PedidoGenerado>(`${this.url}/${id}/convertir`, { sucursalId });
  }
}
