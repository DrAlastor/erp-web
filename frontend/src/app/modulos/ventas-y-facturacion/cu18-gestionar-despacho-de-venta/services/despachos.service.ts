import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  Despacho,
  DespachoPage,
  DespachoRequest,
  PedidoPendienteAprobacion,
  PedidoPorDespachar,
  Sucursal,
} from '../models/despacho.model';

@Injectable({ providedIn: 'root' })
export class DespachosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/despachos`;

  pending(sucursalId: number | null) {
    let params = new HttpParams();
    if (sucursalId) params = params.set('sucursalId', sucursalId);
    return this.http.get<PedidoPorDespachar[]>(`${this.url}/pedidos-pendientes`, { params });
  }

  list(search: string, estado: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    return this.http.get<DespachoPage>(this.url, { params });
  }

  detail(id: number) {
    return this.http.get<Despacho>(`${this.url}/${id}`);
  }

  create(body: DespachoRequest) {
    return this.http.post<Despacho>(this.url, body);
  }

  confirm(id: number) {
    return this.http.post<Despacho>(`${this.url}/${id}/confirmar`, {});
  }

  deliver(id: number) {
    return this.http.post<Despacho>(`${this.url}/${id}/entregar`, {});
  }

  cancel(id: number, motivo: string) {
    return this.http.post<Despacho>(`${this.url}/${id}/anular`, { motivo });
  }

  branches() {
    return this.http.get<Sucursal[]>(`${environment.apiUrl}/almacenes`);
  }

  /** Provisional hasta la CU17. */
  awaitingApproval() {
    return this.http.get<PedidoPendienteAprobacion[]>(`${environment.apiUrl}/pedidos/pendientes-de-aprobacion`);
  }

  /** Provisional hasta la CU17. */
  approve(pedidoId: number) {
    return this.http.post<PedidoPendienteAprobacion>(`${environment.apiUrl}/pedidos/${pedidoId}/aprobar`, {});
  }
}
