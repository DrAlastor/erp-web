import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

export interface Cliente {
  id: number;
  razonSocial: string;
  nitCi: string;
  telefono: string | null;
  direccion: string | null;
  activo: boolean;
  fechaCreacion: string | null;
  usuarioId?: number | null;
  username?: string | null;
  email?: string | null;
}

export interface ClientePage {
  content: Cliente[];
  totalElements: number;
  totalPages: number;
}

export interface ClienteRequest {
  razonSocial: string;
  nitCi: string;
  telefono: string;
  direccion: string;
}

@Injectable({ providedIn: 'root' })
export class ClientesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/clientes`;

  list(search: string, activo: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (activo) params = params.set('activo', activo);
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
}
