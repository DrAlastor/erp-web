import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AlertaStockMinimoResponse,
  AlmacenSimple,
  MovimientoInventarioRequest,
  MovimientoInventarioResponse,
  PageResult,
  ProductoSimple,
  StockAlmacenResponse
} from '../models/movimiento-inventario.model';

@Injectable({ providedIn: 'root' })
export class MovimientoInventarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventario`;

  /**
   * Registra un nuevo movimiento en el Kardex y actualiza el stock automáticamente.
   */
  registrarMovimiento(request: MovimientoInventarioRequest): Observable<MovimientoInventarioResponse> {
    return this.http.post<MovimientoInventarioResponse>(`${this.baseUrl}/movimientos`, request);
  }

  /**
   * Consulta el historial paginado de movimientos de inventario con filtros.
   */
  listarMovimientos(
    productoId?: number,
    almacenId?: number,
    tipoMovimiento?: string,
    fechaInicio?: string,
    fechaFin?: string,
    page: number = 0,
    size: number = 15
  ): Observable<PageResult<MovimientoInventarioResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (productoId) params = params.set('productoId', productoId);
    if (almacenId) params = params.set('almacenId', almacenId);
    if (tipoMovimiento) params = params.set('tipoMovimiento', tipoMovimiento);
    if (fechaInicio) params = params.set('fechaInicio', fechaInicio);
    if (fechaFin) params = params.set('fechaFin', fechaFin);

    return this.http.get<PageResult<MovimientoInventarioResponse>>(`${this.baseUrl}/movimientos`, { params });
  }

  /**
   * Consulta el detalle de un movimiento por su identificador.
   */
  obtenerMovimiento(id: number): Observable<MovimientoInventarioResponse> {
    return this.http.get<MovimientoInventarioResponse>(`${this.baseUrl}/movimientos/${id}`);
  }

  /**
   * Administrar stock por almacén: Consulta paginada general.
   */
  listarStock(
    almacenId?: number,
    productoId?: number,
    search?: string,
    page: number = 0,
    size: number = 15
  ): Observable<PageResult<StockAlmacenResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (almacenId) params = params.set('almacenId', almacenId);
    if (productoId) params = params.set('productoId', productoId);
    if (search && search.trim()) params = params.set('search', search.trim());

    return this.http.get<PageResult<StockAlmacenResponse>>(`${this.baseUrl}/stock`, { params });
  }

  /**
   * Administrar stock por almacén específico.
   */
  listarStockPorAlmacen(almacenId: number, page: number = 0, size: number = 15): Observable<PageResult<StockAlmacenResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResult<StockAlmacenResponse>>(`${this.baseUrl}/stock/almacen/${almacenId}`, { params });
  }

  /**
   * Alertas de stock mínimo: Consulta existencias con déficit.
   */
  listarAlertasStockMinimo(almacenId?: number): Observable<AlertaStockMinimoResponse[]> {
    let params = new HttpParams();
    if (almacenId) params = params.set('almacenId', almacenId);
    return this.http.get<AlertaStockMinimoResponse[]>(`${this.baseUrl}/stock/alertas-minimo`, { params });
  }

  /**
   * Consulta rápida de stock disponible para integración con otros módulos.
   */
  consultarStockDisponible(productoId: number, almacenId: number): Observable<{ productoId: number; almacenId: number; stockDisponible: number }> {
    const params = new HttpParams().set('productoId', productoId).set('almacenId', almacenId);
    return this.http.get<{ productoId: number; almacenId: number; stockDisponible: number }>(
      `${this.baseUrl}/stock/disponible`,
      { params }
    );
  }

  /**
   * Listado de productos activos para selects de formularios.
   */
  listarProductos(): Observable<ProductoSimple[]> {
    return this.http.get<ProductoSimple[]>(`${this.baseUrl}/productos`);
  }

  /**
   * Listado de almacenes activos para selects de formularios.
   */
  listarAlmacenes(): Observable<AlmacenSimple[]> {
    return this.http.get<AlmacenSimple[]>(`${this.baseUrl}/almacenes`);
  }
}
