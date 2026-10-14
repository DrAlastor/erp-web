import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  ClienteOpcion,
  Pedido,
  PedidoPage,
  PedidoRequest,
  ProductoOpcion,
  SucursalOpcion,
} from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/pedidos`;

  list(search: string, estado: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    return this.http.get<PedidoPage>(this.url, { params });
  }

  detail(id: number) {
    return this.http.get<Pedido>(`${this.url}/${id}`);
  }

  create(body: PedidoRequest) {
    return this.http.post<Pedido>(this.url, body);
  }

  update(id: number, body: PedidoRequest) {
    return this.http.put<Pedido>(`${this.url}/${id}`, body);
  }


  anular(id: number, motivo: string) {
    return this.http.patch<Pedido>(`${this.url}/${id}/anular`, { motivo });
  }

  clientes(search: string) {
    const params = new HttpParams().set('search', search);
    return this.http.get<ClienteOpcion[]>(`${this.url}/opciones/clientes`, { params });
  }

  sucursales() {
    return this.http.get<SucursalOpcion[]>(`${this.url}/opciones/sucursales`);
  }

  productos(search: string, sucursalId: number | null) {
    let params = new HttpParams().set('search', search);
    if (sucursalId) params = params.set('sucursalId', sucursalId);
    return this.http.get<ProductoOpcion[]>(`${this.url}/opciones/productos`, { params });
  }
}
