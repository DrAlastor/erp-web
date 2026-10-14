import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Articulo, ArticuloRequest, Categoria } from '../models/articulo.model';

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

  crear(request: ArticuloRequest): Observable<Articulo> {
    return this.http.post<Articulo>(this.baseUrl, request);
  }

  actualizar(id: number, request: ArticuloRequest): Observable<Articulo> {
    return this.http.put<Articulo>(`${this.baseUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<Articulo> {
    return this.http.delete<Articulo>(`${this.baseUrl}/${id}`);
  }

  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${environment.apiUrl}/categorias`);
  }
}
