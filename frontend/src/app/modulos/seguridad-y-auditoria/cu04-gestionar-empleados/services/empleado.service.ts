import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ActualizarEmpleadoDto, CrearEmpleadoDto, Empleado, SucursalItem } from '../models/empleado.model';

@Injectable({
  providedIn: 'root'
})
export class EmpleadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/empleados`;

  listar(filtros?: { sucursalId?: number; cargo?: string; activo?: boolean; search?: string }): Observable<Empleado[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.sucursalId != null) {
        params = params.set('sucursalId', filtros.sucursalId.toString());
      }
      if (filtros.cargo) {
        params = params.set('cargo', filtros.cargo);
      }
      if (filtros.activo != null) {
        params = params.set('activo', filtros.activo.toString());
      }
      if (filtros.search) {
        params = params.set('search', filtros.search);
      }
    }
    return this.http.get<Empleado[]>(this.baseUrl, { params });
  }

  obtenerPorId(id: number): Observable<Empleado> {
    return this.http.get<Empleado>(`${this.baseUrl}/${id}`);
  }

  listarSucursales(): Observable<SucursalItem[]> {
    return this.http.get<SucursalItem[]>(`${this.baseUrl}/sucursales`);
  }

  crear(dto: CrearEmpleadoDto): Observable<Empleado> {
    return this.http.post<Empleado>(this.baseUrl, dto);
  }

  actualizar(id: number, dto: ActualizarEmpleadoDto): Observable<Empleado> {
    return this.http.put<Empleado>(`${this.baseUrl}/${id}`, dto);
  }

  cambiarEstado(id: number, activo: boolean, motivo?: string): Observable<Empleado> {
    return this.http.patch<Empleado>(`${this.baseUrl}/${id}/estado`, { activo, motivo });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
