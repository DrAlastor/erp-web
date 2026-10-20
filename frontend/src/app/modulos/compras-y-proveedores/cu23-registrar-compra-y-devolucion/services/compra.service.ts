import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  Catalogos, Compra, Devolucion, RegistrarCompraDto, RegistrarDevolucionDto
} from '../models/compra.model';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/compras`;

  catalogos(): Observable<Catalogos> {
    return this.http.get<Catalogos>(`${this.baseUrl}/catalogos`);
  }

  listar(filtros?: { proveedorId?: number; estado?: string; desde?: string; hasta?: string; search?: string }): Observable<Compra[]> {
    let params = new HttpParams();
    if (filtros?.proveedorId != null) params = params.set('proveedorId', String(filtros.proveedorId));
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.desde) params = params.set('desde', filtros.desde);
    if (filtros?.hasta) params = params.set('hasta', filtros.hasta);
    if (filtros?.search) params = params.set('search', filtros.search);
    return this.http.get<Compra[]>(this.baseUrl, { params });
  }

  obtener(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.baseUrl}/${id}`);
  }

  registrar(dto: RegistrarCompraDto): Observable<Compra> {
    return this.http.post<Compra>(this.baseUrl, dto);
  }

  anular(id: number, motivo: string): Observable<Compra> {
    return this.http.post<Compra>(`${this.baseUrl}/${id}/anular`, { motivo });
  }

  listarDevoluciones(compraId?: number): Observable<Devolucion[]> {
    let params = new HttpParams();
    if (compraId != null) params = params.set('compraId', String(compraId));
    return this.http.get<Devolucion[]>(`${this.baseUrl}/devoluciones`, { params });
  }

  registrarDevolucion(compraId: number, dto: RegistrarDevolucionDto): Observable<Devolucion> {
    return this.http.post<Devolucion>(`${this.baseUrl}/${compraId}/devoluciones`, dto);
  }
}
