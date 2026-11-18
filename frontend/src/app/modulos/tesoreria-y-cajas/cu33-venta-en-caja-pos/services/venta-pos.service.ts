import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { ClientePos, ProductoPos, VentaPosRequest, VentaPosResponse } from '../models/venta-pos.model';

/** CU33: la sucursal y el turno los resuelve el backend a partir del turno abierto del cajero. */
@Injectable({ providedIn: 'root' })
export class VentaPosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cajas/pos`;

  productos(search: string) {
    return this.http.get<ProductoPos[]>(`${this.url}/productos`, { params: new HttpParams().set('search', search) });
  }

  clientes(search: string) {
    return this.http.get<ClientePos[]>(`${this.url}/clientes`, { params: new HttpParams().set('search', search) });
  }

  vender(body: VentaPosRequest) {
    return this.http.post<VentaPosResponse>(`${this.url}/ventas`, body);
  }
}
