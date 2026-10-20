import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

export interface ListaPrecio {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  esPrincipal: boolean;
  activo: boolean;
  articulosTarifadosCount: number;
}

export interface ItemPrecioProducto {
  productoId: number;
  codigoSku: string;
  nombre: string;
  categoriaNombre: string;
  costoCompra: number;
  precioCatalogo: number;
  precioAsignado: number;
  margenCalculadoPorc: number;
  fechaActualizacion: string;
}

export interface Descuento {
  id: number;
  nombre: string;
  tipo: 'PORCENTAJE' | 'MONTO_FIJO';
  valor: number;
  fechaInicio: string | null;
  fechaFin: string | null;
  activo: boolean;
}

export interface CrearListaPrecioRequest {
  codigo: string;
  nombre: string;
  descripcion?: string;
  esPrincipal?: boolean;
}

export interface ActualizarListaPrecioRequest {
  nombre: string;
  descripcion?: string;
  esPrincipal?: boolean;
  activo?: boolean;
}

export interface ActualizarPreciosMatrizRequest {
  items: {
    productoId: number;
    precio: number;
  }[];
}

export interface CrearDescuentoRequest {
  nombre: string;
  tipo: 'PORCENTAJE' | 'MONTO_FIJO';
  valor: number;
  fechaInicio?: string;
  fechaFin?: string;
}

export interface ActualizarDescuentoRequest {
  nombre: string;
  tipo: 'PORCENTAJE' | 'MONTO_FIJO';
  valor: number;
  fechaInicio?: string;
  fechaFin?: string;
  activo?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PreciosListasService {
  private readonly http = inject(HttpClient);
  private readonly urlListas = `${environment.apiUrl}/listas-precios`;
  private readonly urlDescuentos = `${environment.apiUrl}/descuentos`;

  // Listas de Precios
  listarListas() {
    return this.http.get<ListaPrecio[]>(this.urlListas);
  }

  obtenerLista(id: number) {
    return this.http.get<ListaPrecio>(`${this.urlListas}/${id}`);
  }

  crearLista(body: CrearListaPrecioRequest) {
    return this.http.post<ListaPrecio>(this.urlListas, body);
  }

  actualizarLista(id: number, body: ActualizarListaPrecioRequest) {
    return this.http.put<ListaPrecio>(`${this.urlListas}/${id}`, body);
  }

  cambiarEstadoLista(id: number, activo: boolean) {
    return this.http.patch<ListaPrecio>(`${this.urlListas}/${id}/status`, null, {
      params: { activo: String(activo) }
    });
  }

  obtenerMatrizProductos(listaId: number) {
    return this.http.get<ItemPrecioProducto[]>(`${this.urlListas}/${listaId}/matriz-productos`);
  }

  actualizarPreciosMatriz(listaId: number, body: ActualizarPreciosMatrizRequest) {
    return this.http.put<void>(`${this.urlListas}/${listaId}/precios`, body);
  }

  // Descuentos y Promociones
  listarDescuentos(soloActivos?: boolean) {
    let params = new HttpParams();
    if (soloActivos !== undefined) {
      params = params.set('soloActivos', String(soloActivos));
    }
    return this.http.get<Descuento[]>(this.urlDescuentos, { params });
  }

  crearDescuento(body: CrearDescuentoRequest) {
    return this.http.post<Descuento>(this.urlDescuentos, body);
  }

  actualizarDescuento(id: number, body: ActualizarDescuentoRequest) {
    return this.http.put<Descuento>(`${this.urlDescuentos}/${id}`, body);
  }

  cambiarEstadoDescuento(id: number, activo: boolean) {
    return this.http.patch<Descuento>(`${this.urlDescuentos}/${id}/status`, null, {
      params: { activo: String(activo) }
    });
  }

  eliminarDescuento(id: number) {
    return this.http.delete<void>(`${this.urlDescuentos}/${id}`);
  }
}
