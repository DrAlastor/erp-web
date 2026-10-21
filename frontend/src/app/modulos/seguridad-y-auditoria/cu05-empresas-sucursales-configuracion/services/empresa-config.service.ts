import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  ActualizarEmpresaDto, Ciudad, Empresa, GuardarSucursalDto, Parametro, Sucursal
} from '../models/empresa-config.model';

@Injectable({ providedIn: 'root' })
export class EmpresaConfigService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  obtenerEmpresa(): Observable<Empresa> {
    return this.http.get<Empresa>(`${this.api}/empresa`);
  }

  actualizarEmpresa(dto: ActualizarEmpresaDto): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.api}/empresa`, dto);
  }

  listarSucursales(filtros?: { activo?: boolean; search?: string }): Observable<Sucursal[]> {
    let params = new HttpParams();
    if (filtros?.activo != null) params = params.set('activo', String(filtros.activo));
    if (filtros?.search) params = params.set('search', filtros.search);
    return this.http.get<Sucursal[]>(`${this.api}/sucursales`, { params });
  }

  listarCiudades(): Observable<Ciudad[]> {
    return this.http.get<Ciudad[]>(`${this.api}/sucursales/ciudades`);
  }

  crearSucursal(dto: GuardarSucursalDto): Observable<Sucursal> {
    return this.http.post<Sucursal>(`${this.api}/sucursales`, dto);
  }

  actualizarSucursal(id: number, dto: GuardarSucursalDto): Observable<Sucursal> {
    return this.http.put<Sucursal>(`${this.api}/sucursales/${id}`, dto);
  }

  cambiarEstadoSucursal(id: number, activo: boolean): Observable<Sucursal> {
    return this.http.patch<Sucursal>(`${this.api}/sucursales/${id}/estado`, { activo });
  }

  obtenerConfiguracion(): Observable<Parametro[]> {
    return this.http.get<Parametro[]>(`${this.api}/configuracion`);
  }

  guardarConfiguracion(parametros: Record<string, string>): Observable<Parametro[]> {
    return this.http.put<Parametro[]>(`${this.api}/configuracion`, { parametros });
  }
}
