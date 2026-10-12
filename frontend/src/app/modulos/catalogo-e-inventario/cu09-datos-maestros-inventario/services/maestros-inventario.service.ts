import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  ActualizarAlmacenRequest,
  ActualizarCategoriaRequest,
  ActualizarUnidadMedidaRequest,
  Almacen,
  Categoria,
  CategoriaNodo,
  CrearAlmacenRequest,
  CrearCategoriaRequest,
  CrearUnidadMedidaRequest,
  CiudadItem,
  SucursalItem,
  UnidadMedida
} from '../models/maestros-inventario.model';

@Injectable({
  providedIn: 'root'
})
export class MaestrosInventarioService {
  private readonly http = inject(HttpClient);
  private readonly almacenesUrl = `${environment.apiUrl}/almacenes`;
  private readonly categoriasUrl = `${environment.apiUrl}/categorias`;
  private readonly unidadesUrl = `${environment.apiUrl}/unidades-medida`;
  private readonly sucursalesUrl = `${environment.apiUrl}/empleados/sucursales`;

  // ==========================================
  // SUCURSALES (Para Almacenes)
  // ==========================================
  listarSucursales(): Observable<SucursalItem[]> {
    return this.http.get<SucursalItem[]>(this.sucursalesUrl);
  }

  listarCiudades(): Observable<CiudadItem[]> {
    return this.http.get<CiudadItem[]>(`${this.almacenesUrl}/ciudades`);
  }

  // ==========================================
  // ALMACENES
  // ==========================================
  listarAlmacenes(): Observable<Almacen[]> {
    return this.http.get<Almacen[]>(this.almacenesUrl);
  }

  obtenerAlmacen(id: number): Observable<Almacen> {
    return this.http.get<Almacen>(`${this.almacenesUrl}/${id}`);
  }

  crearAlmacen(data: CrearAlmacenRequest): Observable<Almacen> {
    return this.http.post<Almacen>(this.almacenesUrl, data);
  }

  actualizarAlmacen(id: number, data: ActualizarAlmacenRequest): Observable<Almacen> {
    return this.http.put<Almacen>(`${this.almacenesUrl}/${id}`, data);
  }

  cambiarEstadoAlmacen(id: number, activo: boolean): Observable<Almacen> {
    return this.http.patch<Almacen>(`${this.almacenesUrl}/${id}/estado`, { activo });
  }

  eliminarAlmacen(id: number): Observable<void> {
    return this.http.delete<void>(`${this.almacenesUrl}/${id}`);
  }

  // ==========================================
  // CATEGORÍAS (Árbol y Lista)
  // ==========================================
  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.categoriasUrl);
  }

  obtenerArbol(): Observable<CategoriaNodo[]> {
    return this.http.get<CategoriaNodo[]>(`${this.categoriasUrl}/arbol`);
  }

  obtenerCategoria(id: number): Observable<Categoria> {
    return this.http.get<Categoria>(`${this.categoriasUrl}/${id}`);
  }

  crearCategoria(data: CrearCategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.categoriasUrl, data);
  }

  actualizarCategoria(id: number, data: ActualizarCategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.categoriasUrl}/${id}`, data);
  }

  cambiarEstadoCategoria(id: number, activo: boolean): Observable<Categoria> {
    return this.http.patch<Categoria>(`${this.categoriasUrl}/${id}/estado`, { activo });
  }

  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.categoriasUrl}/${id}`);
  }

  // ==========================================
  // UNIDADES DE MEDIDA
  // ==========================================
  listarUnidades(): Observable<UnidadMedida[]> {
    return this.http.get<UnidadMedida[]>(this.unidadesUrl);
  }

  obtenerUnidad(id: number): Observable<UnidadMedida> {
    return this.http.get<UnidadMedida>(`${this.unidadesUrl}/${id}`);
  }

  crearUnidad(data: CrearUnidadMedidaRequest): Observable<UnidadMedida> {
    return this.http.post<UnidadMedida>(this.unidadesUrl, data);
  }

  actualizarUnidad(id: number, data: ActualizarUnidadMedidaRequest): Observable<UnidadMedida> {
    return this.http.put<UnidadMedida>(`${this.unidadesUrl}/${id}`, data);
  }

  cambiarEstadoUnidad(id: number, activo: boolean): Observable<UnidadMedida> {
    return this.http.patch<UnidadMedida>(`${this.unidadesUrl}/${id}/estado`, { activo });
  }

  eliminarUnidad(id: number): Observable<void> {
    return this.http.delete<void>(`${this.unidadesUrl}/${id}`);
  }
}
