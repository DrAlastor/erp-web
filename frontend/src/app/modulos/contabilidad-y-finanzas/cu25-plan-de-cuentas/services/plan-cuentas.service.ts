import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { Cuenta, CuentaActualizarRequest, CuentaNodo, CuentaRequest } from '../models/plan-cuentas.model';

@Injectable({ providedIn: 'root' })
export class PlanCuentasService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/plan-cuentas`;

  arbol() {
    return this.http.get<CuentaNodo[]>(`${this.url}/arbol`);
  }

  crear(body: CuentaRequest) {
    return this.http.post<Cuenta>(this.url, body);
  }

  actualizar(id: number, body: CuentaActualizarRequest) {
    return this.http.put<Cuenta>(`${this.url}/${id}`, body);
  }

  desactivar(id: number) {
    return this.http.patch<Cuenta>(`${this.url}/${id}/desactivar`, {});
  }

  reactivar(id: number) {
    return this.http.patch<Cuenta>(`${this.url}/${id}/reactivar`, {});
  }
}
