import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  HistorialProveedor,
  Proveedor,
  ProveedorOpcion,
  ProveedorPage,
  ProveedorRequest,
} from '../models/proveedor.model';

@Injectable({ providedIn: 'root' })
export class ProveedoresService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/proveedores`;

  list(search: string, activo: string, ciudad: string, page: number) {
    let params = new HttpParams().set('search', search).set('ciudad', ciudad).set('page', page).set('size', 15);
    if (activo) params = params.set('activo', activo);
    return this.http.get<ProveedorPage>(this.url, { params });
  }

  activos() {
    return this.http.get<ProveedorOpcion[]>(`${this.url}/activos`);
  }

  ciudades() {
    return this.http.get<string[]>(`${this.url}/ciudades`);
  }

  historial(id: number) {
    return this.http.get<HistorialProveedor>(`${this.url}/${id}/historial`);
  }

  create(body: ProveedorRequest) {
    return this.http.post<Proveedor>(this.url, body);
  }

  update(id: number, body: ProveedorRequest) {
    return this.http.put<Proveedor>(`${this.url}/${id}`, body);
  }

  deactivate(id: number, motivo: string) {
    return this.http.post<Proveedor>(`${this.url}/${id}/desactivar`, { motivo });
  }

  reactivate(id: number) {
    return this.http.post<Proveedor>(`${this.url}/${id}/reactivar`, {});
  }
}
