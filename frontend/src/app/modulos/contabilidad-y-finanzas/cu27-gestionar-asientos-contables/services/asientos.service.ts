import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  Asiento,
  AsientoPage,
  AsientoRequest,
  CuentaOpcion,
  FiltroAsientos,
  PermisosAsientos,
} from '../models/asiento.model';

/** HU-27 / CU27 — cliente HTTP de `/api/asientos-contables`. */
@Injectable({ providedIn: 'root' })
export class AsientosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/asientos-contables`;

  list(f: FiltroAsientos, page: number) {
    let params = new HttpParams().set('page', page).set('size', 15);
    for (const [k, v] of Object.entries(f)) if (v) params = params.set(k, String(v).trim());
    return this.http.get<AsientoPage>(this.url, { params });
  }

  detail(id: number) {
    return this.http.get<Asiento>(`${this.url}/${id}`);
  }

  create(body: AsientoRequest) {
    return this.http.post<Asiento>(this.url, body);
  }

  update(id: number, body: AsientoRequest) {
    return this.http.put<Asiento>(`${this.url}/${id}`, body);
  }

  contabilizar(id: number) {
    return this.http.post<Asiento>(`${this.url}/${id}/contabilizar`, {});
  }

  anular(id: number, motivo: string) {
    return this.http.post<Asiento>(`${this.url}/${id}/anular`, { motivo });
  }

  cuentas(search: string) {
    return this.http.get<CuentaOpcion[]>(`${this.url}/opciones/cuentas`, { params: { search } });
  }

  tipos() {
    return this.http.get<string[]>(`${this.url}/opciones/tipos`);
  }

  permisos() {
    return this.http.get<PermisosAsientos>(`${this.url}/opciones/permisos`);
  }
}
