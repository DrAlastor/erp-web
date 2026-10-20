import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  NotaCredito,
  NotaCreditoPage,
  NotaCreditoRequest,
  VentaDevolucion,
} from '../models/nota-credito.model';

@Injectable({ providedIn: 'root' })
export class NotaCreditoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/notas-credito`;

  buscarVentas(numeroFactura: number | null, nitCliente: string) {
    let params = new HttpParams();
    if (numeroFactura) params = params.set('numeroFactura', numeroFactura);
    if (nitCliente) params = params.set('nitCliente', nitCliente);
    return this.http.get<VentaDevolucion[]>(`${environment.apiUrl}/ventas/buscar`, { params });
  }

  listar(search: string, page: number) {
    const params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    return this.http.get<NotaCreditoPage>(this.url, { params });
  }

  detalle(id: number) {
    return this.http.get<NotaCredito>(`${this.url}/${id}`);
  }

  emitir(body: NotaCreditoRequest) {
    return this.http.post<NotaCredito>(this.url, body);
  }
}
