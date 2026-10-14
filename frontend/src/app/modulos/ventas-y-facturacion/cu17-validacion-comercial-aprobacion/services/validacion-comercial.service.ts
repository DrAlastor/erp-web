import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { PageResult, PedidoResumen, ValidacionPedido } from '../models/validacion-comercial.model';

@Injectable({ providedIn: 'root' })
export class ValidacionComercialService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/validacion-comercial`;

  /** Bandeja de pedidos por estado; TODOS devuelve el historial completo. */
  listar(estado: string, page = 0, size = 10): Observable<PageResult<PedidoResumen>> {
    const params = new HttpParams().set('estado', estado).set('page', page).set('size', size);
    return this.http.get<PageResult<PedidoResumen>>(`${this.baseUrl}/pedidos`, { params });
  }

  /** Pedido con el resultado de cada regla comercial, calculado en este momento. */
  evaluar(pedidoId: number): Observable<ValidacionPedido> {
    return this.http.get<ValidacionPedido>(`${this.baseUrl}/pedidos/${pedidoId}`);
  }

  aprobar(pedidoId: number, observacion: string): Observable<ValidacionPedido> {
    return this.http.post<ValidacionPedido>(`${this.baseUrl}/pedidos/${pedidoId}/aprobar`, { observacion });
  }

  rechazar(pedidoId: number, observacion: string): Observable<ValidacionPedido> {
    return this.http.post<ValidacionPedido>(`${this.baseUrl}/pedidos/${pedidoId}/rechazar`, { observacion });
  }
}
