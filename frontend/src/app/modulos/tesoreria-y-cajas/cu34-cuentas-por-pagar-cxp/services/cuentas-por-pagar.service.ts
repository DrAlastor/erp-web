import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  CompraSinCuentaResponse,
  CuentaPorPagarPage,
  CuentaPorPagarResponse,
  CuentaPorPagarResumenResponse,
  FiltroEstadoCxp,
  FormaPagoOpcionResponse,
  GenerarCuotasRequest,
  ProveedorOpcionResponse,
  RegistrarPagoRequest,
  ResumenCxpResponse,
} from '../models/cuenta-por-pagar.model';

/** HU-24 / CU34 — cliente HTTP de `/api/cuentas-por-pagar` (CONTABILIDAD consultar / crear). */
@Injectable({ providedIn: 'root' })
export class CuentasPorPagarService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cuentas-por-pagar`;

  list(search: string, estado: FiltroEstadoCxp, proveedorId: number | null, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    if (proveedorId) params = params.set('proveedorId', proveedorId);
    return this.http.get<CuentaPorPagarPage>(this.url, { params });
  }

  resumen() {
    return this.http.get<ResumenCxpResponse>(`${this.url}/resumen`);
  }

  detail(id: number) {
    return this.http.get<CuentaPorPagarResponse>(`${this.url}/${id}`);
  }

  registrarPago(id: number, body: RegistrarPagoRequest) {
    return this.http.post<CuentaPorPagarResponse>(`${this.url}/${id}/pagos`, body);
  }

  generar(body: GenerarCuotasRequest) {
    return this.http.post<CuentaPorPagarResumenResponse[]>(`${this.url}/generar`, body);
  }

  comprasSinCuenta(search: string) {
    const params = new HttpParams().set('search', search);
    return this.http.get<CompraSinCuentaResponse[]>(`${this.url}/opciones/compras-sin-cuenta`, { params });
  }

  proveedores(search: string) {
    const params = new HttpParams().set('search', search);
    return this.http.get<ProveedorOpcionResponse[]>(`${this.url}/opciones/proveedores`, { params });
  }

  formasPago() {
    return this.http.get<FormaPagoOpcionResponse[]>(`${this.url}/opciones/formas-pago`);
  }
}
