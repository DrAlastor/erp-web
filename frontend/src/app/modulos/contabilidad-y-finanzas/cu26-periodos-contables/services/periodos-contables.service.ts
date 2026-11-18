import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { Gestion, PeriodoContable } from '../models/periodo-contable.model';

@Injectable({ providedIn: 'root' })
export class PeriodosContablesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/periodos-contables`;

  gestiones() {
    return this.http.get<number[]>(`${this.url}/gestiones`);
  }

  gestion(gestion: number) {
    return this.http.get<Gestion>(this.url, { params: new HttpParams().set('gestion', gestion) });
  }

  generar(gestion: number) {
    return this.http.post<Gestion>(`${this.url}/gestiones`, { gestion });
  }

  cerrar(id: number) {
    return this.http.patch<PeriodoContable>(`${this.url}/${id}/cerrar`, {});
  }

  reabrir(id: number) {
    return this.http.patch<PeriodoContable>(`${this.url}/${id}/reabrir`, {});
  }
}
