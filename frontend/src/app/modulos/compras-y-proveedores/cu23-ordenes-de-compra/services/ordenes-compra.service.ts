import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  OpcionesOrdenCompra,
  OrdenCompra,
  OrdenCompraPage,
  OrdenCompraRequest,
  SugerenciaReposicion,
} from '../models/orden-compra.model';

export interface FiltrosOrden {
  search: string;
  estado: string;
  proveedorId: number | null;
  desde: string;
  hasta: string;
}

@Injectable({ providedIn: 'root' })
export class OrdenesCompraService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/ordenes-compra`;

  list(f: FiltrosOrden, page: number) {
    let params = new HttpParams().set('search', f.search).set('page', page).set('size', 15);
    if (f.estado) params = params.set('estado', f.estado);
    if (f.proveedorId) params = params.set('proveedorId', f.proveedorId);
    if (f.desde) params = params.set('desde', f.desde);
    if (f.hasta) params = params.set('hasta', f.hasta);
    return this.http.get<OrdenCompraPage>(this.url, { params });
  }

  options() {
    return this.http.get<OpcionesOrdenCompra>(`${this.url}/opciones`);
  }

  suggestions(sucursalId: number) {
    return this.http.get<SugerenciaReposicion[]>(`${this.url}/sugerencias`, {
      params: new HttpParams().set('sucursalId', sucursalId),
    });
  }

  detail(id: number) {
    return this.http.get<OrdenCompra>(`${this.url}/${id}`);
  }

  create(body: OrdenCompraRequest) {
    return this.http.post<OrdenCompra>(this.url, body);
  }

  update(id: number, body: OrdenCompraRequest) {
    return this.http.put<OrdenCompra>(`${this.url}/${id}`, body);
  }

  requestApproval(id: number) {
    return this.http.post<OrdenCompra>(`${this.url}/${id}/solicitar-aprobacion`, {});
  }

  approve(id: number) {
    return this.http.post<OrdenCompra>(`${this.url}/${id}/aprobar`, {});
  }

  reject(id: number, motivo: string) {
    return this.http.post<OrdenCompra>(`${this.url}/${id}/rechazar`, { motivo });
  }

  send(id: number) {
    return this.http.post<OrdenCompra>(`${this.url}/${id}/enviar`, {});
  }

  cancel(id: number, motivo: string) {
    return this.http.post<OrdenCompra>(`${this.url}/${id}/anular`, { motivo });
  }
}
