import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

export interface Role {
  id: string;
  nombre: string;
}

export interface User {
  id: number;
  username: string;
  fullname: string;
  email: string;
  enable: boolean;
  roles: Role[];
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UserPage {
  content: User[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/usuarios`;

  list(search: string, role: string, enable: string, page: number) {
    let params = new HttpParams().set('search', search).set('page', page).set('size', 15);
    if (role) params = params.set('role', role);
    if (enable) params = params.set('enable', enable);
    return this.http.get<UserPage>(this.url, { params });
  }

  roles() {
    return this.http.get<Role[]>(`${this.url}/roles`);
  }

  detail(id: number) {
    return this.http.get<User>(`${this.url}/${id}`);
  }

  update(id: number, body: { fullname: string; email: string }) {
    return this.http.put<User>(`${this.url}/${id}`, body);
  }

  status(id: number, enable: boolean) {
    return this.http.patch<User>(`${this.url}/${id}/status`, { enable });
  }
}
