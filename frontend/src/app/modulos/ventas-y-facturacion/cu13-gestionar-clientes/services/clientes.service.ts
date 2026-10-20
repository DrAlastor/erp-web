import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

export interface Cliente {
  id: number;
  razonSocial: string;
  nitCi: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  ciudad: string | null;
  condicionPago: 'CONTADO' | 'CREDITO';
  limiteCredito: number;
  diasCredito: number;
  activo: boolean;
  fechaCreacion: string | null;
  usuarioId?: number | null;
  username?: string | null;
}

export interface ClientePage {
  content: Cliente[];
  totalElements: number;
  totalPages: number;
}

export interface ClienteRequest {
  razonSocial: string;
  nitCi: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  ciudad?: string;
  condicionPago: 'CONTADO' | 'CREDITO';
  limiteCredito: number;
  diasCredito: number;
}

@Injectable({ providedIn: 'root' })
export class ClientesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/clientes`;

  list(
    search = '',
    condicionPagoOrActivo = '',
    activoOrPage: string | number = '',
    pageArg?: number
  ) {
    let condicionPago = '';
    let activo = '';
    let page = 0;

    if (typeof activoOrPage === 'number') {
      // Compatibilidad con llamadas legadas: list(search, activo, page)
      activo = condicionPagoOrActivo;
      page = activoOrPage;
    } else {
      // Nueva firma: list(search, condicionPago, activo, page)
      condicionPago = condicionPagoOrActivo;
      activo = activoOrPage;
      page = pageArg ?? 0;
    }

    let params = new HttpParams()
      .set('search', search)
      .set('page', page)
      .set('size', 12);

    if (condicionPago) {
      params = params.set('condicionPago', condicionPago);
    }
    if (activo) {
      params = params.set('activo', activo);
    }

    return this.http.get<ClientePage>(this.url, { params });
  }

  detail(id: number) {
    return this.http.get<Cliente>(`${this.url}/${id}`);
  }

  current() {
    return this.http.get<Cliente>(`${environment.apiUrl}/perfil`);
  }

  create(body: ClienteRequest) {
    return this.http.post<Cliente>(this.url, body);
  }

  update(id: number, body: ClienteRequest) {
    return this.http.put<Cliente>(`${this.url}/${id}`, body);
  }

  status(id: number, activo: boolean) {
    return this.http.patch<Cliente>(`${this.url}/${id}/status`, { activo });
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
