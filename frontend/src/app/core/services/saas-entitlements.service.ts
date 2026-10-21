import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SaasFunction {
  codigo: string;
  nombre: string;
  modulo: string;
  rutaFrontend: string | null;
}

export interface TenantEntitlements {
  codigoEmpresa: string;
  planCodigo: string;
  planNombre: string;
  estadoSuscripcion: string;
  fechaFin: string;
  limiteUsuarios: number;
  limiteSucursales: number;
  limiteAlmacenes: number;
  limiteProductos: number;
  funciones: SaasFunction[];
}

@Injectable({ providedIn: 'root' })
export class SaasEntitlementsService {
  private readonly http = inject(HttpClient);
  private readonly state = signal<TenantEntitlements | null>(null);

  readonly entitlements = this.state.asReadonly();

  load() {
    return this.http.get<TenantEntitlements>(`${environment.apiUrl}/saas/mi-suscripcion`).pipe(
      tap(value => this.state.set(value))
    );
  }

  allows(functionName: string): boolean {
    const current = this.state();
    if (!current) return false;
    const expected = this.normalize(functionName);
    return current.funciones.some(item => this.normalize(item.nombre) === expected);
  }

  clear(): void {
    this.state.set(null);
  }

  private normalize(value: string): string {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();
  }
}
