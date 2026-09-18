import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Articulo } from '../models/articulo.model';

@Injectable({ providedIn: 'root' })
export class ArticuloService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/articulos`;

  listarTodos(): Observable<Articulo[]> {
    return this.http.get<Articulo[]>(this.baseUrl);
  }

  buscarPorId(id: number): Observable<Articulo> {
    return this.http.get<Articulo>(`${this.baseUrl}/${id}`);
  }

  listarPorCategoria(categoriaId: number): Observable<Articulo[]> {
    return this.http.get<Articulo[]>(this.baseUrl, {
      params: { categoriaId: String(categoriaId) }
    });
  }
}
