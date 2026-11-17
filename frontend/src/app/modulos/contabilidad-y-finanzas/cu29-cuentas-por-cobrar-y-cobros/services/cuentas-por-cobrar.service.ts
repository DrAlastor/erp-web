import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  CuentaPorCobrar,
  CuotaPage,
  EstadoCuentaCliente,
  FiltroEstadoCxc,
  Opcion,
  RegistrarCobroRequest,
  RegistrarCobroResponse,
  ResumenCxc,
} from '../models/cuenta-por-cobrar.model';

/** HU-29 / CU29 — cliente HTTP de `/api/cuentas-por-cobrar`. */
@Injectable({ providedIn: 'root' })
export class CuentasPorCobrarService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cuentas-por-cobrar`;

  list(search: string, estado: FiltroEstadoCxc, clienteId: number | null, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    if (clienteId) params = params.set('clienteId', clienteId);
    return this.http.get<CuotaPage>(this.url, { params });
  }

  resumen() {
    return this.http.get<ResumenCxc>(`${this.url}/resumen`);
  }

  detail(id: number) {
    return this.http.get<CuentaPorCobrar>(`${this.url}/${id}`);
  }

  registrarCobro(id: number, body: RegistrarCobroRequest) {
    return this.http.post<RegistrarCobroResponse>(`${this.url}/${id}/cobros`, body);
  }

  estadoCuenta(clienteId: number) {
    return this.http.get<EstadoCuentaCliente>(`${this.url}/clientes/${clienteId}/estado-cuenta`);
  }

  clientes(search: string) {
    return this.http.get<Opcion[]>(`${this.url}/opciones/clientes`, { params: { search } });
  }

  formasPago() {
    return this.http.get<Opcion[]>(`${this.url}/opciones/formas-pago`);
  }

  permisos() {
    return this.http.get<{ cobrar: boolean }>(`${this.url}/opciones/permisos`);
  }
}
