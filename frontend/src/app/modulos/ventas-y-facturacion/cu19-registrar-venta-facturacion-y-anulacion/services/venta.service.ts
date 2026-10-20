import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  CatalogosVenta,
  ClienteVenta,
  CobroStripeRequest,
  CobroStripeResponse,
  Dosificacion,
  DosificacionRequest,
  ProductoVenta,
  Venta,
  VentaPage,
  VentaRequest,
} from '../models/venta.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/ventas`;

  listar(search: string, estado: string, comprobante: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (estado) params = params.set('estado', estado);
    if (comprobante) params = params.set('comprobante', comprobante);
    return this.http.get<VentaPage>(this.url, { params });
  }

  detalle(id: number) {
    return this.http.get<Venta>(`${this.url}/${id}`);
  }

  catalogos() {
    return this.http.get<CatalogosVenta>(`${this.url}/catalogos`);
  }

  productos(sucursalId: number, search: string) {
    const params = new HttpParams().set('sucursalId', sucursalId).set('search', search);
    return this.http.get<ProductoVenta[]>(`${this.url}/productos`, { params });
  }

  clientes(search: string) {
    return this.http.get<ClienteVenta[]>(`${this.url}/clientes`, { params: { search } });
  }

  registrar(body: VentaRequest) {
    return this.http.post<Venta>(this.url, body);
  }

  /** Valida la venta y abre el cobro con tarjeta en Stripe Checkout. */
  iniciarCobroStripe(body: CobroStripeRequest) {
    return this.http.post<CobroStripeResponse>(`${this.url}/stripe/checkout`, body);
  }

  anular(id: number, motivo: string) {
    return this.http.patch<Venta>(`${this.url}/${id}/anular`, { motivo });
  }

  // Dosificaciones del SIN (secuencia_facturacion)

  dosificaciones() {
    return this.http.get<Dosificacion[]>(`${this.url}/dosificaciones`);
  }

  registrarDosificacion(body: DosificacionRequest) {
    return this.http.post<Dosificacion>(`${this.url}/dosificaciones`, body);
  }

  cambiarEstadoDosificacion(id: number, activo: boolean) {
    return this.http.patch<Dosificacion>(`${this.url}/dosificaciones/${id}/estado`, { activo });
  }
}
